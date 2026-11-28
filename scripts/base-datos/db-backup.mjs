#!/usr/bin/env node
// Respaldo de Postgres y prueba de restauración, sobre el contenedor `postgres` de Docker Compose.
//
//   pnpm db:backup               volcado de la base en backups/<base>-<fecha>.dump
//   pnpm db:backup -- --keep 7   igual, y conserva solo los 7 más recientes
//   pnpm db:verify               restaura el último respaldo en una base TEMPORAL, comprueba
//                                que quedó usable y la borra (tu base real no se toca)
//   pnpm db:restore <archivo> --confirmar
//                                REEMPLAZA la base real con el respaldo (backend detenido);
//                                sirve también sobre un servidor vacío
//   agregá --prod para usar docker-compose.prod.yml
//
// * Se hace desde Node y no con `docker compose exec ... > archivo` en PowerShell: la
// * redirección de PowerShell 5 re-codifica la salida y corrompe un volcado binario.
// * El respaldo contiene datos reales (hashes de contraseñas, bitácora): el archivo se crea
// * con permisos 600 y `backups/` está en .gitignore. Guardalo cifrado y fuera de esta máquina.

import { spawn } from 'node:child_process';
import {
  chmodSync,
  createReadStream,
  createWriteStream,
  existsSync,
  mkdirSync,
  readdirSync,
  readFileSync,
  rmSync,
  statSync,
} from 'node:fs';
import { dirname, join, resolve } from 'node:path';
import { fileURLToPath, pathToFileURL } from 'node:url';

import { leerClaves } from '../proyecto/setup-env.mjs';

const IDENTIFICADOR = /^[A-Za-z0-9_]+$/;

function validar(nombre, valor) {
  if (!IDENTIFICADOR.test(valor ?? '')) throw new Error(`${nombre} inválido: solo letras, números y guion bajo`);
  return valor;
}

const dos = (n) => String(n).padStart(2, '0');

/** Marca de tiempo en UTC: 20261125-081530. */
export function marcaDeTiempo(fecha) {
  return (
    `${fecha.getUTCFullYear()}${dos(fecha.getUTCMonth() + 1)}${dos(fecha.getUTCDate())}` +
    `-${dos(fecha.getUTCHours())}${dos(fecha.getUTCMinutes())}${dos(fecha.getUTCSeconds())}`
  );
}

const patronRespaldo = (base) => new RegExp(`^${base}-\\d{8}-\\d{6}\\.dump$`);

/** Respaldos de `base` en `dir`, del más reciente al más viejo. */
export function listarRespaldos(dir, base) {
  if (!existsSync(dir)) return [];
  return readdirSync(dir)
    .filter((n) => patronRespaldo(base).test(n))
    .sort()
    .reverse();
}

/**
 * Hace un respaldo. `db.pgDump(usuario, base, rutaSalida)` escribe el volcado en `rutaSalida`.
 * Con `conservar` borra los respaldos más viejos de esa misma base.
 */
export async function hacerBackup(db, { usuario, base, dir, ahora = new Date(), conservar }) {
  validar('POSTGRES_USER', usuario);
  validar('POSTGRES_DB', base);
  mkdirSync(dir, { recursive: true });
  const archivo = join(dir, `${base}-${marcaDeTiempo(ahora)}.dump`);

  try {
    await db.pgDump(usuario, base, archivo);
    const bytes = existsSync(archivo) ? statSync(archivo).size : 0;
    if (bytes === 0) throw new Error('el volcado quedó vacío');
    try {
      chmodSync(archivo, 0o600);
    } catch {
      // Windows: chmod no aplica.
    }
    const borrados = [];
    if (conservar !== undefined) {
      for (const viejo of listarRespaldos(dir, base).slice(conservar)) {
        rmSync(join(dir, viejo), { force: true });
        borrados.push(viejo);
      }
    }
    return { archivo, bytes, borrados };
  } catch (error) {
    rmSync(archivo, { force: true });
    throw error;
  }
}

/** El volcado trae los permisos de app_runtime, pero no el rol: esto confirma que quedaron aplicados. */
async function runtimeConPermisos(db, usuario, base) {
  const sql = "SELECT has_table_privilege('app_runtime', 'authentication.usuario', 'SELECT')";
  return (await db.consulta(usuario, base, sql)).trim() === 't';
}

/**
 * Restaura un respaldo en una base temporal y comprueba que quedó usable. Siempre borra la
 * temporal al terminar, haya salido bien o mal. No toca la base real.
 */
export async function verificarBackup(db, { usuario, base, dir, archivo, ahora = Date.now() }) {
  validar('POSTGRES_USER', usuario);
  validar('POSTGRES_DB', base);
  const origen = archivo ?? (listarRespaldos(dir, base)[0] && join(dir, listarRespaldos(dir, base)[0]));
  if (!origen || !existsSync(origen)) throw new Error(`no hay respaldos de ${base} en ${dir}. Corré: pnpm db:backup`);

  const temporal = `verify_${ahora}`;
  await db.crearBase(usuario, temporal);
  try {
    await db.restaurar(usuario, temporal, origen);
    const numero = async (sql) => Number((await db.consulta(usuario, temporal, sql)).trim());
    const migraciones = await numero('SELECT count(*) FROM public.flyway_schema_history');
    const tablas = await numero(
      "SELECT count(*) FROM information_schema.tables WHERE table_schema IN ('public','authentication') AND table_type = 'BASE TABLE'",
    );
    const usuarios = await numero('SELECT count(*) FROM authentication.usuario');
    if (!(migraciones > 0)) throw new Error('la base restaurada no tiene historial de migraciones');
    if (!(tablas > 1)) throw new Error('la base restaurada casi no tiene tablas');
    if (!(await runtimeConPermisos(db, usuario, temporal))) {
      throw new Error('el respaldo no trae los permisos de app_runtime: la app no podría usar la base restaurada');
    }
    return { archivo: origen, migraciones, tablas, usuarios };
  } finally {
    await db.borrarBase(usuario, temporal);
  }
}

// * Mismos atributos que la migración que lo crea (V20261125063100). Si ya existe, no se toca.
const sqlRolRuntime = (password) => `DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'app_runtime') THEN
        CREATE ROLE app_runtime LOGIN PASSWORD '${password}' NOSUPERUSER NOBYPASSRLS NOCREATEDB NOCREATEROLE;
    END IF;
END
$$;`;

// * El respaldo "revive" las sesiones cerradas después de hacerlo: se revocan todas.
const SQL_REVOCAR_SESIONES = `UPDATE authentication.sesion
   SET estado = 'REVOCADA', revocada_en = now(), motivo_revocacion = 'ADMIN', updated_at = now()
 WHERE estado = 'ACTIVA';`;

/**
 * Reemplaza la base real con un respaldo. El volcado no incluye roles y su historial de Flyway
 * impide que la migración vuelva a crear `app_runtime`: hay que crearlo ANTES de restaurar.
 * La restauración es una sola transacción: o queda completa o la base no cambia.
 */
export async function restaurarBackup(db, { usuario, base, archivo, passwordRuntime, confirmado }) {
  validar('POSTGRES_USER', usuario);
  validar('POSTGRES_DB', base);
  if (!archivo || !existsSync(archivo)) throw new Error('indicá un archivo de respaldo que exista');
  if (!/^[A-Za-z0-9]+$/.test(passwordRuntime ?? '') || passwordRuntime === 'CAMBIAME') {
    throw new Error('APP_RUNTIME_PASSWORD falta o no es válida en .env (solo letras y números)');
  }
  if (!confirmado) throw new Error(`esto REEMPLAZA la base "${base}". Si es lo que querés, repetí con --confirmar`);
  if (await db.backendActivo()) throw new Error('el backend está en marcha: detenelo antes de restaurar');

  await db.ejecutarSql(usuario, base, sqlRolRuntime(passwordRuntime));
  await db.restaurar(usuario, base, archivo, { limpiar: true });
  await db.ejecutarSql(usuario, base, SQL_REVOCAR_SESIONES);
  if (!(await runtimeConPermisos(db, usuario, base))) {
    throw new Error('se restauró, pero app_runtime quedó sin permisos: no arranques la app');
  }
  return { archivo };
}

/** Adaptador real: ejecuta pg_dump/pg_restore/psql dentro del servicio `postgres` de Compose. */
export function crearDocker({ root, prod }) {
  const compose = ['compose', ...(prod ? ['-f', 'docker-compose.prod.yml'] : [])];
  const enPostgres = [...compose, 'exec', '-T', 'postgres'];

  const lanzar = (nombre, argumentos, { entrada, texto, salida } = {}) =>
    new Promise((resolver, rechazar) => {
      const conEntrada = Boolean(entrada) || texto !== undefined;
      const proceso = spawn('docker', argumentos, { cwd: root, stdio: [conEntrada ? 'pipe' : 'ignore', 'pipe', 'pipe'] });
      let stdout = '';
      let stderr = '';
      const pendientes = [];

      if (salida) {
        const destino = createWriteStream(salida, { mode: 0o600 });
        pendientes.push(new Promise((ok, mal) => destino.on('finish', ok).on('error', mal)));
        proceso.stdout.pipe(destino);
      } else {
        proceso.stdout.on('data', (d) => (stdout += d));
      }
      proceso.stderr.on('data', (d) => (stderr += d));
      if (entrada) {
        const origen = createReadStream(entrada);
        origen.on('error', rechazar);
        proceso.stdin.on('error', () => {}); // EPIPE si pg_restore corta antes de leer todo
        origen.pipe(proceso.stdin);
      } else if (texto !== undefined) {
        proceso.stdin.on('error', () => {});
        proceso.stdin.end(texto);
      }
      proceso.on('error', (e) =>
        rechazar(e.code === 'ENOENT' ? new Error('no encontré `docker` en el PATH') : e),
      );
      proceso.on('close', async (codigo) => {
        try {
          await Promise.all(pendientes);
        } catch (e) {
          return rechazar(e);
        }
        if (codigo !== 0) {
          return rechazar(new Error(`docker ${nombre} terminó con código ${codigo}: ${stderr.trim().split('\n').slice(-5).join(' | ')}`));
        }
        resolver(stdout);
      });
    });

  const ejecutar = (args, opciones) => lanzar(args[0], [...enPostgres, ...args], opciones);

  return {
    backendActivo: async () =>
      (await lanzar('compose ps', [...compose, 'ps', '--status', 'running', '-q', 'backend'])).trim() !== '',
    // * El SQL va por stdin: así una contraseña no aparece en la lista de procesos.
    ejecutarSql: (usuario, nombre, sql) =>
      ejecutar(['psql', '-U', usuario, '-d', nombre, '-v', 'ON_ERROR_STOP=1', '-q', '-f', '-'], { texto: sql }),
    pgDump: (usuario, base, salida) => ejecutar(['pg_dump', '-U', usuario, '-d', base, '-Fc'], { salida }),
    crearBase: (usuario, nombre) => ejecutar(['createdb', '-U', usuario, nombre]),
    restaurar: (usuario, nombre, entrada, { limpiar = false } = {}) =>
      ejecutar(
        [
          'pg_restore', '-U', usuario, '-d', nombre, '--no-owner', '--exit-on-error', '--single-transaction',
          ...(limpiar ? ['--clean', '--if-exists'] : []),
        ],
        { entrada },
      ),
    consulta: (usuario, nombre, sql) => ejecutar(['psql', '-U', usuario, '-d', nombre, '-tA', '-c', sql]),
    borrarBase: (usuario, nombre) => ejecutar(['dropdb', '-U', usuario, '--if-exists', nombre]),
  };
}

function leerOpciones(args) {
  const valor = (flag) => {
    const i = args.indexOf(flag);
    return i >= 0 ? args[i + 1] : undefined;
  };
  const keep = valor('--keep');
  if (keep !== undefined && !(Number.isInteger(Number(keep)) && Number(keep) >= 1)) {
    throw new Error('--keep necesita un número entero mayor o igual a 1');
  }
  const posicional = args.slice(1).find((a, i, todos) => !a.startsWith('--') && todos[i - 1] !== '--keep');
  return { prod: args.includes('--prod'), confirmado: args.includes('--confirmar'), conservar: keep === undefined ? undefined : Number(keep), archivo: posicional };
}

async function main() {
  const [comando, ...resto] = process.argv.slice(2);
  const aqui = dirname(fileURLToPath(import.meta.url));
  const root = resolve(aqui, '..', '..');
  const dir = join(root, 'backups');

  if (!['backup', 'verify', 'restore'].includes(comando)) {
    console.error('Uso: node scripts/base-datos/db-backup.mjs <backup|verify|restore> [archivo] [--keep N] [--prod] [--confirmar]');
    process.exit(2);
  }
  const { prod, confirmado, conservar, archivo } = leerOpciones([comando, ...resto]);

  const rutaEnv = join(root, '.env');
  if (!existsSync(rutaEnv)) throw new Error('no existe .env en la raíz. Corré: pnpm setup:env');
  const env = leerClaves(readFileSync(rutaEnv, 'utf8'));
  const usuario = env.get('POSTGRES_USER');
  const base = env.get('POSTGRES_DB');
  const db = crearDocker({ root, prod });

  if (comando === 'backup') {
    const r = await hacerBackup(db, { usuario, base, dir, conservar });
    console.log(`✔ respaldo creado: ${r.archivo} (${(r.bytes / 1024).toFixed(1)} KB)`);
    for (const b of r.borrados) console.log(`  · borrado respaldo viejo: ${b}`);
    console.log('  Contiene datos reales: guardalo cifrado y fuera de esta máquina.');
    return;
  }
  if (comando === 'restore') {
    const r = await restaurarBackup(db, {
      usuario,
      base,
      archivo: archivo ? resolve(archivo) : undefined,
      passwordRuntime: env.get('APP_RUNTIME_PASSWORD'),
      confirmado,
    });
    console.log(`✔ base restaurada desde ${r.archivo}`);
    console.log('  Se revocaron todas las sesiones. Ya podés levantar la app.');
    return;
  }
  const r = await verificarBackup(db, { usuario, base, dir, archivo: archivo ? resolve(archivo) : undefined });
  console.log(`✔ restauración verificada: ${r.archivo}`);
  console.log(`  ${r.migraciones} migraciones, ${r.tablas} tablas, ${r.usuarios} usuarios (en una base temporal, ya borrada)`);
}

if (process.argv[1] && import.meta.url === pathToFileURL(resolve(process.argv[1])).href) {
  main().catch((error) => {
    console.error(`✖ ${error.message}`);
    process.exit(1);
  });
}
