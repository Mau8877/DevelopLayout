// Protecciones y piezas puras de la prueba de producción. No usa Docker.
import { describe, it } from 'node:test';
import assert from 'node:assert/strict';

import { PROYECTO, crearJarra, overrideCompose, validarAislamiento } from './prod-smoke.mjs';

const configAislada = () => ({
  name: PROYECTO,
  services: { backend: { container_name: 'app-backend' }, postgres: { container_name: 'app-postgres' } },
  volumes: { postgres_data: { name: `${PROYECTO}-postgres_data` } },
  networks: { 'app-network': { name: `${PROYECTO}-network` } },
});

describe('validarAislamiento (se comprueba antes de levantar o borrar nada)', () => {
  it('acepta una configuración con nombres propios y sin contenedores previos', () => {
    assert.doesNotThrow(() => validarAislamiento(configAislada(), ['otro-contenedor']));
  });

  for (const [caso, romper, mensaje] of [
    ['un volumen real del proyecto', (c) => (c.volumes.postgres_data.name = 'app-prod-postgres_data'), /volumen/],
    ['un volumen nuevo sin renombrar', (c) => (c.volumes.otro = { name: 'app-otro' }), /volumen/],
    ['un volumen externo', (c) => (c.volumes.postgres_data.external = true), /volumen/],
    ['una red sin prefijo', (c) => (c.networks['app-network'].name = 'app-prod-network'), /red/],
    ['otro nombre de proyecto', (c) => (c.name = 'app-prod'), /proyecto/],
    ['una configuración sin volúmenes (no se leyó bien)', (c) => (c.volumes = {}), /volumen/],
  ]) {
    it(`se niega con ${caso}`, () => {
      const config = configAislada();
      romper(config);
      assert.throws(() => validarAislamiento(config, []), mensaje);
    });
  }

  it('se niega si ya existe un contenedor con el mismo nombre', () => {
    assert.throws(() => validarAislamiento(configAislada(), ['app-postgres']), /app-postgres/);
  });
});

describe('overrideCompose', () => {
  it('reemplaza los puertos del proxy (no los suma) y publica solo en 127.0.0.1', () => {
    const yml = overrideCompose(['postgres_data'], ['app-network'], { http: 18080, https: 18443 });
    assert.match(yml, /ports: !override\n\s+- '127\.0\.0\.1:18080:8080'\n\s+- '127\.0\.0\.1:18443:8443'/);
    assert.match(yml, new RegExp(`postgres_data:\\n    name: ${PROYECTO}-postgres_data`));
    assert.match(yml, new RegExp(`app-network:\\n    name: ${PROYECTO}-network`));
  });
});

describe('crearJarra (cookies)', () => {
  it('guarda, reemplaza y borra con Max-Age=0', () => {
    const jarra = crearJarra();
    jarra.guardar(['a=1; Path=/; HttpOnly', 'b=2; Path=/api']);
    jarra.guardar(['a=3; Path=/']);
    assert.equal(jarra.cabecera(), 'a=3; b=2');
    jarra.guardar(['b=; Max-Age=0; Path=/api']);
    assert.equal(jarra.cabecera(), 'a=3');
    assert.equal(jarra.valor('a'), '3');
  });

  it('la copia no cambia cuando cambia la original', () => {
    const jarra = crearJarra();
    jarra.guardar(['a=1']);
    const copia = jarra.copiar();
    jarra.guardar(['a=; Max-Age=0']);
    assert.equal(copia.cabecera(), 'a=1');
  });
});
