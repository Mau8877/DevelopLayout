#!/usr/bin/env node
// Convierte una copia de la plantilla en un proyecto con nombre propio.
//
//   pnpm project:init --nombre "Acme Tienda" --slug acme-tienda --paquete com.acme [--dry-run]
//
// * Se usa UNA vez, sobre una copia recién clonada y sin cambios sin commitear: así
// * todo lo que haga se revisa con `git diff` y se deshace con git.
// * Reemplaza solo los nombres conocidos de la plantilla, uno por uno. Un reemplazo
// * a ciegas de "app" rompería cosas como `app-sandbox` de macOS o el rol
// * `app_runtime` de las migraciones.
// ! No lee ni modifica ningún `.env`: tienen secretos. Se crean después con `pnpm setup:env`.

import { execFileSync } from 'node:child_process';
import {
  existsSync, mkdirSync, readFileSync, readdirSync, renameSync, rmdirSync, writeFileSync,
} from 'node:fs';
import { dirname, join, resolve } from 'node:path';
import { fileURLToPath, pathToFileURL } from 'node:url';

/** Contenedores, red, volúmenes, bucket e imágenes que usan el prefijo `app-`. */
const RECURSOS = [
  'proxy', 'frontend-node', 'frontend', 'backend', 'postgres', 'redis', 'minio-permisos', 'minio-init',
  'minio', 'mailpit', 'prometheus', 'grafana', 'network', 'storage', 'prod',
];
const RECURSO = new RegExp(`(?<![A-Za-z0-9_-])app-(${RECURSOS.join('|')})(?![A-Za-z0-9])`, 'g');
const DOMINIO = /(?<![A-Za-z0-9])com\.example(?![A-Za-z0-9])/g;

// * No se tocan: el historial (historias y tareas ya escritas), la guía que explica
// * los nombres de la plantilla, este script y sus pruebas, y los lockfiles.
const EXCLUIDOS = [
  'docs/HistoriasDeUsuario/', 'docs/UnidadesTrabajo/', 'docs/README.md', 'docs/TUTORIAL.md',
  'scripts/proyecto/init-project.mjs', 'scripts/proyecto/init-project.test.mjs', 'pnpm-lock.yaml', 'projects/mobile/pubspec.lock',
];

/** Palabras que no pueden ser parte de un paquete de Java, Kotlin o Dart. */
const RESERVADAS = new Set((
  'abstract as assert async await boolean break byte case catch char class const continue covariant default ' +
  'deferred do double dynamic else enum export extends extension external factory false final finally float for ' +
  'fun get goto if implements import in instanceof int interface is late library long mixin native new null ' +
  'object operator package part private protected public required rethrow return sealed set short static ' +
  'strictfp super switch synchronized this throw throws transient true try typealias typeof val var void ' +
  'volatile when while with yield'
).split(' '));

/**
 * Valida lo que escribió la persona y deriva el resto de los nombres.
 * @throws si algún dato rompería el código generado.
 */
export function crearValores({ nombre, slug, paquete }) {
  const errores = [];
  const visible = (nombre ?? '').trim();
  // * Va dentro de XML, JSON, SQL, JSX y cadenas de Dart: por eso sin comillas ni símbolos.
  if (!/^[\p{L}\p{N}][\p{L}\p{N} ._-]{0,39}$/u.test(visible)) {
    errores.push('--nombre: de 1 a 40 caracteres; letras, números, espacios, punto, guion o guion bajo');
  }
  if (!/^[a-z][a-z0-9]*(-[a-z0-9]+)*$/.test(slug ?? '') || (slug ?? '').length > 30 || slug === 'app') {
    errores.push('--slug: minúsculas, números y guiones (ej. acme-tienda), hasta 30, y distinto de "app"');
  }
  const partes = (paquete ?? '').split('.');
  if (
    partes.length < 2 ||
    partes.some((p) => !/^[a-z][a-z0-9]*$/.test(p) || RESERVADAS.has(p)) ||
    paquete === 'com.example' || (paquete ?? '').startsWith('com.example.')
  ) {
    errores.push('--paquete: dominio al revés en minúsculas y sin guiones (ej. com.acme), distinto de com.example');
  }
  if (errores.length) throw new Error(`datos inválidos:\n  ${errores.join('\n  ')}`);

  const snake = slug.replaceAll('-', '_');
  return {
    nombre: visible,
    slug,
    snake,
    paquete,
    rutaPaquete: paquete.replaceAll('.', '/'),
    clase: slug.split('-').map((p) => p[0].toUpperCase() + p.slice(1)).join(''),
    dart: `${snake}_mobile`,
  };
}

const en = (...archivos) => (ruta) => archivos.includes(ruta);
const bajo = (...carpetas) => (ruta) => carpetas.some((c) => ruta.startsWith(c));

/**
 * Cada regla: dónde aplica y qué reemplaza. En la plantilla todas tienen que
 * encontrar algo: si una deja de hacerlo, las pruebas fallan y se corrige aquí.
 */
export function crearReglas(v) {
  const todo = () => true;
  return [
    { id: 'dominio', donde: todo, buscar: DOMINIO, poner: v.paquete },
    { id: 'carpeta-del-paquete', donde: todo, buscar: /com\/example(?![A-Za-z0-9])/g, poner: v.rutaPaquete },
    { id: 'recursos-docker', donde: todo, buscar: RECURSO, poner: (_, recurso) => `${v.slug}-${recurso}` },
    {
      id: 'prefijo-explicado', donde: en('docs/INFRAESTRUCTURA.md'),
      buscar: /`app-`/g, poner: `\`${v.slug}-\``,
    },
    { id: 'proyecto-compose', donde: en('docker-compose.yml'), buscar: /^name: app(?=\r?$)/m, poner: `name: ${v.slug}` },
    { id: 'base-de-datos', donde: todo, buscar: /(?<![A-Za-z0-9])app_(db|admin)(?![A-Za-z0-9])/g, poner: (_, cual) => `${v.snake}_${cual}` },
    { id: 'clase-principal', donde: todo, buscar: /AppBackendApplication/g, poner: `${v.clase}BackendApplication` },
    { id: 'paquete-dart', donde: bajo('projects/mobile/'), buscar: /package:mobile\//g, poner: `package:${v.dart}/` },
    { id: 'nombre-pubspec', donde: en('projects/mobile/pubspec.yaml'), buscar: /^name: mobile(?=\r?$)/m, poner: `name: ${v.dart}` },
    { id: 'paquete-raiz', donde: en('package.json'), buscar: /"name": "developlayout"/g, poner: `"name": "${v.slug}"` },
    {
      id: 'nombre-del-repo', donde: en('README.md', '.claude/CLAUDE.md', 'docs/INFRAESTRUCTURA.md'),
      buscar: /DevelopLayout/g, poner: v.nombre,
    },
    {
      id: 'titulos-de-codigo', donde: bajo('projects/frontend/src/', 'projects/mobile/lib/', 'projects/mobile/test/'),
      buscar: /(['"])App( Admin)?\1/g, poner: (_, comilla, admin) => `${comilla}${v.nombre}${admin ?? ''}${comilla}`,
    },
    { id: 'titulo-web', donde: en('projects/frontend/index.html'), buscar: /<title>frontend<\/title>/g, poner: `<title>${v.nombre}</title>` },
    {
      id: 'dato-de-ejemplo', donde: en('projects/backend/src/main/resources/db/migration/V1__init_schema.sql'),
      buscar: /'App'/g, poner: `'${v.nombre}'`,
    },
    { id: 'etiqueta-android', donde: en('projects/mobile/android/app/src/main/AndroidManifest.xml'), buscar: /android:label="mobile"/g, poner: `android:label="${v.nombre}"` },
    { id: 'nombre-ios', donde: en('projects/mobile/ios/Runner/Info.plist'), buscar: /<string>[Mm]obile<\/string>/g, poner: `<string>${v.nombre}</string>` },
    {
      id: 'web-de-mobile', donde: en('projects/mobile/web/index.html', 'projects/mobile/web/manifest.json'),
      buscar: /(<title>|"(?:short_name|name)": "|apple-mobile-web-app-title" content=")mobile(?=[<"])/g,
      poner: (_, antes) => `${antes}${v.nombre}`,
    },
  ];
}

/** Aplica las reglas a un archivo. `usadas` junta los id de las que encontraron algo. */
export function transformar(ruta, texto, reglas, usadas = new Set()) {
  let resultado = texto;
  for (const regla of reglas) {
    if (!regla.donde(ruta)) continue;
    const siguiente = resultado.replace(regla.buscar, regla.poner);
    if (siguiente !== resultado) usadas.add(regla.id);
    resultado = siguiente;
  }
  return resultado;
}

export function nuevaRuta(ruta, v) {
  return ruta
    .replace('/com/example/', `/${v.rutaPaquete}/`)
    .replace(/AppBackendApplication(?=[A-Za-z]*\.java$)/, `${v.clase}BackendApplication`);
}

/** Lo que quedaría de la plantilla en un texto ya transformado. */
export function restos(texto) {
  const patrones = [
    DOMINIO, /com\/example(?![A-Za-z0-9])/g, RECURSO, /(?<![A-Za-z0-9])app_(?:db|admin)(?![A-Za-z0-9])/g,
    /package:mobile\//g, /AppBackendApplication/g,
    // * Un nombre armado al ejecutar (`app-${algo}`) no se puede renombrar: hay que escribirlo entero.
    /(?<![A-Za-z0-9_-])app-\$\{/g,
  ];
  return patrones.flatMap((p) => texto.match(new RegExp(p.source, p.flags)) ?? []);
}

/** Texto UTF-8 del archivo, o `null` si es binario (no se toca). */
function leerTexto(ruta) {
  const bytes = readFileSync(ruta);
  if (bytes.includes(0)) return null;
  const texto = bytes.toString('utf8');
  return Buffer.from(texto, 'utf8').equals(bytes) ? texto : null;
}

export function archivosVersionados(raiz) {
  return execFileSync('git', ['ls-files', '-z'], { cwd: raiz, encoding: 'utf8', maxBuffer: 64 * 1024 * 1024 })
    .split('\0')
    .filter(Boolean);
}

/** Calcula todos los cambios sin escribir nada. */
export function planificar(raiz, valores, archivos = archivosVersionados(raiz)) {
  const reglas = crearReglas(valores);
  const usadas = new Set();
  const cambios = [];
  const pendientes = [];
  for (const ruta of archivos) {
    if (EXCLUIDOS.some((e) => ruta === e || (e.endsWith('/') && ruta.startsWith(e)))) continue;
    if (/(^|\/)\.env$/.test(ruta) || !existsSync(join(raiz, ruta))) continue;
    const destino = nuevaRuta(ruta, valores);
    const texto = leerTexto(join(raiz, ruta));
    const contenido = texto === null ? null : transformar(ruta, texto, reglas, usadas);
    if (contenido !== null) {
      for (const resto of restos(contenido)) {
        // * Un paquete como `com.examples` no es un resto: ya lo descarta el patrón.
        pendientes.push(`${destino}: ${resto}`);
      }
    }
    if (destino !== ruta || contenido !== texto) {
      cambios.push({ ruta, destino, contenido: contenido === texto ? null : contenido });
    }
  }
  const sinUso = reglas.map((r) => r.id).filter((id) => !usadas.has(id));
  return { cambios, pendientes, sinUso };
}

function borrarCarpetasVacias(raiz, desde) {
  for (let carpeta = desde; carpeta !== raiz && carpeta.startsWith(raiz); carpeta = dirname(carpeta)) {
    if (!existsSync(carpeta) || readdirSync(carpeta).length > 0) return;
    rmdirSync(carpeta);
  }
}

export function aplicar(raiz, { cambios }) {
  for (const { ruta, destino, contenido } of cambios) {
    if (contenido !== null) writeFileSync(join(raiz, ruta), contenido);
    if (destino !== ruta) {
      if (existsSync(join(raiz, destino))) throw new Error(`ya existe ${destino}: no se pisa`);
      mkdirSync(dirname(join(raiz, destino)), { recursive: true });
      renameSync(join(raiz, ruta), join(raiz, destino));
    }
  }
  for (const { ruta, destino } of cambios) {
    if (destino !== ruta) borrarCarpetasVacias(raiz, dirname(join(raiz, ruta)));
  }
}

/** Se niega si la plantilla ya se inicializó o si hay cambios que se mezclarían con los suyos. */
export function exigirPlantillaLimpia(raiz) {
  const pom = join(raiz, 'projects/backend/pom.xml');
  if (!existsSync(pom) || !readFileSync(pom, 'utf8').includes('<groupId>com.example</groupId>')) {
    throw new Error('este proyecto ya no es la plantilla sin inicializar: el script se usa una sola vez');
  }
  let estado;
  try {
    estado = execFileSync('git', ['status', '--porcelain'], { cwd: raiz, encoding: 'utf8' });
  } catch {
    throw new Error('hace falta un repositorio git (git init y un primer commit) para poder revisar y deshacer');
  }
  if (estado.trim()) throw new Error('hay cambios sin commitear: commitealos o descartalos antes de inicializar');
}

export function leerArgumentos(argumentos) {
  const opciones = { dryRun: false };
  for (let i = 0; i < argumentos.length; i++) {
    const actual = argumentos[i];
    if (actual === '--') continue;
    if (actual === '--dry-run') {
      opciones.dryRun = true;
      continue;
    }
    const m = /^--(nombre|slug|paquete)(?:=(.*))?$/.exec(actual);
    if (!m) throw new Error(`argumento desconocido: ${actual}`);
    opciones[m[1]] = m[2] ?? argumentos[++i];
  }
  return opciones;
}

const USO = 'uso: pnpm project:init --nombre "Acme Tienda" --slug acme-tienda --paquete com.acme [--dry-run]';

function main() {
  const raiz = resolve(dirname(fileURLToPath(import.meta.url)), '..', '..');
  try {
    const opciones = leerArgumentos(process.argv.slice(2));
    const valores = crearValores(opciones);
    exigirPlantillaLimpia(raiz);
    const plan = planificar(raiz, valores);
    if (plan.pendientes.length) {
      throw new Error(`quedarían nombres de la plantilla:\n  ${plan.pendientes.slice(0, 20).join('\n  ')}`);
    }
    const movidos = plan.cambios.filter((c) => c.destino !== c.ruta).length;
    const editados = plan.cambios.filter((c) => c.contenido !== null).length;
    if (opciones.dryRun) {
      for (const c of plan.cambios) console.log(c.destino === c.ruta ? c.ruta : `${c.ruta} -> ${c.destino}`);
      console.log(`\nSimulación: ${editados} archivos se editarían y ${movidos} se moverían. No se escribió nada.`);
      return;
    }
    aplicar(raiz, plan);
    console.log(`✔ Proyecto «${valores.nombre}» listo: ${editados} archivos editados y ${movidos} movidos.

Revisá los cambios con \`git status\` y \`git diff\`.
Para deshacerlos: git checkout -- . && git clean -fd

Seguí con:
  1. pnpm install
  2. pnpm setup:env        (y cambiá INITIAL_ADMIN_EMAIL)
  3. docker compose up

Queda a mano (ver docs/README.md): íconos y pantalla de inicio, el nombre del archivo
ejecutable de escritorio, el titular de LICENSE y la descripción del README.`);
    if (existsSync(join(raiz, '.env')) || existsSync(join(raiz, 'projects/backend/.env'))) {
      console.log('\n! Ya tenías archivos .env y no se tocaron: revisá el nombre y el usuario de la base.');
    }
  } catch (error) {
    console.error(`✖ ${error.message}\n\n${USO}`);
    process.exitCode = 1;
  }
}

if (process.argv[1] && import.meta.url === pathToFileURL(resolve(process.argv[1])).href) main();
