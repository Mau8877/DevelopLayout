import assert from 'node:assert/strict';
import { spawnSync } from 'node:child_process';
import { mkdirSync, mkdtempSync, readFileSync, rmSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { afterEach, beforeEach, describe, it } from 'node:test';
import { fileURLToPath } from 'node:url';

import { completar, esPlaceholder, generarSecreto, leerClaves, prepararEntornos } from './setup-env.mjs';

const EJEMPLO_RAIZ = `POSTGRES_DB=app_db
POSTGRES_USER=app_admin
POSTGRES_PASSWORD=CAMBIAME
APP_RUNTIME_PASSWORD=CAMBIAME
REDIS_PASSWORD=CAMBIAME
MINIO_ROOT_USER=minio_admin
MINIO_ROOT_PASSWORD=CAMBIAME
INITIAL_ADMIN_EMAIL=admin@midominio.com
INITIAL_ADMIN_PASSWORD=CAMBIAME
# POSTGRES_HOST_PORT=5433
JWT_SECRET=CAMBIAME_MINIMO_32_CARACTERES_ALEATORIOS
`;

const EJEMPLO_BACKEND = `SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/app_db
SPRING_DATASOURCE_USERNAME=app_runtime
SPRING_DATASOURCE_PASSWORD=CAMBIAME
SPRING_FLYWAY_USER=app_admin
SPRING_FLYWAY_PASSWORD=CAMBIAME
APP_RUNTIME_PASSWORD=CAMBIAME
INITIAL_ADMIN_PASSWORD=CAMBIAME
SPRING_DATA_REDIS_PASSWORD=CAMBIAME
MINIO_ROOT_PASSWORD=CAMBIAME
JWT_SECRET=CAMBIAME_MINIMO_32_CARACTERES_ALEATORIOS
`;

const EJEMPLO_FRONT = 'VITE_API_BASE_URL=http://localhost:8080/api/v1\n';

let root;

function escribir(ruta, texto) {
  const completa = join(root, ruta);
  mkdirSync(join(completa, '..'), { recursive: true });
  writeFileSync(completa, texto);
}
const leer = (ruta) => readFileSync(join(root, ruta), 'utf8');

beforeEach(() => {
  root = mkdtempSync(join(tmpdir(), 'setup-env-'));
  escribir('.env.example', EJEMPLO_RAIZ);
  escribir('projects/backend/.env.example', EJEMPLO_BACKEND);
  escribir('projects/frontend/.env.example', EJEMPLO_FRONT);
  escribir('projects/mobile/.env.example', 'API_BASE_URL=http://10.0.2.2:8080/api/v1\n');
});
afterEach(() => rmSync(root, { recursive: true, force: true }));

describe('generarSecreto', () => {
  it('genera 32 alfanuméricos y valores distintos en cada llamada', () => {
    const a = generarSecreto('POSTGRES_PASSWORD');
    assert.match(a, /^[A-Za-z0-9]{32}$/);
    assert.notEqual(a, generarSecreto('POSTGRES_PASSWORD'));
  });

  it('JWT_SECRET tiene al menos 32 caracteres', () => {
    assert.ok(generarSecreto('JWT_SECRET').length >= 32);
  });
});

describe('esPlaceholder', () => {
  it('reconoce CAMBIAME y sus variantes, no valores reales', () => {
    assert.equal(esPlaceholder('CAMBIAME'), true);
    assert.equal(esPlaceholder('CAMBIAME_MINIMO_32_CARACTERES_ALEATORIOS'), true);
    assert.equal(esPlaceholder('abc123'), false);
    assert.equal(esPlaceholder(''), false);
  });
});

describe('completar', () => {
  it('conserva comentarios y claves comentadas', () => {
    const { texto } = completar(null, EJEMPLO_RAIZ, generarSecreto);
    assert.match(texto, /^# POSTGRES_HOST_PORT=5433$/m);
  });

  it('respeta el fin de línea CRLF del archivo existente', () => {
    const existente = 'A=1\r\nB=CAMBIAME\r\n';
    const { texto } = completar(existente, 'A=1\nB=CAMBIAME\nC=2\n', () => 'X');
    assert.equal(texto, 'A=1\r\nB=X\r\nC=2\r\n');
  });
});

describe('prepararEntornos', () => {
  it('desde cero crea los cuatro .env sin ningún CAMBIAME', () => {
    const reporte = prepararEntornos({ root });
    assert.deepEqual(reporte.map((r) => r.estado), ['creado', 'creado', 'creado', 'creado']);
    for (const ruta of ['.env', 'projects/backend/.env']) {
      assert.doesNotMatch(leer(ruta), /CAMBIAME/);
    }
  });

  it('el backend toma de la raíz las mismas contraseñas', () => {
    prepararEntornos({ root });
    const raiz = leerClaves(leer('.env'));
    const back = leerClaves(leer('projects/backend/.env'));
    assert.equal(back.get('SPRING_DATASOURCE_PASSWORD'), raiz.get('APP_RUNTIME_PASSWORD'));
    assert.equal(back.get('APP_RUNTIME_PASSWORD'), raiz.get('APP_RUNTIME_PASSWORD'));
    assert.equal(back.get('SPRING_FLYWAY_PASSWORD'), raiz.get('POSTGRES_PASSWORD'));
    assert.equal(back.get('SPRING_DATA_REDIS_PASSWORD'), raiz.get('REDIS_PASSWORD'));
    assert.equal(back.get('MINIO_ROOT_PASSWORD'), raiz.get('MINIO_ROOT_PASSWORD'));
    assert.equal(back.get('INITIAL_ADMIN_PASSWORD'), raiz.get('INITIAL_ADMIN_PASSWORD'));
    assert.equal(back.get('JWT_SECRET'), raiz.get('JWT_SECRET'));
  });

  it('APP_RUNTIME_PASSWORD es solo letras y números (lo exige la migración)', () => {
    prepararEntornos({ root });
    assert.match(leerClaves(leer('.env')).get('APP_RUNTIME_PASSWORD'), /^[A-Za-z0-9]+$/);
  });

  it('es idempotente: la segunda corrida no cambia ningún archivo', () => {
    prepararEntornos({ root });
    const antes = ['.env', 'projects/backend/.env'].map(leer);
    const reporte = prepararEntornos({ root });
    assert.deepEqual(reporte.map((r) => r.estado), ['sin-cambios', 'sin-cambios', 'sin-cambios', 'sin-cambios']);
    assert.deepEqual(['.env', 'projects/backend/.env'].map(leer), antes);
  });

  it('nunca pisa un valor existente y completa solo lo que falta', () => {
    escribir('.env', 'POSTGRES_PASSWORD=miClaveReal\nREDIS_PASSWORD=CAMBIAME\n');
    prepararEntornos({ root });
    const raiz = leerClaves(leer('.env'));
    assert.equal(raiz.get('POSTGRES_PASSWORD'), 'miClaveReal');
    assert.match(raiz.get('REDIS_PASSWORD'), /^[A-Za-z0-9]{32}$/);
    assert.ok(raiz.has('JWT_SECRET'), 'agrega las claves que faltaban');
    assert.equal(leerClaves(leer('projects/backend/.env')).get('SPRING_FLYWAY_PASSWORD'), 'miClaveReal');
  });

  it('el reporte nunca contiene los valores de los secretos', () => {
    const reporte = JSON.stringify(prepararEntornos({ root }));
    for (const v of leerClaves(leer('.env')).values()) {
      if (v.length >= 20) assert.ok(!reporte.includes(v));
    }
  });

  it('avisa si el correo del administrador sigue siendo el de ejemplo', () => {
    const [raiz] = prepararEntornos({ root });
    assert.ok(raiz.avisos.some((a) => a.includes('INITIAL_ADMIN_EMAIL')));
  });

  it('--check no escribe nada y marca lo que falta', () => {
    const reporte = prepararEntornos({ root, soloVerificar: true });
    assert.deepEqual(reporte.map((r) => r.estado), ['falta', 'falta', 'falta', 'falta']);
    assert.throws(() => leer('.env'));
  });

  it('--check da ok después de preparar', () => {
    prepararEntornos({ root });
    const reporte = prepararEntornos({ root, soloVerificar: true });
    assert.deepEqual(reporte.map((r) => r.estado), ['ok', 'ok', 'ok', 'ok']);
  });

  it('--check marca incompleto un .env con CAMBIAME', () => {
    escribir('.env', 'POSTGRES_PASSWORD=CAMBIAME\n');
    const [raiz] = prepararEntornos({ root, soloVerificar: true });
    assert.equal(raiz.estado, 'pendiente');
  });
});

describe('archivos sin cambios de fondo', () => {
  it('no reescribe un .env completo aunque le falte el salto de línea final', () => {
    prepararEntornos({ root });
    const ruta = join(root, 'projects/frontend/.env');
    const sinSalto = readFileSync(ruta, 'utf8').replace(/\n+$/, '');
    writeFileSync(ruta, sinSalto);
    const reporte = prepararEntornos({ root });
    assert.equal(reporte.find((r) => r.archivo === 'projects/frontend/.env').estado, 'sin-cambios');
    assert.equal(readFileSync(ruta, 'utf8'), sinSalto);
  });
});

describe('valores que no coinciden entre la raíz y el backend', () => {
  const cambiar = (ruta, clave, valor) =>
    escribir(ruta, leer(ruta).replace(new RegExp(`^${clave}=.*$`, 'm'), () => `${clave}=${valor}`));
  const verificar = () => {
    const reporte = prepararEntornos({ root, soloVerificar: true });
    return { reporte, backend: reporte.find((r) => r.archivo === 'projects/backend/.env') };
  };
  beforeEach(() => prepararEntornos({ root }));

  it('recién preparados no hay diferencias ni avisos de conexión', () => {
    const { backend } = verificar();
    assert.equal(backend.estado, 'ok');
    assert.deepEqual(backend.distintas, []);
    assert.deepEqual(backend.avisos.filter((a) => a.includes('SPRING_DATASOURCE_URL')), []);
  });

  for (const [claveBackend, claveRaiz] of [
    ['SPRING_DATASOURCE_PASSWORD', 'APP_RUNTIME_PASSWORD'],
    ['SPRING_FLYWAY_PASSWORD', 'POSTGRES_PASSWORD'],
    ['SPRING_DATA_REDIS_PASSWORD', 'REDIS_PASSWORD'],
    ['SPRING_FLYWAY_USER', 'POSTGRES_USER'],
    ['MINIO_ROOT_PASSWORD', 'MINIO_ROOT_PASSWORD'],
    ['JWT_SECRET', 'JWT_SECRET'],
  ]) {
    it(`marca ${claveBackend} distinta de ${claveRaiz}, sin mostrar valores`, () => {
      cambiar('projects/backend/.env', claveBackend, 'valorQueNoCoincide123');
      const { reporte, backend } = verificar();
      assert.equal(backend.estado, 'distinto');
      assert.deepEqual(backend.distintas, [`${claveBackend} (raíz: ${claveRaiz})`]);
      assert.doesNotMatch(JSON.stringify(reporte), /valorQueNoCoincide123/);
    });
  }

  it('no compara claves que pueden diferir: CORS de la raíz es el de producción', () => {
    escribir('.env', `${leer('.env')}CORS_ALLOWED_ORIGINS=https://app.midominio.com\n`);
    escribir('projects/backend/.env', `${leer('projects/backend/.env')}CORS_ALLOWED_ORIGINS=http://localhost:5173\n`);
    const { backend } = verificar();
    assert.equal(backend.estado, 'ok');
    assert.deepEqual(backend.distintas, []);
  });

  it('avisa, sin fallar, si el puerto local de la URL no es el que publica Docker', () => {
    escribir('.env', `${leer('.env')}POSTGRES_HOST_PORT=5433\n`);
    const { backend } = verificar();
    assert.equal(backend.estado, 'ok');
    assert.ok(backend.avisos.some((a) => a.includes('SPRING_DATASOURCE_URL') && a.includes('puerto')));
  });

  it('avisa si la base de la URL local no es POSTGRES_DB', () => {
    cambiar('projects/backend/.env', 'SPRING_DATASOURCE_URL', 'jdbc:postgresql://127.0.0.1:5432/otra_base');
    assert.ok(verificar().backend.avisos.some((a) => a.includes('base')));
  });

  it('no opina sobre una URL que apunta a otro servidor', () => {
    cambiar('projects/backend/.env', 'SPRING_DATASOURCE_URL', 'jdbc:postgresql://db.interna:6000/otra_base');
    assert.deepEqual(verificar().backend.avisos.filter((a) => a.includes('SPRING_DATASOURCE_URL')), []);
  });

  it('al preparar avisa de la diferencia y no pisa el valor', () => {
    cambiar('projects/backend/.env', 'SPRING_FLYWAY_PASSWORD', 'valorPropio123');
    const backend = prepararEntornos({ root }).find((r) => r.archivo === 'projects/backend/.env');
    assert.deepEqual(backend.distintas, ['SPRING_FLYWAY_PASSWORD (raíz: POSTGRES_PASSWORD)']);
    assert.match(leer('projects/backend/.env'), /^SPRING_FLYWAY_PASSWORD=valorPropio123$/m);
  });

  it('el comando --check sale con error y no imprime los valores', () => {
    cambiar('projects/backend/.env', 'SPRING_DATA_REDIS_PASSWORD', 'valorQueNoCoincide123');
    const script = fileURLToPath(new URL('./setup-env.mjs', import.meta.url));
    const r = spawnSync(process.execPath, [script, '--check', '--root', root], { encoding: 'utf8' });
    assert.equal(r.status, 1);
    assert.match(r.stdout, /SPRING_DATA_REDIS_PASSWORD \(raíz: REDIS_PASSWORD\)/);
    assert.doesNotMatch(r.stdout + r.stderr, /valorQueNoCoincide123/);
  });
});
