import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { createServer } from 'node:net';
import { describe, it } from 'node:test';

import {
  CONTENEDORES, PREFIJO, PUERTOS, crearRegistro, entornoHeredado, exigirSinChoques, puertoOcupado,
} from './e2e.mjs';

describe('protecciones de las pruebas de navegador', () => {
  it('todos los contenedores llevan el prefijo propio', () => {
    assert.ok(Object.values(CONTENEDORES).every((nombre) => nombre.startsWith(PREFIJO)));
  });

  it('se niega si ya existe un contenedor con uno de sus nombres', () => {
    assert.throws(() => exigirSinChoques(['otro', CONTENEDORES.redis], []), /ya existen contenedores.*e2e-nav-redis/);
  });

  it('se niega si un puerto está ocupado', () => {
    assert.throws(() => exigirSinChoques([], [PUERTOS.web]), /puertos ya están ocupados: 25173/);
  });

  it('sigue si no hay choques, aunque haya otros contenedores', () => {
    assert.doesNotThrow(() => exigirSinChoques(['app-postgres', 'app-redis'], []));
  });

  it('detecta un puerto ocupado de verdad', async () => {
    const servidor = createServer().listen(0, '127.0.0.1');
    await new Promise((ok) => servidor.once('listening', ok));
    const { port } = servidor.address();
    try {
      assert.equal(await puertoOcupado(port), true);
    } finally {
      await new Promise((ok) => servidor.close(ok));
    }
    assert.equal(await puertoOcupado(port), false);
  });

  it('solo borra lo que creó, por su identificador', () => {
    const llamadas = [];
    const registro = crearRegistro((args) => {
      llamadas.push(args);
      return `id-${llamadas.length}`;
    });
    registro.crear('postgres', ['imagen']);
    registro.crear('redis', ['imagen']);

    registro.limpiar();
    registro.limpiar();

    assert.deepEqual(llamadas.filter((a) => a[0] === 'rm'), [['rm', '-f', '-v', 'id-2'], ['rm', '-f', '-v', 'id-1']]);
  });

  it('si crear un contenedor falla, no intenta borrarlo', () => {
    const borrados = [];
    const registro = crearRegistro((args) => {
      if (args[0] === 'rm') borrados.push(args);
      else throw new Error('Conflict. The container name is already in use');
      return '';
    });
    assert.throws(() => registro.crear('postgres', []), /already in use/);

    registro.limpiar();

    assert.deepEqual(borrados, []);
  });

  it('no crea contenedores fuera de su lista', () => {
    assert.throws(() => crearRegistro(() => '').crear('app-postgres', []), /servicio desconocido/);
  });

  it('no hereda la configuración de quien lo ejecuta', () => {
    const heredado = entornoHeredado({
      PATH: '/bin', JAVA_HOME: '/jdk', HOME: '/h', SPRING_DATASOURCE_URL: 'jdbc:real', VITE_API_BASE_URL: 'x',
      JWT_SECRET: 's', APP_COOKIES_SECURE: 'false', MAIL_FROM: 'x', PORT: '1', REDIS_PASSWORD: 'r',
    });

    assert.deepEqual(Object.keys(heredado).sort(), ['HOME', 'JAVA_HOME', 'PATH']);
  });

  it('comprueba los choques antes de crear el primer contenedor', () => {
    const codigo = readFileSync(new URL('./e2e.mjs', import.meta.url), 'utf8');

    assert.ok(codigo.indexOf('exigirSinChoques(existentes, ocupados)') < codigo.indexOf("registro.crear('postgres'"));
  });

  it('publica los puertos de los contenedores solo en 127.0.0.1 y no lee ningún .env', () => {
    const codigo = readFileSync(new URL('./e2e.mjs', import.meta.url), 'utf8');
    const publicaciones = [...codigo.matchAll(/'-p', `([^`]+)`/g)].map((m) => m[1]);

    assert.equal(publicaciones.length, 4);
    assert.ok(publicaciones.every((p) => p.startsWith('127.0.0.1:')), publicaciones.join(', '));
    assert.ok(!/readFileSync\([^)]*\.env/.test(codigo));
  });
});
