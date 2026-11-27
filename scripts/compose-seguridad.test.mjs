// Reglas de endurecimiento de los compose (ver .claude/rules/SEGURIDAD_OWASP.md).
// Solo lee texto: no necesita Docker ni dependencias.
import { describe, it } from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { dirname, join } from 'node:path';

const raiz = join(dirname(fileURLToPath(import.meta.url)), '..');

/** Bloque de un servicio: desde `  nombre:` hasta el siguiente servicio o sección. */
function servicio(archivo, nombre) {
  const lineas = readFileSync(join(raiz, archivo), 'utf8').split(/\r?\n/);
  const inicio = lineas.indexOf(`  ${nombre}:`);
  assert.notEqual(inicio, -1, `no existe el servicio ${nombre} en ${archivo}`);
  const resto = lineas.slice(inicio + 1);
  const fin = resto.findIndex((l) => /^( {0,2})[\w-]+:/.test(l));
  return resto
    .slice(0, fin === -1 ? resto.length : fin)
    .filter((l) => !l.trim().startsWith('#'))
    .join('\n');
}

const SIN_ROOT = 'user: "${MINIO_UID:-65532}:${MINIO_UID:-65532}"';

for (const archivo of ['docker-compose.yml', 'docker-compose.prod.yml']) {
  describe(`MinIO en ${archivo}`, () => {
    it('no corre como root', () => {
      const minio = servicio(archivo, 'minio');
      assert.ok(minio.includes(SIN_ROOT), 'minio debe fijar un usuario sin privilegios');
      assert.doesNotMatch(minio, /user:\s*['"]?(0|root)\b/);
    });

    it('espera a que el volumen tenga el dueño correcto', () => {
      assert.match(servicio(archivo, 'minio'), /minio-permisos:\s*\n\s*condition: service_completed_successfully/);
    });

    it('el único paso como root no tiene red ni más permisos que los de cambiar el dueño', () => {
      const permisos = servicio(archivo, 'minio-permisos');
      assert.match(permisos, /network_mode: none/);
      assert.match(permisos, /cap_drop:\s*\n\s*- ALL/);
      assert.match(permisos, /cap_add:\s*\n\s*- CHOWN\s*\n\s*- DAC_OVERRIDE\s*(\n|$)/);
      assert.match(permisos, /restart: ['"]no['"]/);
    });
  });
}

describe('endurecimiento de MinIO', () => {
  it('quita todas las capabilities y prohíbe escalar privilegios', () => {
    assert.match(servicio('docker-compose.prod.yml', 'minio'), /<<: \*hardening/);
    const desarrollo = servicio('docker-compose.yml', 'minio');
    assert.match(desarrollo, /cap_drop:\s*\n\s*- ALL/);
    assert.match(desarrollo, /no-new-privileges:true/);
  });
});

describe('imágenes de nginx en producción', () => {
  const minimo = (ruta) => /'nginx>=([\d.]+)'/.exec(readFileSync(join(raiz, ruta), 'utf8'))?.[1];

  it('el proxy se construye con la imagen parcheada, no usa la pública tal cual', () => {
    const proxy = servicio('docker-compose.prod.yml', 'proxy');
    assert.match(proxy, /context: infrastructure\/nginx/);
    assert.doesNotMatch(proxy, /image: nginx/);
  });

  it('el proxy y la web exigen el mismo mínimo de nginx', () => {
    const web = minimo('projects/frontend/Dockerfile');
    assert.ok(web, 'la imagen web debe exigir un mínimo de nginx');
    assert.equal(minimo('infrastructure/nginx/Dockerfile'), web);
  });
});

describe('minio-init (cliente que crea el bucket)', () => {
  const imagen = (archivo) => /^ {4}image: (\S+)$/m.exec(servicio(archivo, 'minio-init'))?.[1];

  for (const archivo of ['docker-compose.yml', 'docker-compose.prod.yml']) {
    it(`${archivo}: imagen mantenida y fijada por digest`, () => {
      assert.match(imagen(archivo), /^cgr\.dev\/chainguard\/minio-client:[\w.-]+@sha256:[0-9a-f]{64}$/);
    });

    it(`${archivo}: sin capabilities ni escalada de privilegios, y no se reinicia`, () => {
      const init = servicio(archivo, 'minio-init');
      assert.match(init, /cap_drop:\s*\n\s*- ALL/);
      assert.match(init, /no-new-privileges:true/);
      assert.doesNotMatch(init, /unless-stopped|<<: \*hardening|user:\s*['"]?(0|root)\b/);
    });
  }

  it('desarrollo y producción usan la misma imagen', () => {
    assert.equal(imagen('docker-compose.yml'), imagen('docker-compose.prod.yml'));
  });
});
