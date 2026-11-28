#!/usr/bin/env node
// Levanta docker-compose.prod.yml completo con datos descartables y comprueba que funciona,
// incluida la recuperación de un respaldo en un servidor vacío.
//
//   pnpm test:prod        (necesita Docker y openssl; la corre el job `docker` de CI)
//
// ! BORRA volúmenes: solo los suyos. Antes de levantar o borrar nada comprueba sobre la
// ! configuración final de Compose que todos los volúmenes y redes llevan su prefijo y que no
// ! hay contenedores con los mismos nombres (los compose fijan `container_name`).
// * No lee ni escribe el .env de la raíz: genera secretos, certificado y override en una
// * carpeta temporal que borra al terminar.

import { execFileSync, spawnSync } from 'node:child_process';
import { chmodSync, existsSync, mkdtempSync, readFileSync, rmSync, writeFileSync } from 'node:fs';
import https from 'node:https';
import http from 'node:http';
import { tmpdir } from 'node:os';
import { dirname, join, resolve } from 'node:path';
import { fileURLToPath, pathToFileURL } from 'node:url';

import { crearDocker, hacerBackup, restaurarBackup, verificarBackup } from './db-backup.mjs';
import { generarSecreto } from './setup-env.mjs';

export const PROYECTO = 'e2eprod-ci';
const PUERTOS = { http: 18080, https: 18443 };
const ADMIN = 'admin@e2e.test';

/** Lanza si usar esta configuración pudiera tocar contenedores, volúmenes o redes ajenos. */
export function validarAislamiento(config, contenedoresExistentes) {
  if (config.name !== PROYECTO) throw new Error(`nombre de proyecto inesperado: ${config.name}`);
  const volumenes = Object.values(config.volumes ?? {});
  if (volumenes.length === 0) throw new Error('la configuración no declara ningún volumen: no se pudo verificar');
  for (const v of volumenes) {
    if (v.external || !v.name?.startsWith(`${PROYECTO}-`)) {
      throw new Error(`me niego: el volumen "${v.name}" no es propio de la prueba`);
    }
  }
  for (const red of Object.values(config.networks ?? {})) {
    if (red.external || !red.name?.startsWith(`${PROYECTO}-`)) {
      throw new Error(`me niego: la red "${red.name}" no es propia de la prueba`);
    }
  }
  const existentes = new Set(contenedoresExistentes);
  const chocan = Object.values(config.services ?? {})
    .map((s) => s.container_name)
    .filter((n) => existentes.has(n));
  if (chocan.length) {
    throw new Error(`me niego: ya existen contenedores con esos nombres (${chocan.join(', ')}). Bajá tu stack primero`);
  }
}

export function overrideCompose(volumenes, redes, puertos) {
  return [
    'services:',
    '  proxy:',
    // `!override`: sin esto Compose SUMA los puertos a los 80/443 del archivo base.
    '    ports: !override',
    `      - '127.0.0.1:${puertos.http}:8080'`,
    `      - '127.0.0.1:${puertos.https}:8443'`,
    'volumes:',
    ...volumenes.flatMap((v) => [`  ${v}:`, `    name: ${PROYECTO}-${v}`]),
    'networks:',
    ...redes.flatMap((r) => [`  ${r}:`, `    name: ${PROYECTO}-network`]),
    '',
  ].join('\n');
}

/** Cookies mínimas: nombre y valor; `Max-Age=0` borra. */
export function crearJarra(inicial = []) {
  const cookies = new Map(inicial);
  return {
    guardar(lineas = []) {
      for (const linea of lineas) {
        const [par, ...atributos] = linea.split(';');
        const i = par.indexOf('=');
        const nombre = par.slice(0, i).trim();
        if (atributos.some((a) => /^\s*max-age=0\s*$/i.test(a))) cookies.delete(nombre);
        else cookies.set(nombre, par.slice(i + 1));
      }
    },
    valor: (nombre) => cookies.get(nombre),
    cabecera: () => [...cookies].map(([k, v]) => `${k}=${v}`).join('; '),
    copiar: () => crearJarra([...cookies]),
  };
}

const esperar = (ms) => new Promise((r) => setTimeout(r, ms));

function comprobar(condicion, descripcion, detalle = '') {
  if (!condicion) throw new Error(`falló: ${descripcion}${detalle ? ` (${detalle})` : ''}`);
  console.log(`  ✔ ${descripcion}`);
}

function buscarOpenssl() {
  const candidatos = ['openssl', 'C:/Program Files/Git/usr/bin/openssl.exe', 'C:/Program Files/Git/mingw64/bin/openssl.exe'];
  const hallado = candidatos.find((c) => spawnSync(c, ['version'], { stdio: 'ignore' }).status === 0);
  if (!hallado) throw new Error('no encontré openssl (en Windows viene con Git)');
  return hallado;
}

function crearCertificado(carpeta) {
  const clave = join(carpeta, 'privkey.pem');
  const certificado = join(carpeta, 'fullchain.pem');
  execFileSync(
    buscarOpenssl(),
    ['req', '-x509', '-newkey', 'rsa:2048', '-nodes', '-days', '2', '-subj', '/CN=localhost',
      '-addext', 'subjectAltName=DNS:localhost', '-keyout', clave, '-out', certificado],
    { stdio: 'ignore', env: { ...process.env, MSYS_NO_PATHCONV: '1' } },
  );
  // * El proxy corre como uid 101: en Linux tiene que poder atravesar la carpeta y leer la clave.
  // ! 644 solo porque es una clave descartable de prueba (ver docs/INFRAESTRUCTURA.md).
  chmodSync(carpeta, 0o755);
  chmodSync(clave, 0o644);
  return readFileSync(certificado);
}

function crearEntorno(carpetaCertificados) {
  const secreto = (clave) => generarSecreto(clave);
  return {
    POSTGRES_DB: 'app_db',
    POSTGRES_USER: 'app_admin',
    POSTGRES_PASSWORD: secreto('POSTGRES_PASSWORD'),
    APP_RUNTIME_PASSWORD: secreto('APP_RUNTIME_PASSWORD'),
    REDIS_PASSWORD: secreto('REDIS_PASSWORD'),
    GRAFANA_ADMIN_USER: 'admin',
    GRAFANA_ADMIN_PASSWORD: secreto('GRAFANA_ADMIN_PASSWORD'),
    MINIO_ROOT_USER: 'minio_admin',
    MINIO_ROOT_PASSWORD: secreto('MINIO_ROOT_PASSWORD'),
    MINIO_IMAGE: process.env.MINIO_IMAGE || 'cgr.dev/chainguard/minio:latest',
    INITIAL_ADMIN_EMAIL: ADMIN,
    INITIAL_ADMIN_PASSWORD: secreto('INITIAL_ADMIN_PASSWORD'),
    JWT_SECRET: secreto('JWT_SECRET'),
    REFRESH_TOKEN_SECRET: secreto('REFRESH_TOKEN_SECRET'),
    TLS_CERT_DIR: carpetaCertificados.replaceAll('\\', '/'),
    CORS_ALLOWED_ORIGINS: 'https://localhost',
    // * SMTP inalcanzable a propósito: la app tiene que arrancar y seguir sana igual.
    SPRING_MAIL_HOST: 'smtp.invalid',
    SPRING_MAIL_USERNAME: '',
    SPRING_MAIL_PASSWORD: '',
    MAIL_FROM: 'no-reply@e2e.test',
    APP_FRONTEND_BASE_URL: 'https://localhost',
    APP_VERSION: process.env.APP_VERSION || 'e2e-prod',
  };
}

/** Petición HTTPS al proxy, verificando el certificado de prueba (no se desactiva la verificación). */
function crearCliente(ca) {
  return function pedir(ruta, { method = 'GET', headers = {}, body, jarra, plano = false } = {}) {
    return new Promise((ok, mal) => {
      const cabeceras = { Host: 'localhost', ...headers };
      if (jarra?.cabecera()) cabeceras.Cookie = jarra.cabecera();
      if (body !== undefined) cabeceras['Content-Type'] = 'application/json';
      const req = (plano ? http : https).request(
        { host: '127.0.0.1', port: plano ? PUERTOS.http : PUERTOS.https, path: ruta, method, headers: cabeceras,
          ...(plano ? {} : { ca, servername: 'localhost' }), timeout: 10_000 },
        (res) => {
          let texto = '';
          res.on('data', (d) => (texto += d));
          res.on('end', () => {
            const cookies = res.headers['set-cookie'] ?? [];
            jarra?.guardar(cookies);
            let json;
            try {
              json = JSON.parse(texto);
            } catch {
              json = undefined;
            }
            ok({ status: res.statusCode, headers: res.headers, cookies, texto, json });
          });
        },
      );
      req.on('timeout', () => req.destroy(new Error('timeout')));
      req.on('error', mal);
      req.end(body === undefined ? undefined : JSON.stringify(body));
    });
  };
}

function crearCompose(root, override, entorno) {
  const base = ['compose', '-p', PROYECTO, '-f', 'docker-compose.prod.yml', '-f', override, '--project-directory', '.'];
  // * Las variables del proceso ganan sobre cualquier .env: así nunca se usa el de la raíz.
  const env = { ...process.env, ...entorno };
  return (args, { capturar = false, tolerar = false } = {}) => {
    const r = spawnSync('docker', [...base, ...args], {
      cwd: root, env, encoding: 'utf8', maxBuffer: 32 * 1024 * 1024,
      stdio: capturar ? 'pipe' : ['ignore', 'ignore', 'pipe'],
    });
    if (r.status !== 0 && !tolerar) {
      throw new Error(`docker compose ${args[0]} terminó con código ${r.status}: ${(r.stderr ?? '').trim().split('\n').slice(-8).join(' | ')}`);
    }
    return { codigo: r.status, salida: r.stdout ?? '' };
  };
}

const docker = (...args) => execFileSync('docker', args, { encoding: 'utf8' }).trim();

async function esperarBackend(pedir, segundos = 300) {
  const limite = Date.now() + segundos * 1000;
  while (Date.now() < limite) {
    const [estado, salud, reinicios] = docker(
      'inspect', '-f', '{{.State.Status}} {{if .State.Health}}{{.State.Health.Status}}{{end}} {{.RestartCount}}', 'app-backend',
    ).split(' ');
    if (Number(reinicios) >= 3) throw new Error('el backend está en un bucle de reinicios');
    if (estado === 'running' && salud === 'healthy') {
      // El proxy puede tardar unos segundos más en resolver el contenedor nuevo.
      const r = await pedir('/api/v1/auth/csrf').catch(() => undefined);
      if (r?.status === 204) return;
    }
    await esperar(3000);
  }
  throw new Error(`el backend no quedó disponible en ${segundos} s`);
}

async function comprobarServicios(compose) {
  // * Nombres completos, sin armarlos: así `project:init` los puede renombrar.
  const unaVez = ['minio-permisos', 'minio-init'];
  for (const contenedor of ['app-minio-permisos', 'app-minio-init']) {
    comprobar(docker('wait', contenedor) === '0', `${contenedor} terminó con éxito`);
  }
  const texto = compose(['ps', '-a', '--format', 'json'], { capturar: true }).salida.trim();
  const filas = texto.startsWith('[') ? JSON.parse(texto) : texto.split('\n').map((l) => JSON.parse(l));
  const caidos = filas.filter((f) => !unaVez.includes(f.Service) && f.State !== 'running').map((f) => f.Service);
  comprobar(caidos.length === 0, 'todos los servicios siguen en marcha', caidos.join(', '));
  const publican = filas.filter((f) => (f.Publishers ?? []).some((p) => p.PublishedPort > 0)).map((f) => f.Service);
  comprobar(publican.join(',') === 'proxy', 'solo el proxy publica puertos', publican.join(', '));
}

async function comprobarEntrada(pedir) {
  const plano = await pedir('/ruta', { plano: true });
  comprobar(plano.status === 301 && plano.headers.location === 'https://localhost/ruta', 'HTTP redirige a HTTPS');
  const inicio = await pedir('/');
  const h = inicio.headers;
  comprobar(
    inicio.status === 200 && /max-age=\d+/.test(h['strict-transport-security'] ?? '') &&
      h['x-content-type-options'] === 'nosniff' && h['x-frame-options'] === 'DENY' &&
      /default-src 'self'/.test(h['content-security-policy'] ?? ''),
    'la web responde con HSTS, nosniff, X-Frame-Options y CSP',
  );
  const normal = h['referrer-policy'];
  // Las dos páginas que reciben un token en el enlace de un correo.
  for (const ruta of ['/reset-password', '/accept-invitation']) {
    const cabeceras = (await pedir(ruta)).headers;
    comprobar(
      normal === 'strict-origin-when-cross-origin' && cabeceras['referrer-policy'] === 'no-referrer',
      `${ruta} no manda Referer y el resto conserva su política`,
      `${normal} / ${cabeceras['referrer-policy']}`,
    );
    comprobar(
      ['strict-transport-security', 'content-security-policy', 'x-frame-options', 'x-content-type-options']
        .every((cabecera) => cabeceras[cabecera] === h[cabecera]),
      `${ruta} conserva las demás cabeceras de seguridad`,
    );
  }
  for (const ruta of ['/health', '/prometheus', '/actuator', '/v3/api-docs', '/swagger-ui/index.html']) {
    const r = await pedir(ruta);
    comprobar(!/jvm_|"openapi"|swagger-ui|"status"\s*:\s*"UP"/i.test(r.texto), `${ruta} no expone datos internos`);
  }
}

const login = (pedir, entorno, extra = {}) =>
  pedir('/api/v1/auth/login', {
    method: 'POST', body: { email: ADMIN, password: entorno.INITIAL_ADMIN_PASSWORD }, ...extra,
  });
const comoMobile = { headers: { 'X-Client-Type': 'mobile' } };

/** Recorre la sesión web y deja una sesión mobile abierta (la devuelve). */
async function comprobarSesiones(pedir, entorno) {
  const jarra = crearJarra();
  await pedir('/api/v1/auth/csrf', { jarra });
  const csrf = () => ({ 'X-XSRF-TOKEN': jarra.valor('XSRF-TOKEN'), Origin: 'https://localhost' });

  comprobar((await login(pedir, entorno, { jarra })).status === 403, 'el login web sin token CSRF recibe 403');
  const web = await login(pedir, entorno, { jarra, headers: csrf() });
  comprobar(web.status === 200, 'login web', `status ${web.status}`);
  for (const nombre of ['accessToken', 'refreshToken']) {
    const cookie = web.cookies.find((c) => c.startsWith(`${nombre}=`)) ?? '';
    comprobar(
      /;\s*Secure/i.test(cookie) && /;\s*HttpOnly/i.test(cookie) && /SameSite=Strict/i.test(cookie),
      `cookie ${nombre}: Secure, HttpOnly y SameSite=Strict`,
    );
  }
  comprobar(!/accessToken|refreshToken/.test(web.texto), 'el login web no devuelve tokens en el cuerpo');
  comprobar((await pedir('/api/v1/auth/me', { jarra })).status === 200, '/me con la sesión web');
  comprobar((await pedir('/api/v1/auth/refresh', { method: 'POST', jarra, headers: csrf() })).status === 200, 'refresh web');
  const anterior = jarra.copiar();
  comprobar((await pedir('/api/v1/auth/logout', { method: 'POST', jarra, headers: csrf() })).status === 200, 'logout web');
  comprobar((await pedir('/api/v1/auth/me', { jarra: anterior })).status === 401, 'la sesión cerrada deja de valer de inmediato');

  const ajeno = await login(pedir, entorno, { headers: { 'X-Client-Type': 'mobile', Origin: 'https://atacante.test' } });
  comprobar(ajeno.status === 403, 'un origen ajeno recibe 403');
  const mobile = await login(pedir, entorno, comoMobile);
  comprobar(mobile.status === 200 && Boolean(mobile.json?.data?.refreshToken), 'login mobile');
  return mobile.json.data.refreshToken;
}

/** Con el correo caído: respuesta uniforme, fallo registrado y la app sigue sana. */
async function comprobarRecuperacion(pedir, compose) {
  const jarra = crearJarra();
  await pedir('/api/v1/auth/csrf', { jarra });
  const olvide = (email) =>
    pedir('/api/v1/auth/password/forgot', {
      method: 'POST', jarra, body: { email },
      headers: { 'X-XSRF-TOKEN': jarra.valor('XSRF-TOKEN'), Origin: 'https://localhost' },
    });
  const sinFecha = (r) => r.texto.replace(/"timestamp":"[^"]+"/, '');
  const existente = await olvide(ADMIN);
  const inexistente = await olvide('nadie@e2e.test');
  comprobar(
    existente.status === 200 && inexistente.status === 200 && sinFecha(existente) === sinFecha(inexistente),
    'pedir el enlace responde igual exista o no la cuenta',
    `${existente.status}/${inexistente.status}`,
  );
  const invalido = await pedir('/api/v1/auth/password/reset', {
    method: 'POST', jarra, body: { token: 'no-existe', password: 'ClaveCualquiera123' }, // gitleaks:allow
    headers: { 'X-XSRF-TOKEN': jarra.valor('XSRF-TOKEN'), Origin: 'https://localhost' },
  });
  comprobar(invalido.status === 400 && invalido.json?.error === 'ERR_AUTH_05', 'un enlace inventado recibe ERR_AUTH_05');

  const limite = Date.now() + 60_000;
  let registrado = false;
  while (!registrado && Date.now() < limite) {
    registrado = compose(['logs', '--no-color', 'backend'], { capturar: true }).salida.includes('No se pudo enviar el enlace');
    if (!registrado) await esperar(2000);
  }
  comprobar(registrado, 'el fallo de envío queda en el log');
  const salud = docker('inspect', '-f', '{{.State.Health.Status}} {{.RestartCount}}', 'app-backend');
  const health = compose(['exec', '-T', 'backend', 'wget', '-qO-', 'http://localhost:8080/health'], { capturar: true }).salida;
  comprobar(salud === 'healthy 0' && /"status":"UP"/.test(health), 'con el SMTP caído el backend sigue sano', `${salud} ${health}`);
}

/** Con el correo caído: la cuenta se crea, se avisa y no puede entrar hasta aceptar la invitación. */
async function comprobarUsuarios(pedir, entorno, compose) {
  const sesion = await login(pedir, entorno, comoMobile);
  comprobar(sesion.status === 200, 'login del administrador', `status ${sesion.status}`);
  const admin = {
    headers: { ...comoMobile.headers, Authorization: `Bearer ${sesion.json.data.accessToken}` },
  };
  const email = 'invitada@e2e.test';
  const alta = await pedir('/api/v1/usuarios', { method: 'POST', body: { email, roles: ['USUARIO'] }, ...admin });
  comprobar(
    alta.status === 201 && alta.json?.data?.invitacionEnviada === false,
    'el alta crea la cuenta y avisa que el correo no salió',
    `${alta.status} ${alta.texto}`,
  );
  const listado = await pedir('/api/v1/usuarios/buscar', { method: 'POST', body: { search: email }, ...admin });
  comprobar(
    listado.json?.data?.length === 1 && listado.json.data[0].invitacionPendiente === true,
    'la cuenta aparece con la invitación pendiente',
  );
  const entrada = await pedir('/api/v1/auth/login', {
    method: 'POST', body: { email, password: 'ClaveCualquiera123' }, ...comoMobile, // gitleaks:allow
  });
  comprobar(entrada.status === 401, 'la cuenta pendiente no puede iniciar sesión', `status ${entrada.status}`);
  const cuenta = `/api/v1/usuarios/${alta.json.data.usuario.codigo}`;
  const cambio = await pedir(`${cuenta}/roles`, { method: 'PUT', body: { roles: ['USUARIO', 'SUPER_ADMIN'] }, ...admin });
  comprobar(
    cambio.status === 200 && cambio.json?.data?.roles?.join() === 'SUPER_ADMIN,USUARIO',
    'se pueden cambiar los roles a través del proxy',
    `${cambio.status} ${cambio.texto}`,
  );
  const baja = await pedir(`${cuenta}/desactivar`, { method: 'POST', ...admin });
  comprobar(baja.status === 200 && baja.json?.data?.estado === 'INACTIVO', 'se puede desactivar', `status ${baja.status}`);
  const salud = docker('inspect', '-f', '{{.State.Health.Status}} {{.RestartCount}}', 'app-backend');
  const health = compose(['exec', '-T', 'backend', 'wget', '-qO-', 'http://localhost:8080/health'], { capturar: true }).salida;
  comprobar(salud === 'healthy 0' && /"status":"UP"/.test(health), 'el backend sigue sano', `${salud} ${health}`);
}

function comprobarBucket(compose) {
  const r = compose(
    ['run', '--rm', '--no-deps', '-T', '--entrypoint', '/bin/sh', 'minio-init', '-c',
      'mc alias set p http://minio:9000 "$MINIO_ROOT_USER" "$MINIO_ROOT_PASSWORD" >/dev/null && mc anonymous get p/app-storage'],
    { capturar: true },
  );
  comprobar(/is `private`/.test(r.salida), 'el bucket de MinIO existe y es privado', r.salida.trim());
}

async function main() {
  const root = resolve(dirname(fileURLToPath(import.meta.url)), '..');
  const carpeta = mkdtempSync(join(tmpdir(), `${PROYECTO}-`));
  let compose;
  let aislado = false;
  let limpio = false;
  const limpiar = () => {
    if (limpio) return;
    limpio = true;
    // * Sin la comprobación de aislamiento superada no se borra nada en Docker.
    if (aislado) compose(['down', '-v', '--remove-orphans'], { tolerar: true });
    rmSync(carpeta, { recursive: true, force: true });
  };
  for (const senal of ['SIGINT', 'SIGTERM']) {
    process.once(senal, () => {
      limpiar();
      process.exit(130);
    });
  }

  try {
    const certificados = mkdtempSync(join(carpeta, 'certs-'));
    chmodSync(carpeta, 0o755);
    const ca = crearCertificado(certificados);
    const entorno = crearEntorno(certificados);
    const base = readFileSync(join(root, 'docker-compose.prod.yml'), 'utf8').replaceAll('\r\n', '\n');
    const claves = (seccion) =>
      [...(new RegExp(`^${seccion}:\\n((?:[ #].*\\n|\\n)+)`, 'm').exec(base)?.[1] ?? '').matchAll(/^ {2}([\w-]+):/gm)].map((m) => m[1]);
    const override = join(carpeta, 'override.yml');
    writeFileSync(override, overrideCompose(claves('volumes'), claves('networks'), PUERTOS));
    const envVacio = join(carpeta, 'compose.env');
    writeFileSync(envVacio, Object.entries(entorno).map(([k, v]) => `${k}=${v}\n`).join(''), { mode: 0o600 });
    // db-backup.mjs llama a `docker compose` por su cuenta: estas variables lo apuntan a la prueba.
    Object.assign(process.env, entorno, { COMPOSE_PROJECT_NAME: PROYECTO, COMPOSE_ENV_FILES: envVacio });
    compose = crearCompose(root, override, entorno);

    const config = JSON.parse(compose(['config', '--format', 'json'], { capturar: true }).salida);
    validarAislamiento(config, docker('ps', '-a', '--format', '{{.Names}}').split('\n'));
    aislado = true;
    const pedir = crearCliente(ca);

    console.log('1. Arranque desde cero');
    compose(['down', '-v', '--remove-orphans'], { tolerar: true });
    compose(['build']);
    compose(['up', '-d']);
    await esperarBackend(pedir);
    await comprobarServicios(compose);

    console.log('2. Entrada pública');
    await comprobarEntrada(pedir);

    console.log('3. Sesiones');
    const refreshAbierto = await comprobarSesiones(pedir, entorno);

    console.log('4. Recuperación de contraseña con el correo caído');
    await comprobarRecuperacion(pedir, compose);

    console.log('5. Alta de usuarios con el correo caído');
    await comprobarUsuarios(pedir, entorno, compose);

    console.log('6. Almacenamiento');
    comprobarBucket(compose);

    console.log('7. Respaldo');
    const db = crearDocker({ root, prod: true });
    const datos = { usuario: entorno.POSTGRES_USER, base: entorno.POSTGRES_DB };
    const dir = join(carpeta, 'backups');
    const { archivo } = await hacerBackup(db, { ...datos, dir });
    const verificado = await verificarBackup(db, { ...datos, dir });
    comprobar(verificado.usuarios >= 1, 'respaldo creado y verificado');

    console.log('8. Recuperación en un servidor vacío');
    compose(['down', '-v']);
    compose(['up', '-d', '--wait', 'postgres']);
    // * Durante la inicialización Postgres ya responde por el socket pero va a reiniciarse:
    // * por TCP solo contesta el servidor definitivo.
    for (let i = 0; compose(['exec', '-T', 'postgres', 'pg_isready', '-h', '127.0.0.1'], { tolerar: true, capturar: true }).codigo !== 0; i++) {
      if (i >= 60) throw new Error('Postgres no quedó listo');
      await esperar(2000);
    }
    await restaurarBackup(db, { ...datos, archivo, passwordRuntime: entorno.APP_RUNTIME_PASSWORD, confirmado: true });
    comprobar(true, 'respaldo restaurado');
    compose(['up', '-d']);
    await esperarBackend(pedir);
    comprobar(true, 'el backend arranca sobre la base restaurada');
    const revivida = await pedir('/api/v1/auth/refresh', { method: 'POST', body: { refreshToken: refreshAbierto }, ...comoMobile });
    comprobar(revivida.status === 401, 'una sesión anterior al desastre no revive', `status ${revivida.status}`);
    comprobar((await login(pedir, entorno, comoMobile)).status === 200, 'login después de restaurar');
    await comprobarServicios(compose);

    console.log('\n✔ producción verificada');
  } catch (error) {
    console.error(`\n✖ ${error.message}`);
    if (aislado) {
      // Los secretos de esta corrida son descartables.
      console.error(compose(['logs', '--no-color', '--tail', '40', 'proxy', 'backend', 'postgres', 'minio-init'], { capturar: true, tolerar: true }).salida);
    }
    process.exitCode = 1;
  } finally {
    limpiar();
    if (existsSync(carpeta)) console.error(`! quedó sin borrar ${carpeta}`);
  }
}

if (process.argv[1] && import.meta.url === pathToFileURL(resolve(process.argv[1])).href) main();
