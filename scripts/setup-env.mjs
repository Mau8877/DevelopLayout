#!/usr/bin/env node
// Prepara los archivos `.env` de los cuatro proyectos a partir de sus `.env.example`.
//
// * Idempotente: nunca pisa un valor que ya existe. Solo completa lo que sigue
// * diciendo `CAMBIAME` o lo que falta respecto del `.env.example`.
// * Los secretos se generan con `crypto` (aleatorios) y NUNCA se imprimen: el
// * reporte dice cuántos se generaron, no cuáles son.
// * El `.env` del backend deriva sus claves de las de la raíz (misma contraseña
// * de Postgres, Redis, MinIO, JWT), para que los dos archivos no se desincronicen.
//
// Uso (desde la raíz del repo):
//   pnpm setup:env          crea/completa los .env
//   pnpm setup:env:check    no escribe nada; falla si falta algo, queda un CAMBIAME o el
//                           .env del backend no coincide con el de la raíz

import { randomBytes, randomInt } from 'node:crypto';
import { chmodSync, existsSync, readFileSync, writeFileSync } from 'node:fs';
import { dirname, join, resolve } from 'node:path';
import { fileURLToPath, pathToFileURL } from 'node:url';

const PLACEHOLDER = 'CAMBIAME';
const ALFANUMERICO = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789';
const LINEA = /^([A-Za-z_][A-Za-z0-9_]*)=(.*)$/;

// Clave del .env del backend -> clave de la raíz de la que toma el valor.
// Las que no figuran acá se buscan con el mismo nombre en la raíz.
const ALIAS_BACKEND = {
  SPRING_DATASOURCE_PASSWORD: 'APP_RUNTIME_PASSWORD',
  SPRING_FLYWAY_PASSWORD: 'POSTGRES_PASSWORD',
  SPRING_DATA_REDIS_PASSWORD: 'REDIS_PASSWORD',
};

// * Claves del backend que TIENEN que valer lo mismo que en la raíz. Lista explícita:
// * compartir el nombre no alcanza (CORS_ALLOWED_ORIGINS es el de producción en la
// * raíz y el de desarrollo en el backend).
const DEBEN_COINCIDIR = {
  ...ALIAS_BACKEND,
  SPRING_FLYWAY_USER: 'POSTGRES_USER',
  APP_RUNTIME_PASSWORD: 'APP_RUNTIME_PASSWORD',
  INITIAL_ADMIN_EMAIL: 'INITIAL_ADMIN_EMAIL',
  INITIAL_ADMIN_PASSWORD: 'INITIAL_ADMIN_PASSWORD',
  MINIO_ROOT_USER: 'MINIO_ROOT_USER',
  MINIO_ROOT_PASSWORD: 'MINIO_ROOT_PASSWORD',
  JWT_SECRET: 'JWT_SECRET',
  REFRESH_TOKEN_SECRET: 'REFRESH_TOKEN_SECRET',
};

// Valores del example que son de ejemplo y conviene personalizar (solo se avisa).
const AVISOS = { INITIAL_ADMIN_EMAIL: 'admin@midominio.com' };

const ARCHIVOS = [
  { dir: '', nombre: '.env', tipo: 'raiz' },
  { dir: 'projects/backend', nombre: 'projects/backend/.env', tipo: 'backend' },
  { dir: 'projects/frontend', nombre: 'projects/frontend/.env', tipo: 'plano' },
  { dir: 'projects/mobile', nombre: 'projects/mobile/.env', tipo: 'plano' },
];

export function esPlaceholder(valor) {
  return valor.trim().startsWith(PLACEHOLDER);
}

/** Claves de firma (JWT y refresh): 64 caracteres base64url. El resto: 32 alfanuméricos (sirven en URLs, SQL y compose sin escapar). */
export function generarSecreto(clave) {
  if (clave === 'JWT_SECRET' || clave === 'REFRESH_TOKEN_SECRET') return randomBytes(48).toString('base64url');
  let s = '';
  for (let i = 0; i < 32; i++) s += ALFANUMERICO[randomInt(ALFANUMERICO.length)];
  return s;
}

/** Claves activas (no comentadas) de un .env -> Map clave => valor. */
export function leerClaves(texto) {
  const mapa = new Map();
  for (const linea of texto.split(/\r?\n/)) {
    const m = LINEA.exec(linea);
    if (m) mapa.set(m[1], m[2]);
  }
  return mapa;
}

/**
 * Devuelve el texto final de un .env.
 * - `existente` null: parte del example.
 * - `existente` con texto: conserva todo y solo reemplaza los CAMBIAME y agrega las claves que faltan.
 * `resolver(clave)` da el valor de reemplazo de un placeholder.
 */
export function completar(existente, ejemplo, resolver) {
  const base = existente ?? ejemplo;
  const eol = base.includes('\r\n') ? '\r\n' : '\n';
  const secretos = [];
  const agregadas = [];

  const lineas = base.split(/\r?\n/).map((linea) => {
    const m = LINEA.exec(linea);
    if (m && esPlaceholder(m[2])) {
      secretos.push(m[1]);
      return `${m[1]}=${resolver(m[1])}`;
    }
    return linea;
  });
  while (lineas.length && lineas[lineas.length - 1] === '') lineas.pop();

  if (existente !== null) {
    const presentes = leerClaves(existente);
    for (const linea of ejemplo.split(/\r?\n/)) {
      const m = LINEA.exec(linea);
      if (!m || presentes.has(m[1])) continue;
      const valor = esPlaceholder(m[2]) ? resolver(m[1]) : m[2];
      if (esPlaceholder(m[2])) secretos.push(m[1]);
      lineas.push(`${m[1]}=${valor}`);
      agregadas.push(m[1]);
    }
  }
  return { texto: lineas.join(eol) + eol, secretos, agregadas };
}

/**
 * Claves de DEBEN_COINCIDIR que valen distinto. Solo nombres:
 * los valores nunca salen de acá. Los CAMBIAME y las claves ausentes se informan aparte.
 */
function diferencias(raiz, backend) {
  return [...backend].flatMap(([clave, valor]) => {
    if (!Object.hasOwn(DEBEN_COINCIDIR, clave)) return [];
    const claveRaiz = DEBEN_COINCIDIR[clave];
    const original = raiz.get(claveRaiz);
    if (original === undefined || esPlaceholder(original) || esPlaceholder(valor)) return [];
    return original === valor ? [] : [`${clave} (raíz: ${claveRaiz})`];
  });
}

/** Solo avisa: una URL local distinta puede ser a propósito (otro Postgres de la máquina). */
function avisosDeConexion(raiz, backend) {
  const url = /^jdbc:postgresql:\/\/(?:localhost|127\.0\.0\.1)(?::(\d+))?\/([^?]+)/.exec(
    backend.get('SPRING_DATASOURCE_URL') ?? '',
  );
  if (!url) return [];
  const [, puerto = '5432', base] = url;
  const avisos = [];
  if (raiz.has('POSTGRES_DB') && base !== raiz.get('POSTGRES_DB')) {
    avisos.push('SPRING_DATASOURCE_URL apunta a una base distinta de POSTGRES_DB');
  }
  if (puerto !== (raiz.get('POSTGRES_HOST_PORT') || '5432')) {
    avisos.push('SPRING_DATASOURCE_URL usa un puerto distinto del que publica Docker (POSTGRES_HOST_PORT, o 5432)');
  }
  return avisos;
}

/**
 * Crea/completa los .env bajo `root`. Con `soloVerificar` no escribe nada.
 * Devuelve un reporte sin valores secretos.
 */
export function prepararEntornos({ root, soloVerificar = false }) {
  const reporte = [];
  let valoresRaiz = new Map();

  for (const { dir, nombre, tipo } of ARCHIVOS) {
    const rutaEnv = join(root, dir, '.env');
    const rutaEjemplo = join(root, dir, '.env.example');
    if (!existsSync(rutaEjemplo)) {
      reporte.push({ archivo: nombre, estado: 'sin-example', secretos: 0, agregadas: 0, distintas: [], avisos: [] });
      continue;
    }
    const ejemplo = readFileSync(rutaEjemplo, 'utf8');
    const existente = existsSync(rutaEnv) ? readFileSync(rutaEnv, 'utf8') : null;

    const resolver = (clave) => {
      if (tipo === 'backend') {
        const delaRaiz = valoresRaiz.get(ALIAS_BACKEND[clave] ?? clave);
        if (delaRaiz !== undefined && !esPlaceholder(delaRaiz)) return delaRaiz;
      }
      return generarSecreto(clave);
    };

    if (soloVerificar) {
      const claves = leerClaves(existente ?? '');
      if (tipo === 'raiz') valoresRaiz = claves;
      const distintas = tipo === 'backend' ? diferencias(valoresRaiz, claves) : [];
      const pendientes = [...claves].filter(([, v]) => esPlaceholder(v)).map(([k]) => k);
      const faltan = [...leerClaves(ejemplo).keys()].filter((k) => !claves.has(k));
      const incompleto = pendientes.length || faltan.length;
      const estado = existente === null ? 'falta' : incompleto ? 'pendiente' : distintas.length ? 'distinto' : 'ok';
      const avisos = tipo === 'backend' ? avisosDeConexion(valoresRaiz, claves) : [];
      reporte.push({ archivo: nombre, estado, secretos: pendientes.length, agregadas: faltan.length, distintas, avisos });
      continue;
    }

    const { texto, secretos, agregadas } = completar(existente, ejemplo, resolver);
    // * Un archivo existente solo se reescribe si de verdad se completó algo: normalizar
    // * el salto de línea final no cuenta como cambio.
    const huboCambios = secretos.length > 0 || agregadas.length > 0;
    const estado = existente === null ? 'creado' : huboCambios ? 'completado' : 'sin-cambios';
    if (estado !== 'sin-cambios') {
      writeFileSync(rutaEnv, texto, { encoding: 'utf8', mode: 0o600 });
      try {
        chmodSync(rutaEnv, 0o600);
      } catch {
        // Windows: chmod no aplica; el .env queda con los permisos de la carpeta.
      }
    }
    if (tipo === 'raiz') valoresRaiz = leerClaves(texto);

    const finales = leerClaves(texto);
    const avisos = Object.entries(AVISOS)
      .filter(([k, v]) => finales.get(k) === v)
      .map(([k]) => `${k} conserva el valor de ejemplo: cambialo por uno real`);
    let distintas = [];
    if (tipo === 'backend') {
      distintas = diferencias(valoresRaiz, finales);
      avisos.push(...avisosDeConexion(valoresRaiz, finales));
    }
    reporte.push({ archivo: nombre, estado, secretos: secretos.length, agregadas: agregadas.length, distintas, avisos });
  }
  return reporte;
}

const ETIQUETA = {
  creado: '✔ creado',
  completado: '✔ completado',
  'sin-cambios': '· sin cambios',
  'sin-example': '! no tiene .env.example',
  ok: '✔ ok',
  falta: '✖ no existe',
  pendiente: '✖ incompleto',
  distinto: '✖ no coincide con la raíz',
};

function main() {
  const args = process.argv.slice(2);
  const soloVerificar = args.includes('--check');
  const i = args.indexOf('--root');
  const aqui = dirname(fileURLToPath(import.meta.url));
  const root = i >= 0 ? resolve(args[i + 1]) : resolve(aqui, '..');

  const reporte = prepararEntornos({ root, soloVerificar });
  for (const r of reporte) {
    const detalle = [];
    if (r.secretos) detalle.push(`${r.secretos} ${soloVerificar ? 'con CAMBIAME' : 'secretos generados'}`);
    if (r.agregadas) detalle.push(`${r.agregadas} claves ${soloVerificar ? 'faltan' : 'agregadas'}`);
    console.log(`${ETIQUETA[r.estado]}  ${r.archivo}${detalle.length ? ` (${detalle.join(', ')})` : ''}`);
    for (const k of r.distintas) console.log(`    ≠ ${k}`);
    for (const a of r.avisos) console.log(`    ! ${a}`);
  }

  if (soloVerificar) {
    const falta = reporte.some((r) => r.estado === 'falta' || r.estado === 'pendiente');
    const distinto = reporte.some((r) => r.distintas.length > 0);
    if (falta) console.error('\nFaltan variables de entorno. Corré: pnpm setup:env');
    if (distinto) {
      console.error('\nHay valores distintos entre los .env. setup:env no pisa valores: igualalos a mano.');
    }
    if (falta || distinto) process.exit(1);
    return;
  }
  console.log('\nListo. Los secretos no se muestran: están en cada .env (ignorado por git).');
}

if (process.argv[1] && import.meta.url === pathToFileURL(resolve(process.argv[1])).href) main();
