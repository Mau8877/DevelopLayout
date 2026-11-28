import { strict as assert } from 'node:assert';
import { execFileSync } from 'node:child_process';
import { existsSync, mkdirSync, mkdtempSync, readFileSync, rmSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { dirname, join, resolve } from 'node:path';
import { afterEach, beforeEach, describe, it } from 'node:test';
import { fileURLToPath } from 'node:url';

import {
  aplicar, crearReglas, crearValores, exigirPlantillaLimpia, leerArgumentos, nuevaRuta, planificar, restos,
  transformar,
} from './init-project.mjs';

const DATOS = { nombre: 'Acme Tienda', slug: 'acme-tienda', paquete: 'org.acme.web' };
const valores = crearValores(DATOS);
const cambiar = (ruta, texto) => transformar(ruta, texto, crearReglas(valores));

describe('datos del proyecto', () => {
  it('deriva los demás nombres', () => {
    assert.deepEqual(
      { snake: valores.snake, clase: valores.clase, dart: valores.dart, ruta: valores.rutaPaquete },
      { snake: 'acme_tienda', clase: 'AcmeTienda', dart: 'acme_tienda_mobile', ruta: 'org/acme/web' },
    );
  });

  for (const [campo, malos] of Object.entries({
    nombre: ['', '   ', "O'Brien", 'a"b', '<b>', 'precio $5', 'x'.repeat(41)],
    slug: ['', 'app', 'Acme', 'acme_tienda', '1acme', 'acme-', 'a'.repeat(31), 'a; rm -rf /'],
    paquete: ['', 'acme', 'com.example', 'com.example.tienda', 'com.mi-empresa', 'com.Acme', 'com.new', 'com..a', 'com.1a'],
  })) {
    it(`rechaza un ${campo} que rompería el proyecto`, () => {
      for (const malo of malos) {
        assert.throws(() => crearValores({ ...DATOS, [campo]: malo }), new RegExp(`--${campo}`), `aceptó «${malo}»`);
      }
    });
  }

  it('acepta acentos en el nombre visible', () => {
    assert.equal(crearValores({ ...DATOS, nombre: ' Gestión Año 2.0 ' }).nombre, 'Gestión Año 2.0');
  });

  it('lee los argumentos en sus dos formas y rechaza los desconocidos', () => {
    assert.deepEqual(leerArgumentos(['--', '--nombre', 'A B', '--slug=ab', '--paquete', 'com.ab', '--dry-run']), {
      dryRun: true, nombre: 'A B', slug: 'ab', paquete: 'com.ab',
    });
    assert.throws(() => leerArgumentos(['--force']), /desconocido/);
  });
});

describe('reemplazos', () => {
  it('cambia los recursos de Docker y la base, también los de producción', () => {
    const antes = 'app-backend app-prod-postgres_data app-network app-frontend-node:ci app_db app_admin';
    assert.equal(
      cambiar('docker-compose.yml', antes),
      'acme-tienda-backend acme-tienda-prod-postgres_data acme-tienda-network acme-tienda-frontend-node:ci '
        + 'acme_tienda_db acme_tienda_admin',
    );
  });

  it('no toca lo que se parece pero no es un nombre de la plantilla', () => {
    for (const texto of [
      'com.apple.security.app-sandbox', 'app_runtime APP_RUNTIME_PASSWORD', 'app.minio.bucket-name', 'myapp-backend',
      'app_router.dart AppButton app_button', 'com.examples.otra', 'admin@example.com',
    ]) {
      assert.equal(cambiar('projects/mobile/lib/core/x.dart', texto), texto);
    }
  });

  it('cambia el paquete en Java, en sus rutas y en mobile', () => {
    assert.equal(
      cambiar('projects/backend/src/main/java/com/example/backend/A.java', 'package com.example.backend.a;'),
      'package org.acme.web.backend.a;',
    );
    assert.equal(
      nuevaRuta('projects/backend/src/test/java/com/example/backend/AppBackendApplicationTests.java', valores),
      'projects/backend/src/test/java/org/acme/web/backend/AcmeTiendaBackendApplicationTests.java',
    );
    assert.equal(cambiar('projects/mobile/test/a_test.dart', "import 'package:mobile/main.dart';"),
      "import 'package:acme_tienda_mobile/main.dart';");
    assert.equal(cambiar('projects/mobile/pubspec.yaml', 'name: mobile\r\n'), 'name: acme_tienda_mobile\r\n');
  });

  it('cambia los títulos visibles solo donde corresponde', () => {
    assert.equal(cambiar('projects/frontend/src/layouts/admin/AdminLayout.tsx', 'appName="App Admin"'),
      'appName="Acme Tienda Admin"');
    assert.equal(cambiar('docs/otra.md', "'App'"), "'App'");
  });

  it('el nombre de la red de compose cambia sin tocar otros `name:`', () => {
    assert.equal(cambiar('docker-compose.yml', 'name: app\nservices:\n'), 'name: acme-tienda\nservices:\n');
  });

  it('detecta lo que quedara de la plantilla', () => {
    assert.deepEqual(restos('com.example app-redis app_db package:mobile/'), [
      'com.example', 'app-redis', 'app_db', 'package:mobile/',
    ]);
    assert.deepEqual(restos('app_runtime app-sandbox'), []);
  });

  it('señala un nombre armado al ejecutar, que no podría renombrar', () => {
    assert.deepEqual(restos('docker(`app-${nombre}`)'), ['app-${']);
  });
});

describe('sobre un repositorio', () => {
  let raiz;
  const escribir = (ruta, contenido) => {
    mkdirSync(dirname(join(raiz, ruta)), { recursive: true });
    writeFileSync(join(raiz, ruta), contenido);
  };
  const git = (...args) => execFileSync('git', args, { cwd: raiz, stdio: 'pipe' });
  const leer = (ruta) => readFileSync(join(raiz, ruta), 'utf8');
  const BINARIO = Buffer.from([0, 1, 2, ...Buffer.from('app-backend')]);

  beforeEach(() => {
    raiz = mkdtempSync(join(tmpdir(), 'init-'));
    escribir('projects/backend/pom.xml', '<groupId>com.example</groupId>\r\n<artifactId>app-backend</artifactId>\r\n');
    escribir('projects/backend/src/main/java/com/example/backend/common/util/.gitkeep', '');
    escribir('projects/backend/src/main/java/com/example/backend/AppBackendApplication.java',
      'package com.example.backend;\nclass AppBackendApplication {}\n');
    escribir('icono.png', BINARIO);
    escribir('docs/UnidadesTrabajo/vieja.md', 'com.example.backend');
    escribir('.env', 'POSTGRES_DB=app_db');
    git('init', '-q');
    git('config', 'user.email', 'prueba@test.local');
    git('config', 'user.name', 'Prueba');
    git('config', 'core.autocrlf', 'false');
    git('add', '-A', '-f');
    git('commit', '-q', '-m', 'inicial', '--no-verify');
  });

  afterEach(() => rmSync(raiz, { recursive: true, force: true }));

  it('edita y mueve, incluso las carpetas vacías, y conserva los saltos de línea', () => {
    aplicar(raiz, planificar(raiz, valores));

    assert.equal(leer('projects/backend/pom.xml'),
      '<groupId>org.acme.web</groupId>\r\n<artifactId>acme-tienda-backend</artifactId>\r\n');
    assert.equal(leer('projects/backend/src/main/java/org/acme/web/backend/AcmeTiendaBackendApplication.java'),
      'package org.acme.web.backend;\nclass AcmeTiendaBackendApplication {}\n');
    assert.ok(existsSync(join(raiz, 'projects/backend/src/main/java/org/acme/web/backend/common/util/.gitkeep')));
    assert.ok(!existsSync(join(raiz, 'projects/backend/src/main/java/com')));
  });

  it('no toca binarios, el historial ni los .env', () => {
    aplicar(raiz, planificar(raiz, valores));

    assert.deepEqual(readFileSync(join(raiz, 'icono.png')), BINARIO);
    assert.equal(leer('docs/UnidadesTrabajo/vieja.md'), 'com.example.backend');
    assert.equal(leer('.env'), 'POSTGRES_DB=app_db');
  });

  it('planificar no escribe nada', () => {
    planificar(raiz, valores);

    assert.equal(git('status', '--porcelain').toString(), '');
  });

  it('se niega con cambios sin commitear y se niega a correr dos veces', () => {
    escribir('nuevo.txt', 'x');
    assert.throws(() => exigirPlantillaLimpia(raiz), /sin commitear/);
    rmSync(join(raiz, 'nuevo.txt'));
    assert.doesNotThrow(() => exigirPlantillaLimpia(raiz));

    aplicar(raiz, planificar(raiz, valores));
    assert.throws(() => exigirPlantillaLimpia(raiz), /una sola vez/);
  });
});

const REPO = resolve(dirname(fileURLToPath(import.meta.url)), '..');
const esPlantilla = readFileSync(join(REPO, 'projects/backend/pom.xml'), 'utf8').includes('<groupId>com.example</groupId>');

// * Solo en la plantilla: si alguien agrega un nombre que el script no sabe cambiar,
// * o quita algo que una regla esperaba, falla acá y no en el proyecto de otra persona.
describe('la plantilla real', { skip: !esPlantilla && 'el proyecto ya está inicializado' }, () => {
  it('no deja nombres de la plantilla y todas las reglas encuentran algo', () => {
    const plan = planificar(REPO, valores);

    assert.deepEqual(plan.pendientes, []);
    assert.deepEqual(plan.sinUso, []);
  });
});
