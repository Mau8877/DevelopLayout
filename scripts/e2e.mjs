#!/usr/bin/env node
// Pruebas de navegador: levanta un entorno desechable (Postgres, Redis y Mailpit en
// contenedores propios, backend y web reales), corre Playwright y lo borra todo.
//
//   pnpm test:e2e                      todas las pruebas
//   pnpm test:e2e sesiones             solo los archivos que coincidan
//
// ! No toca nada ajeno: solo borra los contenedores que él mismo creó en esta ejecución, y si
// ! encuentra uno con sus nombres o un puerto ocupado se detiene sin tocar nada.
// * No lee ningún .env: genera los secretos en cada ejecución y no hereda las variables de
// * configuración de quien lo corre. Todo escucha solo en 127.0.0.1.

import { execFileSync, spawn, spawnSync } from 'node:child_process';
import { randomUUID } from 'node:crypto';
import { copyFileSync, createWriteStream, mkdirSync, mkdtempSync, readdirSync, readFileSync, rmSync } from 'node:fs';
import { createConnection } from 'node:net';
import { tmpdir } from 'node:os';
import { dirname, join, resolve } from 'node:path';
import { fileURLToPath, pathToFileURL } from 'node:url';

import { generarSecreto } from './setup-env.mjs';

export const PREFIJO = 'e2e-nav-';

export const CONTENEDORES = {
  postgres: `${PREFIJO}postgres`,
  redis: `${PREFIJO}redis`,
  mailpit: `${PREFIJO}mailpit`,
};

export const PUERTOS = {
  postgres: 25432,
  redis: 26379,
  smtp: 21025,
  mailpit: 28025,
  backend: 28080,
  web: 25173,
};

// * Lo que quien ejecuta pueda tener definido no puede colarse en el entorno de prueba.
const DE_CONFIGURACION =
  /^(SPRING|APP|VITE|JWT|REFRESH|MINIO|INITIAL|CORS|RATE|MAIL|SWAGGER|LOGIN|BITACORA|SERVER|MANAGEMENT|LOGGING|POSTGRES|REDIS|FLYWAY|E2E)_|^PORT$/i;

export function entornoHeredado(env) {
  return Object.fromEntries(Object.entries(env).filter(([clave]) => !DE_CONFIGURACION.test(clave)));
}

/** Lanza, antes de crear nada, si ya hay algo con nuestros nombres o en nuestros puertos. */
export function exigirSinChoques(contenedoresExistentes, puertosOcupados) {
  const existentes = new Set(contenedoresExistentes);
  const nombres = Object.values(CONTENEDORES).filter((nombre) => existentes.has(nombre));
  if (nombres.length) {
    throw new Error(
      `me niego: ya existen contenedores con esos nombres (${nombres.join(', ')}). ` +
        'No los toco: si son restos de una ejecución interrumpida, bórralos a mano',
    );
  }
  if (puertosOcupados.length) {
    throw new Error(`me niego: estos puertos ya están ocupados: ${puertosOcupados.join(', ')}`);
  }
}

/**
 * Lleva la cuenta de lo creado en esta ejecución y borra solo eso: por su
 * identificador y, además, por una etiqueta ÚNICA de esta ejecución. Nunca por
 * nombre ni por patrón.
 *
 * * La etiqueta cubre la interrupción mientras Docker crea un contenedor: ya
 * * existe, pero su identificador no llegó a anotarse.
 */
export function crearRegistro(docker, corrida = randomUUID()) {
  const etiqueta = `e2e-nav-run=${corrida}`;
  const creados = [];
  let intentos = 0;

  // * Un segundo Ctrl+C también llega al `docker` que está borrando: se reintenta.
  const insistir = (argumentos) => {
    for (let intento = 1; ; intento++) {
      try {
        return docker(argumentos);
      } catch (error) {
        if (intento === 3) {
          console.error(`No se pudo ejecutar «docker ${argumentos.join(' ')}»: ${error.message}`);
          return null;
        }
      }
    }
  };
  // * `-v` borra también el volumen anónimo que declara la imagen de Postgres.
  const borrar = (id) => insistir(['rm', '-f', '-v', id]);

  return {
    crear(servicio, argumentos) {
      const nombre = CONTENEDORES[servicio];
      if (!nombre) throw new Error(`servicio desconocido: ${servicio}`);
      intentos++;
      creados.push(docker(['run', '-d', '--name', nombre, '--label', 'e2e-nav=1', '--label', etiqueta, ...argumentos]));
    },
    limpiar() {
      if (!intentos) return;
      while (creados.length) borrar(creados.pop());
      const restos = insistir(['ps', '-a', '-q', '--filter', `label=${etiqueta}`]);
      for (const id of (restos ?? '').split('\n').filter(Boolean)) borrar(id);
      intentos = 0;
    },
  };
}

/** `true` si algo ya atiende ese puerto en la máquina local. */
export async function puertoOcupado(puerto) {
  const atiende = (host) =>
    new Promise((ok) => {
      const conexion = createConnection({ host, port: puerto });
      const terminar = (valor) => {
        conexion.destroy();
        ok(valor);
      };
      conexion.setTimeout(1000, () => terminar(false));
      conexion.once('connect', () => terminar(true));
      conexion.once('error', () => terminar(false));
    });
  return (await atiende('127.0.0.1')) || (await atiende('::1'));
}

async function esperar(descripcion, comprobar, segundos, abortar = () => false) {
  const limite = Date.now() + segundos * 1000;
  while (Date.now() < limite) {
    if (abortar()) throw new Error(`${descripcion}: el proceso terminó antes de estar listo`);
    try {
      if (await comprobar()) return;
    } catch {
      // Todavía no responde.
    }
    await new Promise((r) => setTimeout(r, 500));
  }
  throw new Error(`${descripcion}: no estuvo listo en ${segundos} segundos`);
}

const responde = async (url) => (await fetch(url, { signal: AbortSignal.timeout(2000) })).ok;

/** Mata un proceso y todo lo que haya lanzado. */
function matar(proceso) {
  if (!proceso?.pid || proceso.exitCode !== null) return;
  if (process.platform === 'win32') {
    spawnSync('taskkill', ['/pid', String(proceso.pid), '/T', '/F'], { stdio: 'ignore' });
  } else {
    try {
      process.kill(-proceso.pid, 'SIGKILL');
    } catch {
      // Ya había terminado.
    }
  }
}

function lanzar(comando, argumentos, opciones) {
  return spawn(comando, argumentos, { detached: process.platform !== 'win32', ...opciones });
}

const terminoDe = (proceso) =>
  new Promise((ok, mal) => {
    proceso.once('error', mal);
    proceso.once('exit', (codigo) => ok(codigo ?? 1));
  });

/** El mismo Java que usa Maven: el de `JAVA_HOME` si está definido. */
export function javaDe(javaHome, plataforma = process.platform) {
  if (!javaHome) return 'java';
  return join(javaHome, 'bin', plataforma === 'win32' ? 'java.exe' : 'java');
}

const docker = (argumentos, env = process.env) =>
  execFileSync('docker', argumentos, { encoding: 'utf8', env, stdio: ['ignore', 'pipe', 'pipe'] }).trim();

async function main(argumentos) {
  const raiz = resolve(dirname(fileURLToPath(import.meta.url)), '..');
  const backend = join(raiz, 'projects/backend');
  const frontend = join(raiz, 'projects/frontend');
  const resultados = join(frontend, 'test-results');

  const existentes = docker(['ps', '-a', '--format', '{{.Names}}']).split('\n').filter(Boolean);
  const ocupados = [];
  for (const puerto of Object.values(PUERTOS)) {
    if (await puertoOcupado(puerto)) ocupados.push(puerto);
  }
  exigirSinChoques(existentes, ocupados);

  const temporal = mkdtempSync(join(tmpdir(), PREFIJO));
  const registro = crearRegistro((args) => docker(args, entornoDocker));
  const procesos = [];
  let limpio = false;
  const limpiar = () => {
    if (limpio) return;
    limpio = true;
    for (const proceso of procesos) matar(proceso);
    try {
      registro.limpiar();
    } finally {
      rmSync(temporal, { recursive: true, force: true, maxRetries: 10, retryDelay: 200 });
    }
  };
  for (const senal of ['SIGINT', 'SIGTERM', 'SIGHUP']) {
    // ! `on` y no `once`: sin oyente, un segundo Ctrl+C mataría el proceso a mitad
    // ! de la limpieza.
    process.on(senal, () => {
      console.error('\nInterrumpido: limpiando…');
      limpiar();
      process.exit(130);
    });
  }

  const duena = generarSecreto('POSTGRES_PASSWORD');
  const runtime = generarSecreto('APP_RUNTIME_PASSWORD');
  const redis = generarSecreto('REDIS_PASSWORD');
  const admin = { correo: 'admin@e2e.test', password: generarSecreto('INITIAL_ADMIN_PASSWORD') };
  // * Los secretos viajan por variables de entorno, no en la línea de comandos.
  const entornoDocker = { ...process.env, POSTGRES_PASSWORD: duena, REDIS_PASSWORD: redis };
  const web = `http://localhost:${PUERTOS.web}`;
  const api = `http://localhost:${PUERTOS.backend}`;
  const mailpit = `http://127.0.0.1:${PUERTOS.mailpit}`;
  const base = entornoHeredado(process.env);

  try {
    console.log('▶ Levantando Postgres, Redis y Mailpit desechables…');
    registro.crear('postgres', ['-p', `127.0.0.1:${PUERTOS.postgres}:5432`, '-e', 'POSTGRES_PASSWORD',
      '-e', 'POSTGRES_USER=app_admin', '-e', 'POSTGRES_DB=app_db', 'postgres:18-alpine']);
    registro.crear('redis', ['-p', `127.0.0.1:${PUERTOS.redis}:6379`, '-e', 'REDIS_PASSWORD', 'redis:8-alpine',
      'sh', '-c', 'exec redis-server --requirepass "$REDIS_PASSWORD" --maxmemory-policy noeviction']);
    registro.crear('mailpit', ['-p', `127.0.0.1:${PUERTOS.smtp}:1025`, '-p', `127.0.0.1:${PUERTOS.mailpit}:8025`,
      'axllent/mailpit:v1.33.0']);
    // * `-h 127.0.0.1`: durante la inicialización Postgres solo atiende por el socket local.
    await esperar('Postgres', () => docker(['exec', CONTENEDORES.postgres, 'pg_isready', '-h', '127.0.0.1', '-U', 'app_admin', '-d', 'app_db']) !== '', 90);
    await esperar('Mailpit', () => responde(`${mailpit}/api/v1/messages`), 60);

    console.log('▶ Compilando el backend…');
    const objetivos = ['-B', '-q', '-DskipTests', 'clean', 'package'];
    // * En Windows un .cmd solo se ejecuta a través de cmd.exe; los argumentos son fijos.
    const [comando, argumentosMaven] = process.platform === 'win32'
      ? ['cmd.exe', ['/d', '/c', join(backend, 'mvnw.cmd'), ...objetivos]]
      // * `MVN` permite usar otro Maven (el CI, si el wrapper no puede descargarlo).
      : [process.env.MVN || './mvnw', objetivos];
    const compilacion = lanzar(comando, argumentosMaven, { cwd: backend, env: base, stdio: 'inherit' });
    procesos.push(compilacion);
    if ((await terminoDe(compilacion)) !== 0) throw new Error('no compiló el backend');
    const jar = readdirSync(join(backend, 'target')).find((n) => n.endsWith('.jar') && !n.endsWith('-plain.jar'));
    if (!jar) throw new Error('no encontré el jar del backend');

    console.log('▶ Arrancando el backend y la web…');
    const registroBackend = createWriteStream(join(temporal, 'backend.log'));
    // * Corre desde la carpeta temporal: así Spring no recoge ningún archivo de configuración local.
    const servidor = lanzar(javaDe(process.env.JAVA_HOME), ['-jar', join(backend, 'target', jar),
      `--server.port=${PUERTOS.backend}`, '--server.address=127.0.0.1',
      // * Vida corta del access token para poder probar la renovación; límites altos porque
      // * todas las pruebas salen de la misma IP.
      '--app.jwt.access-ttl=PT20S',
      '--app.ratelimit.capacity=100000', '--app.ratelimit.refill-per-minute=100000',
      '--app.ratelimit.login-capacity=10000', '--app.ratelimit.login-refill-per-minute=10000',
      '--app.ratelimit.refresh-capacity=10000', '--app.ratelimit.refresh-refill-per-minute=10000',
      '--app.ratelimit.forgot-capacity=10000', '--app.ratelimit.forgot-refill-per-minute=10000',
      '--app.ratelimit.reset-capacity=10000', '--app.ratelimit.reset-refill-per-minute=10000',
      '--app.ratelimit.change-capacity=10000', '--app.ratelimit.change-refill-per-minute=10000',
      // * La web de prueba va por HTTP en localhost.
      '--app.cookies.secure=false',
    ], {
      cwd: temporal,
      stdio: ['ignore', 'pipe', 'pipe'],
      env: {
        ...base,
        SPRING_DATASOURCE_URL: `jdbc:postgresql://127.0.0.1:${PUERTOS.postgres}/app_db`,
        SPRING_DATASOURCE_USERNAME: 'app_runtime',
        SPRING_DATASOURCE_PASSWORD: runtime,
        SPRING_FLYWAY_USER: 'app_admin',
        SPRING_FLYWAY_PASSWORD: duena,
        APP_RUNTIME_PASSWORD: runtime,
        SPRING_DATA_REDIS_HOST: '127.0.0.1',
        SPRING_DATA_REDIS_PORT: String(PUERTOS.redis),
        SPRING_DATA_REDIS_PASSWORD: redis,
        JWT_SECRET: generarSecreto('JWT_SECRET'),
        REFRESH_TOKEN_SECRET: generarSecreto('REFRESH_TOKEN_SECRET'),
        INITIAL_ADMIN_EMAIL: admin.correo,
        INITIAL_ADMIN_PASSWORD: admin.password,
        CORS_ALLOWED_ORIGINS: web,
        SPRING_MAIL_HOST: '127.0.0.1',
        SPRING_MAIL_PORT: String(PUERTOS.smtp),
        SPRING_MAIL_SMTP_AUTH: 'false',
        SPRING_MAIL_SMTP_STARTTLS: 'false',
        MAIL_FROM: 'no-reply@e2e.test',
        APP_FRONTEND_BASE_URL: web,
      },
    });
    procesos.push(servidor);
    servidor.stdout.pipe(registroBackend, { end: false });
    servidor.stderr.pipe(registroBackend, { end: false });

    // * Carpeta vacía para que Vite no lea los .env del proyecto.
    const vite = lanzar(process.execPath, ['node_modules/vite/bin/vite.js', '--host', '127.0.0.1',
      '--port', String(PUERTOS.web), '--strictPort'], {
      cwd: frontend,
      stdio: ['ignore', 'ignore', 'inherit'],
      env: { ...base, E2E_ENV_DIR: temporal, VITE_API_BASE_URL: `${api}/api/v1`, VITE_ENABLE_API_LOGS: 'false' },
    });
    procesos.push(vite);

    try {
      await esperar('Backend', () => responde(`http://127.0.0.1:${PUERTOS.backend}/health`), 180,
        () => servidor.exitCode !== null);
    } catch (error) {
      console.error(readFileSync(join(temporal, 'backend.log'), 'utf8').split('\n').slice(-60).join('\n'));
      throw error;
    }
    await esperar('Web', () => responde(`http://127.0.0.1:${PUERTOS.web}/login`), 90, () => vite.exitCode !== null);

    console.log('▶ Corriendo las pruebas de navegador…');
    const pruebas = lanzar(process.execPath, ['node_modules/@playwright/test/cli.js', 'test',
      '--config', 'e2e/playwright.config.ts', ...argumentos], {
      cwd: frontend,
      stdio: 'inherit',
      env: {
        ...base,
        E2E_WEB_URL: web,
        E2E_API_URL: `${api}/api/v1`,
        E2E_MAILPIT_URL: mailpit,
        E2E_ADMIN_EMAIL: admin.correo,
        E2E_ADMIN_PASSWORD: admin.password,
        ...(process.env.CI ? { CI: process.env.CI } : {}),
      },
    });
    procesos.push(pruebas);
    const codigo = await terminoDe(pruebas);
    if (codigo !== 0) {
      mkdirSync(resultados, { recursive: true });
      copyFileSync(join(temporal, 'backend.log'), join(resultados, 'backend.log'));
    }
    return codigo;
  } finally {
    limpiar();
  }
}

if (process.argv[1] && import.meta.url === pathToFileURL(process.argv[1]).href) {
  main(process.argv.slice(2)).then(
    (codigo) => process.exit(codigo),
    (error) => {
      console.error(`✖ ${error.message}`);
      process.exit(1);
    },
  );
}
