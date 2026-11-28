// Interrumpe `e2e.mjs` DE VERDAD, con señales, y comprueba que no deja nada vivo.
//
// * Corre el script real dentro de un proyecto falso: Docker, Maven, Java, Vite y
// * Playwright son sustitutos que anotan sus procesos. No necesita Docker.
// * Windows no entrega estas señales: ahí queda omitido. Lo corre el job `scripts` del CI.
import assert from 'node:assert/strict';
import { spawn } from 'node:child_process';
import {
  chmodSync, copyFileSync, existsSync, mkdirSync, mkdtempSync, readFileSync, readdirSync, rmSync, writeFileSync,
} from 'node:fs';
import { tmpdir } from 'node:os';
import { dirname, join } from 'node:path';
import { afterEach, describe, it } from 'node:test';
import { fileURLToPath } from 'node:url';

const SCRIPTS = join(dirname(fileURLToPath(import.meta.url)), '..');

const COMUN = `
const fs = require('node:fs');
const FAKE = process.env.FAKE_DIR;
const anotar = (nombre, pid) => fs.appendFileSync(FAKE + '/procesos', nombre + ' ' + pid + '\\n');
const quedarse = () => setInterval(() => {}, 1000);
const nieto = (nombre) => {
  const hijo = require('node:child_process').spawn(process.execPath, ['-e', 'setInterval(() => {}, 1000)'], { stdio: 'ignore' });
  anotar(nombre, hijo.pid);
  return hijo;
};
const servir = (puerto) => require('node:http').createServer((_, r) => r.end('{"messages":[]}')).listen(puerto, '127.0.0.1');
`;

const DOCKER = `#!/usr/bin/env node
${COMUN}
const estado = FAKE + '/contenedores';
const leer = () => fs.existsSync(estado) ? JSON.parse(fs.readFileSync(estado, 'utf8')) : [];
const guardar = (lista) => fs.writeFileSync(estado, JSON.stringify(lista));
const dormir = (ms) => Atomics.wait(new Int32Array(new SharedArrayBuffer(4)), 0, 0, ms);
const [orden, ...resto] = process.argv.slice(2);
if (orden === 'ps') {
  const filtro = resto.indexOf('--filter');
  const etiqueta = filtro === -1 ? null : resto[filtro + 1].replace(/^label=/, '');
  console.log(leer().filter((c) => !etiqueta || c.etiquetas.includes(etiqueta))
    .map((c) => (resto.some((a) => a.startsWith('-aq') || a === '-q') ? c.id : c.nombre)).join('\\n'));
} else if (orden === 'run') {
  const nombre = resto[resto.indexOf('--name') + 1];
  const contenedor = { id: 'id-' + nombre, nombre, etiquetas: resto.filter((_, i) => resto[i - 1] === '--label') };
  if (nombre.endsWith('mailpit')) {
    const buzon = require('node:child_process').spawn(process.execPath, ['-e',
      "require('node:http').createServer((_, r) => r.end('{}')).listen(28025, '127.0.0.1')"],
      { stdio: 'ignore', detached: true });
    buzon.unref();
    contenedor.pid = buzon.pid;
    anotar('buzon', buzon.pid);
  }
  guardar([...leer(), contenedor]);
  // * Ya existe, pero la orden todavía no respondió.
  if (process.env.FAKE_FASE === 'creando' && nombre.endsWith('redis')) { anotar('creando', process.pid); dormir(30000); }
  console.log(contenedor.id);
} else if (orden === 'rm') {
  const id = resto.at(-1);
  if (process.env.FAKE_BORRADO_LENTO) { anotar('borrando', process.pid); dormir(700); }
  const contenedor = leer().find((c) => c.id === id);
  if (contenedor?.pid) try { process.kill(contenedor.pid, 'SIGKILL'); } catch {}
  guardar(leer().filter((c) => c.id !== id));
} else if (orden === 'exec') {
  if (process.env.FAKE_FASE === 'esperando') process.exit(1);
  console.log('accepting connections');
}
`;

const MAVEN = `${COMUN}
anotar('maven', process.pid);
const hijo = nieto('compilador');
if (process.env.FAKE_FASE === 'compilando') quedarse();
else { fs.mkdirSync('target', { recursive: true }); fs.writeFileSync('target/app.jar', ''); hijo.kill('SIGKILL'); process.exit(0); }
`;

const JAVA = `${COMUN}
anotar('backend', process.pid);
servir(28080);
`;

const VITE = `${COMUN}
anotar('web', process.pid);
servir(25173);
`;

const PLAYWRIGHT = `${COMUN}
nieto('navegador');
anotar('playwright', process.pid);
quedarse();
`;

function proyectoFalso() {
  const base = mkdtempSync(join(tmpdir(), 'interrupcion-'));
  const escribir = (ruta, texto, ejecutable = false) => {
    mkdirSync(dirname(join(base, ruta)), { recursive: true });
    writeFileSync(join(base, ruta), texto);
    if (ejecutable) chmodSync(join(base, ruta), 0o755);
  };
  for (const archivo of ['e2e/e2e.mjs', 'proyecto/setup-env.mjs']) {
    mkdirSync(dirname(join(base, 'repo/scripts', archivo)), { recursive: true });
    copyFileSync(join(SCRIPTS, archivo), join(base, 'repo/scripts', archivo));
  }
  escribir('bin/docker', DOCKER, true);
  escribir('fake/mvn.cjs', MAVEN);
  escribir('fake/java.cjs', JAVA);
  escribir('repo/projects/backend/mvnw', '#!/bin/sh\nexec node "$FAKE_DIR/mvn.cjs"\n', true);
  escribir('jdk/bin/java', '#!/bin/sh\nexec node "$FAKE_DIR/java.cjs"\n', true);
  escribir('repo/projects/frontend/node_modules/vite/bin/vite.js', VITE);
  escribir('repo/projects/frontend/node_modules/@playwright/test/cli.js', PLAYWRIGHT);
  mkdirSync(join(base, 'temporales'));
  return base;
}

const esperar = async (descripcion, condicion) => {
  const limite = Date.now() + 30_000;
  while (!condicion()) {
    if (Date.now() > limite) throw new Error(`no ocurrió: ${descripcion}`);
    await new Promise((ok) => setTimeout(ok, 50));
  }
};

/** Un proceso terminado que nadie recogió todavía cuenta como muerto. */
function vivo(pid) {
  try {
    return !/^State:\s+Z/m.test(readFileSync(`/proc/${pid}/status`, 'utf8'));
  } catch {
    return false;
  }
}

describe('interrumpir las pruebas de navegador', { skip: process.platform !== 'linux' && 'necesita señales de Linux' }, () => {
  let base;
  let proceso;

  function arrancar(fase, extra = {}) {
    base = proyectoFalso();
    let salida = '';
    // * `detached`: grupo propio, para poder señalar al grupo entero como hace Ctrl+C.
    proceso = spawn(process.execPath, [join(base, 'repo/scripts/e2e/e2e.mjs')], {
      detached: true,
      env: {
        PATH: `${join(base, 'bin')}:${process.env.PATH}`,
        JAVA_HOME: join(base, 'jdk'),
        FAKE_DIR: join(base, 'fake'),
        FAKE_FASE: fase,
        TMPDIR: join(base, 'temporales'),
        ...extra,
      },
    });
    proceso.stdout.on('data', (d) => (salida += d));
    proceso.stderr.on('data', (d) => (salida += d));
    const fin = new Promise((ok) => proceso.once('exit', (codigo, senal) => ok({ codigo, senal })));
    return { fin, salida: () => salida };
  }

  const anotados = () => {
    const ruta = join(base, 'fake/procesos');
    return existsSync(ruta) ? readFileSync(ruta, 'utf8').trim().split('\n').map((l) => l.split(' ')) : [];
  };
  const hay = (nombre) => anotados().some(([n]) => n === nombre);
  const contenedores = () => {
    const ruta = join(base, 'fake/contenedores');
    return existsSync(ruta) ? JSON.parse(readFileSync(ruta, 'utf8')).map((c) => c.nombre) : [];
  };

  async function comprobarQueNoQuedaNada(fin, salida) {
    await fin;
    await esperar(`que mueran todos los procesos\n${salida()}`, () => anotados().every(([, pid]) => !vivo(pid)));
    assert.deepEqual(contenedores(), [], salida());
    assert.deepEqual(readdirSync(join(base, 'temporales')), [], 'quedó la carpeta temporal');
  }

  afterEach(() => {
    for (const [, pid] of base ? anotados() : []) {
      try {
        process.kill(Number(pid), 'SIGKILL');
      } catch {
        // Ya había terminado.
      }
    }
    try {
      process.kill(-proceso.pid, 'SIGKILL');
    } catch {
      // Ya había terminado.
    }
    rmSync(base, { recursive: true, force: true });
  });

  const ctrlC = () => process.kill(-proceso.pid, 'SIGINT');

  for (const [momento, fase, listo] of [
    ['esperando a la base', 'esperando', () => contenedores().length === 3],
    ['compilando el backend', 'compilando', () => hay('compilador')],
    ['corriendo las pruebas', 'pruebas', () => hay('navegador') && hay('backend') && hay('web')],
  ]) {
    it(`con Ctrl+C ${momento}, no queda nada`, async () => {
      const { fin, salida } = arrancar(fase);
      await esperar(`llegar a «${momento}»\n${salida()}`, listo);

      ctrlC();

      await comprobarQueNoQuedaNada(fin, salida);
      assert.notEqual((await fin).codigo, 0);
    });

    it(`si el CI la cancela (SIGTERM) ${momento}, no queda nada`, async () => {
      const { fin, salida } = arrancar(fase);
      await esperar(`llegar a «${momento}»\n${salida()}`, listo);

      proceso.kill('SIGTERM');

      await comprobarQueNoQuedaNada(fin, salida);
      assert.notEqual((await fin).codigo, 0);
    });
  }

  it('con Ctrl+C mientras Docker crea un contenedor, también lo borra', async () => {
    const { fin, salida } = arrancar('creando');
    await esperar(`crear el contenedor\n${salida()}`, () => hay('creando'));

    ctrlC();

    await comprobarQueNoQuedaNada(fin, salida);
  });

  it('un segundo Ctrl+C durante la limpieza no la deja a medias', async () => {
    const { fin, salida } = arrancar('pruebas', { FAKE_BORRADO_LENTO: '1' });
    await esperar(`llegar a las pruebas\n${salida()}`, () => hay('navegador') && hay('backend') && hay('web'));

    ctrlC();
    await esperar(`empezar a borrar\n${salida()}`, () => hay('borrando'));
    ctrlC();

    await comprobarQueNoQuedaNada(fin, salida);
  });
});
