// Una ruta rota en `paths-filter` no da error: el job simplemente deja de correr y
// el CI sigue en verde. Por eso se comprueba que cada script nombrado exista.
import { test } from 'node:test';
import assert from 'node:assert/strict';
import { existsSync, readFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const raiz = join(dirname(fileURLToPath(import.meta.url)), '..', '..');

/** Rutas concretas bajo `scripts/`; los patrones con `*` no entran. */
function rutasNombradas(texto) {
  return [...texto.matchAll(/scripts\/[A-Za-z0-9_./*-]+\.(?:mjs|sh|ps1)/g)]
    .map(([ruta]) => ruta)
    .filter((ruta) => !ruta.includes('*'));
}

test('detecta una ruta concreta y deja pasar los patrones', () => {
  const texto = "- 'scripts/**'\n- 'scripts/no/existe.mjs'\nrun: node --test \"scripts/**/*.test.mjs\"";

  assert.deepEqual(rutasNombradas(texto), ['scripts/no/existe.mjs']);
});

for (const archivo of ['.github/workflows/ci.yml', 'package.json']) {
  test(`${archivo} solo nombra scripts que existen`, () => {
    const rutas = rutasNombradas(readFileSync(join(raiz, archivo), 'utf8'));

    assert.ok(rutas.length > 0, 'no se encontró ninguna ruta: la prueba ya no comprueba nada');
    assert.deepEqual(rutas.filter((ruta) => !existsSync(join(raiz, ruta))), []);
  });
}
