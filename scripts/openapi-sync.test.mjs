import assert from 'node:assert/strict';
import { mkdtempSync, readFileSync, rmSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { afterEach, beforeEach, describe, it } from 'node:test';

import { descargar, guardar, normalizar, RUTA_SNAPSHOT, serializar } from './openapi-sync.mjs';

let root;
beforeEach(() => {
  root = mkdtempSync(join(tmpdir(), 'openapi-'));
});
afterEach(() => rmSync(root, { recursive: true, force: true }));

const doc = (extra = {}) => ({ openapi: '3.1.0', servers: [{ url: 'http://x' }], paths: { '/b': {}, '/a': {} }, ...extra });
const respuesta = (status, cuerpo) => async () => ({ ok: status < 400, status, json: async () => cuerpo });

describe('normalizar', () => {
  it('quita servers y ordena las claves en todos los niveles', () => {
    const n = normalizar(doc());
    assert.equal('servers' in n, false);
    assert.deepEqual(Object.keys(n.paths), ['/a', '/b']);
  });

  it('no reordena arreglos (el orden de una lista sí es parte del contrato)', () => {
    assert.deepEqual(normalizar(doc({ tags: [{ name: 'z' }, { name: 'a' }] })).tags, [{ name: 'z' }, { name: 'a' }]);
  });

  it('misma salida aunque las claves lleguen en otro orden', () => {
    assert.equal(serializar({ openapi: '3', paths: { a: 1, b: 2 } }), serializar({ paths: { b: 2, a: 1 }, openapi: '3' }));
  });
});

describe('guardar', () => {
  it('crea, luego informa sin cambios, luego actualiza', () => {
    assert.equal(guardar(root, doc()), 'creado');
    assert.equal(guardar(root, doc()), 'sin-cambios');
    assert.equal(guardar(root, doc({ info: { title: 'nuevo' } })), 'actualizado');
  });

  it('un cambio solo en servers no cuenta como cambio de contrato', () => {
    guardar(root, doc());
    assert.equal(guardar(root, doc({ servers: [{ url: 'http://otro' }] })), 'sin-cambios');
  });

  it('con soloVerificar no escribe nada', () => {
    assert.equal(guardar(root, doc(), { soloVerificar: true }), 'distinto');
    guardar(root, doc());
    const antes = readFileSync(join(root, RUTA_SNAPSHOT), 'utf8');
    assert.equal(guardar(root, doc({ info: { title: 'x' } }), { soloVerificar: true }), 'distinto');
    assert.equal(readFileSync(join(root, RUTA_SNAPSHOT), 'utf8'), antes);
  });

  it('termina con salto de línea', () => {
    guardar(root, doc());
    assert.ok(readFileSync(join(root, RUTA_SNAPSHOT), 'utf8').endsWith('}\n'));
  });
});

describe('descargar', () => {
  it('devuelve el documento', async () => {
    assert.equal((await descargar('http://x', respuesta(200, doc()))).openapi, '3.1.0');
  });

  it('error claro si no hay backend', async () => {
    await assert.rejects(
      descargar('http://x', async () => {
        throw new Error('ECONNREFUSED');
      }),
      /backend en dev levantado/,
    );
  });

  it('error claro si la documentación está apagada (401/403/404)', async () => {
    await assert.rejects(descargar('http://x', respuesta(404, {})), /SWAGGER_ENABLED/);
  });

  it('rechaza algo que no es OpenAPI', async () => {
    await assert.rejects(descargar('http://x', respuesta(200, { hola: 1 })), /no devolvió un documento OpenAPI/);
  });
});
