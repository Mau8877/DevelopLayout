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
//   pnpm setup:env:check    no escribe nada; falla si falta algo o queda un CAMBIAME

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

/** JWT_SECRET: 64 caracteres base64url. El resto: 32 alfanuméricos (sirven en URLs, SQL y compose sin escapar). */
export function generarSecreto(clave) {
  if (clave === 'JWT_SECRET') return randomBytes(48).toString('base64url');
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
      reporte.push({ archivo: nombre, estado: 'sin-example', secretos: 0, agregadas: 0, avisos: [] });
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
      const pendientes = [...claves].filter(([, v]) => esPlaceholder(v)).map(([k]) => k);
      const faltan = [...leerClaves(ejemplo).keys()].filter((k) => !claves.has(k));
      const estado = existente === null ? 'falta' : pendientes.length || faltan.length ? 'pendiente' : 'ok';
      reporte.push({ archivo: nombre, estado, secretos: pendientes.length, agregadas: faltan.length, avisos: [] });
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
    reporte.push({ archivo: nombre, estado, secretos: secretos.length, agregadas: agregadas.length, avisos });
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
    for (const a of r.avisos) console.log(`    ! ${a}`);
  }

  if (soloVerificar) {
    const mal = reporte.some((r) => r.estado === 'falta' || r.estado === 'pendiente');
    if (mal) {
      console.error('\nFaltan variables de entorno. Corré: pnpm setup:env');
      process.exit(1);
    }
    return;
  }
  console.log('\nListo. Los secretos no se muestran: están en cada .env (ignorado por git).');
}

if (process.argv[1] && import.meta.url === pathToFileURL(resolve(process.argv[1])).href) main();
