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

  /** Docker simulado: `restos` es lo que devuelve la búsqueda por etiqueta. */
  function dockerSimulado({ restos = '', fallaAlCrear = false, fallosAlBorrar = 0 } = {}) {
    const llamadas = [];
    let fallos = fallosAlBorrar;
    const docker = (args) => {
      llamadas.push(args);
      if (args[0] === 'ps') return restos;
      if (args[0] === 'rm' && fallos-- > 0) throw new Error('interrumpido');
      if (args[0] === 'run' && fallaAlCrear) throw new Error('Conflict. The container name is already in use');
      return `id-${llamadas.length}`;
    };
    const borrados = () => llamadas.filter((a) => a[0] === 'rm').map((a) => a.at(-1));
    return { docker, llamadas, borrados };
  }

  it('solo borra lo que creó, por su identificador', () => {
    const { docker, llamadas, borrados } = dockerSimulado();
    const registro = crearRegistro(docker, 'abc');
    registro.crear('postgres', ['imagen']);
    registro.crear('redis', ['imagen']);

    registro.limpiar();
    registro.limpiar();

    assert.deepEqual(borrados(), ['id-2', 'id-1']);
    assert.ok(llamadas.filter((a) => a[0] === 'rm').every((a) => a.slice(0, 3).join(' ') === 'rm -f -v'));
  });

  it('además solo busca por la etiqueta de esta ejecución', () => {
    const { docker, llamadas } = dockerSimulado();
    const registro = crearRegistro(docker, 'abc');
    registro.crear('postgres', ['imagen']);

    registro.limpiar();

    assert.deepEqual(llamadas.find((a) => a[0] === 'ps'), ['ps', '-a', '-q', '--filter', 'label=e2e-nav-run=abc']);
    assert.ok(llamadas[0].includes('e2e-nav-run=abc'));
  });

  it('cada ejecución usa una etiqueta distinta', () => {
    const etiquetas = [1, 2].map(() => {
      const { docker, llamadas } = dockerSimulado();
      crearRegistro(docker).crear('postgres', []);
      return llamadas[0].find((a) => a.startsWith('e2e-nav-run='));
    });

    assert.notEqual(etiquetas[0], etiquetas[1]);
  });

  it('borra el contenedor que quedó creado aunque la orden se interrumpiera', () => {
    const { docker, borrados } = dockerSimulado({ fallaAlCrear: true, restos: 'huerfano' });
    const registro = crearRegistro(docker, 'abc');
    assert.throws(() => registro.crear('postgres', []));

    registro.limpiar();

    assert.deepEqual(borrados(), ['huerfano']);
  });

  it('si crear falla porque el contenedor es de otro, no borra nada', () => {
    // * El ajeno no lleva nuestra etiqueta: la búsqueda no lo devuelve.
    const { docker, borrados } = dockerSimulado({ fallaAlCrear: true });
    const registro = crearRegistro(docker, 'abc');
    assert.throws(() => registro.crear('postgres', []), /already in use/);

    registro.limpiar();

    assert.deepEqual(borrados(), []);
  });

  it('sin haber intentado crear nada, no llama a Docker', () => {
    const { docker, llamadas } = dockerSimulado();

    crearRegistro(docker).limpiar();

    assert.deepEqual(llamadas, []);
  });

  it('si borrar se interrumpe, reintenta y sigue con los demás', () => {
    const { docker, borrados } = dockerSimulado({ fallosAlBorrar: 1 });
    const registro = crearRegistro(docker, 'abc');
    registro.crear('postgres', []);
    registro.crear('redis', []);

    registro.limpiar();

    assert.deepEqual(borrados(), ['id-2', 'id-2', 'id-1']);
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

  it('sigue atendiendo las señales mientras limpia', () => {
    const codigo = readFileSync(new URL('./e2e.mjs', import.meta.url), 'utf8');

    assert.ok(codigo.includes('process.on(senal'));
    assert.ok(!codigo.includes('process.once(senal'));
  });

  it('publica los puertos de los contenedores solo en 127.0.0.1 y no lee ningún .env', () => {
    const codigo = readFileSync(new URL('./e2e.mjs', import.meta.url), 'utf8');
    const publicaciones = [...codigo.matchAll(/'-p', `([^`]+)`/g)].map((m) => m[1]);

    assert.equal(publicaciones.length, 4);
    assert.ok(publicaciones.every((p) => p.startsWith('127.0.0.1:')), publicaciones.join(', '));
    assert.ok(!/readFileSync\([^)]*\.env/.test(codigo));
  });
});
