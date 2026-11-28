// Reglas de endurecimiento de los compose (ver .claude/rules/SEGURIDAD_OWASP.md).
// Solo lee texto: no necesita Docker ni dependencias.
import { describe, it } from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { dirname, join } from 'node:path';

const raiz = join(dirname(fileURLToPath(import.meta.url)), '..', '..');

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

describe('Mailpit (bandeja de correo de desarrollo)', () => {
  const mailpit = () => servicio('docker-compose.yml', 'mailpit');

  it('tag fijo, sin root, sin capabilities ni escalada', () => {
    assert.match(mailpit(), /^ {4}image: axllent\/mailpit:v\d+\.\d+\.\d+$/m);
    assert.match(mailpit(), /user: ['"]65534:65534['"]/);
    assert.match(mailpit(), /cap_drop:\s*\n\s*- ALL/);
    assert.match(mailpit(), /no-new-privileges:true/);
  });

  it('SMTP y bandeja publicados solo en 127.0.0.1', () => {
    const puertos = [...mailpit().matchAll(/^ {6}- '([^']+)'$/gm)].map((m) => m[1]).filter((p) => /:\d+$/.test(p));
    assert.deepEqual(puertos, ['127.0.0.1:1025:1025', '127.0.0.1:8025:8025']);
  });

  it('no existe en producción', () => {
    assert.doesNotMatch(readFileSync(join(raiz, 'docker-compose.prod.yml'), 'utf8'), /mailpit/i);
  });
});

describe('tamaño máximo del body en el proxy (API4)', () => {
  for (const archivo of ['infrastructure/nginx/nginx.conf', 'infrastructure/nginx/nginx.prod.conf']) {
    it(`${archivo} limita a 1 MB y ningún bloque lo amplía`, () => {
      const limites = [...readFileSync(join(raiz, archivo), 'utf8').matchAll(/^\s*client_max_body_size\s+(\S+);/gm)];

      assert.deepEqual(limites.map((m) => m[1]), ['1m']);
    });
  }
});

describe('ajustes de seguridad que llegan al backend', () => {
  // Compose solo pasa lo que declara: sin la línea, cambiar el .env no hace nada.
  const leidas = readFileSync(join(raiz, 'projects/backend/src/main/resources/application.properties'), 'utf8');
  for (const variable of ['RATE_LIMIT_CAPACITY', 'RATE_LIMIT_REFILL_PER_MINUTE', 'LOGIN_MAX_ATTEMPTS_PER_ACCOUNT',
    'BITACORA_RETENTION_MONTHS']) {
    it(`${variable}: los dos compose la pasan con el mismo valor por defecto que la aplicación`, () => {
      const porDefecto = new RegExp(`\\$\\{${variable}:(\\d+)\\}`).exec(leidas)[1];
      for (const archivo of ['docker-compose.yml', 'docker-compose.prod.yml']) {
        assert.ok(readFileSync(join(raiz, archivo), 'utf8').includes(`\${${variable}:-${porDefecto}}`), archivo);
      }
    });
  }
});
