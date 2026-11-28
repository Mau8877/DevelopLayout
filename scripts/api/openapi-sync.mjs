#!/usr/bin/env node
// Sincroniza el contrato de la API: descarga el OpenAPI del backend en dev y lo guarda como
// snapshot versionado (docs/api/openapi.json). De ese snapshot salen los tipos del frontend
// y contra él compara el test de contrato del backend.
//
//   pnpm openapi:sync          descarga el snapshot (backend de dev en http://localhost:8080)
//   pnpm openapi:sync --check  falla si el backend en vivo ya no coincide con el snapshot
//   --url <url>                otro origen para /v3/api-docs
//
// * Normaliza (claves ordenadas, sin `servers`) para que el diff de git muestre solo cambios
// * reales del contrato, no el orden en que springdoc devolvió las claves.

import { existsSync, mkdirSync, readFileSync, writeFileSync } from 'node:fs';
import { dirname, join, resolve } from 'node:path';
import { fileURLToPath, pathToFileURL } from 'node:url';

export const RUTA_SNAPSHOT = 'docs/api/openapi.json';

function ordenar(valor) {
  if (Array.isArray(valor)) return valor.map(ordenar);
  if (valor && typeof valor === 'object') {
    return Object.fromEntries(
      Object.keys(valor)
        .sort()
        .map((k) => [k, ordenar(valor[k])]),
    );
  }
  return valor;
}

/** Contrato normalizado: sin `servers` (depende de desde dónde se pidió) y con claves ordenadas. */
export function normalizar(documento) {
  const { servers: _servers, ...resto } = documento;
  return ordenar(resto);
}

export function serializar(documento) {
  return `${JSON.stringify(normalizar(documento), null, 2)}\n`;
}

export async function descargar(url, fetchFn = fetch) {
  let respuesta;
  try {
    respuesta = await fetchFn(url);
  } catch {
    throw new Error(`no pude conectar con ${url}. ¿Está el backend en dev levantado (docker compose up)?`);
  }
  if (!respuesta.ok) {
    throw new Error(
      `${url} respondió ${respuesta.status}. La documentación de la API solo está encendida en dev (SWAGGER_ENABLED=true).`,
    );
  }
  const documento = await respuesta.json();
  if (!documento || typeof documento !== 'object' || !documento.openapi || !documento.paths) {
    throw new Error(`${url} no devolvió un documento OpenAPI válido`);
  }
  return documento;
}

/** Devuelve 'creado' | 'actualizado' | 'sin-cambios' (o 'distinto' con soloVerificar). */
export function guardar(root, documento, { soloVerificar = false } = {}) {
  const ruta = join(root, RUTA_SNAPSHOT);
  const nuevo = serializar(documento);
  const existe = existsSync(ruta);
  const actual = existe ? readFileSync(ruta, 'utf8') : null;
  if (actual === nuevo) return 'sin-cambios';
  if (soloVerificar) return 'distinto';
  mkdirSync(dirname(ruta), { recursive: true });
  writeFileSync(ruta, nuevo);
  return existe ? 'actualizado' : 'creado';
}

async function main() {
  const args = process.argv.slice(2);
  const i = args.indexOf('--url');
  const url = i >= 0 ? args[i + 1] : 'http://localhost:8080/v3/api-docs';
  const root = resolve(dirname(fileURLToPath(import.meta.url)), '..', '..');
  const soloVerificar = args.includes('--check');

  const resultado = guardar(root, await descargar(url), { soloVerificar });
  if (resultado === 'distinto') {
    console.error(`✖ ${RUTA_SNAPSHOT} está desactualizado respecto al backend. Corré: pnpm openapi:sync`);
    process.exit(1);
  }
  const mensajes = {
    creado: `✔ snapshot creado: ${RUTA_SNAPSHOT}`,
    actualizado: `✔ snapshot actualizado: ${RUTA_SNAPSHOT} (revisá el diff y commitealo junto con el cambio del backend)`,
    'sin-cambios': `✔ ${RUTA_SNAPSHOT} ya está al día`,
  };
  console.log(mensajes[resultado]);
  if (resultado !== 'sin-cambios') console.log('  Siguiente paso: pnpm openapi:types');
}

if (process.argv[1] && import.meta.url === pathToFileURL(resolve(process.argv[1])).href) {
  main().catch((error) => {
    console.error(`✖ ${error.message}`);
    process.exit(1);
  });
}
