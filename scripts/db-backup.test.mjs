import assert from 'node:assert/strict';
import { existsSync, mkdirSync, mkdtempSync, rmSync, statSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { afterEach, beforeEach, describe, it } from 'node:test';

import { hacerBackup, listarRespaldos, marcaDeTiempo, verificarBackup } from './db-backup.mjs';

let dir;
beforeEach(() => {
  dir = mkdtempSync(join(tmpdir(), 'db-backup-'));
});
afterEach(() => rmSync(dir, { recursive: true, force: true }));

/** Base falsa que registra qué se le pidió, en orden. */
function dbFalsa({ contenido = 'PGDMP-datos', consultas = {}, falla } = {}) {
  const llamadas = [];
  return {
    llamadas,
    pgDump: async (usuario, base, salida) => {
      llamadas.push(['pgDump', usuario, base]);
      if (falla === 'pgDump') throw new Error('boom');
      writeFileSync(salida, contenido);
    },
    crearBase: async (u, n) => llamadas.push(['crearBase', n]),
    restaurar: async (u, n, entrada) => {
      llamadas.push(['restaurar', n]);
      if (falla === 'restaurar') throw new Error('restore roto');
    },
    consulta: async (u, n, sql) => {
      llamadas.push(['consulta', n]);
      for (const [clave, valor] of Object.entries(consultas)) if (sql.includes(clave)) return `${valor}\n`;
      return '0\n';
    },
    borrarBase: async (u, n) => llamadas.push(['borrarBase', n]),
  };
}

describe('marcaDeTiempo', () => {
  it('formatea en UTC como AAAAMMDD-HHMMSS', () => {
    assert.equal(marcaDeTiempo(new Date('2026-11-25T08:15:30Z')), '20261125-081530');
  });
});

describe('hacerBackup', () => {
  it('crea <base>-<fecha>.dump con el contenido del volcado', async () => {
    const db = dbFalsa();
    const r = await hacerBackup(db, { usuario: 'admin', base: 'app_db', dir, ahora: new Date('2026-11-25T08:15:30Z') });
    assert.equal(r.archivo, join(dir, 'app_db-20261125-081530.dump'));
    assert.ok(statSync(r.archivo).size > 0);
    assert.deepEqual(db.llamadas[0], ['pgDump', 'admin', 'app_db']);
  });

  it('si el volcado queda vacío falla y no deja el archivo', async () => {
    await assert.rejects(
      hacerBackup(dbFalsa({ contenido: '' }), { usuario: 'admin', base: 'app_db', dir }),
      /vacío/,
    );
    assert.deepEqual(listarRespaldos(dir, 'app_db'), []);
  });

  it('si pg_dump falla no deja un archivo a medias', async () => {
    await assert.rejects(hacerBackup(dbFalsa({ falla: 'pgDump' }), { usuario: 'admin', base: 'app_db', dir }), /boom/);
    assert.deepEqual(listarRespaldos(dir, 'app_db'), []);
  });

  it('con conservar borra solo los más viejos de esa base', async () => {
    for (const f of ['20261120-000000', '20261121-000000', '20261122-000000']) {
      writeFileSync(join(dir, `app_db-${f}.dump`), 'x');
    }
    writeFileSync(join(dir, 'otra_db-20261101-000000.dump'), 'x');
    writeFileSync(join(dir, 'notas.txt'), 'x');

    const r = await hacerBackup(dbFalsa(), {
      usuario: 'admin',
      base: 'app_db',
      dir,
      ahora: new Date('2026-11-25T00:00:00Z'),
      conservar: 2,
    });
    assert.equal(r.borrados.length, 2);
    assert.deepEqual(listarRespaldos(dir, 'app_db'), ['app_db-20261125-000000.dump', 'app_db-20261122-000000.dump']);
    assert.ok(existsSync(join(dir, 'otra_db-20261101-000000.dump')), 'no toca otras bases');
    assert.ok(existsSync(join(dir, 'notas.txt')), 'no toca archivos ajenos');
  });

  it('rechaza nombres con caracteres raros', async () => {
    await assert.rejects(hacerBackup(dbFalsa(), { usuario: 'a; rm -rf /', base: 'app_db', dir }), /inválido/);
    await assert.rejects(hacerBackup(dbFalsa(), { usuario: 'admin', base: '../x', dir }), /inválido/);
  });
});

describe('verificarBackup', () => {
  const consultasOk = { flyway_schema_history: 3, information_schema: 12, 'authentication.usuario': 1 };

  beforeEach(() => mkdirSync(dir, { recursive: true }));

  it('crea una base temporal, restaura, comprueba y la borra; devuelve los conteos', async () => {
    writeFileSync(join(dir, 'app_db-20261125-000000.dump'), 'x');
    const db = dbFalsa({ consultas: consultasOk });
    const r = await verificarBackup(db, { usuario: 'admin', base: 'app_db', dir, ahora: 123 });
    assert.deepEqual({ m: r.migraciones, t: r.tablas, u: r.usuarios }, { m: 3, t: 12, u: 1 });
    const pasos = db.llamadas.map((l) => l[0]);
    assert.equal(pasos[0], 'crearBase');
    assert.equal(pasos.at(-1), 'borrarBase');
    assert.ok(db.llamadas.every((l) => l[0] === 'pgDump' || l[1] === 'verify_123'), 'nunca usa la base real');
  });

  it('usa el respaldo más reciente cuando no se indica uno', async () => {
    writeFileSync(join(dir, 'app_db-20261120-000000.dump'), 'x');
    writeFileSync(join(dir, 'app_db-20261124-000000.dump'), 'x');
    const r = await verificarBackup(dbFalsa({ consultas: consultasOk }), { usuario: 'admin', base: 'app_db', dir });
    assert.ok(r.archivo.endsWith('app_db-20261124-000000.dump'));
  });

  it('borra la base temporal aunque la restauración falle', async () => {
    writeFileSync(join(dir, 'app_db-20261125-000000.dump'), 'x');
    const db = dbFalsa({ falla: 'restaurar' });
    await assert.rejects(verificarBackup(db, { usuario: 'admin', base: 'app_db', dir }), /restore roto/);
    assert.equal(db.llamadas.at(-1)[0], 'borrarBase');
  });

  it('falla si la base restaurada no tiene migraciones', async () => {
    writeFileSync(join(dir, 'app_db-20261125-000000.dump'), 'x');
    const db = dbFalsa({ consultas: { ...consultasOk, flyway_schema_history: 0 } });
    await assert.rejects(verificarBackup(db, { usuario: 'admin', base: 'app_db', dir }), /migraciones/);
    assert.equal(db.llamadas.at(-1)[0], 'borrarBase');
  });

  it('sin respaldos da un mensaje claro y no crea nada', async () => {
    const db = dbFalsa();
    await assert.rejects(verificarBackup(db, { usuario: 'admin', base: 'app_db', dir }), /pnpm db:backup/);
    assert.deepEqual(db.llamadas, []);
  });
});
