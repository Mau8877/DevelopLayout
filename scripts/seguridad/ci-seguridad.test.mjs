// Cadena de suministro del CI (ver .claude/rules/CI_CD.md). Solo lee texto.
import { describe, it } from 'node:test';
import assert from 'node:assert/strict';
import { readdirSync, readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { dirname, join } from 'node:path';

const workflows = join(dirname(fileURLToPath(import.meta.url)), '..', '..', '.github', 'workflows');

// SHA completo y, como comentario, la versión (Dependabot actualiza los dos).
const FIJADA = /^[\w.-]+\/[\w./-]+@[0-9a-f]{40} # v\d+\.\d+\.\d+$/;

/** Referencias `uses:` de acciones externas (las locales `./` viven en el repo). */
export function accionesSinFijar(texto) {
  return texto
    .split(/\r?\n/)
    .map((linea) => /^\s*(?:-\s+)?uses:\s*(.+?)\s*$/.exec(linea)?.[1])
    .filter((ref) => ref && !ref.startsWith('./') && !FIJADA.test(ref));
}

describe('acciones de GitHub fijadas por SHA', () => {
  it('reconoce lo que no está fijado', () => {
    const sha = 'a'.repeat(40);
    for (const mala of ['actions/checkout@v4', `actions/checkout@${sha}`, 'actions/checkout@abc1234 # v4.0.0', `x/y@main # v1.0.0`]) {
      assert.deepEqual(accionesSinFijar(`      - uses: ${mala}`), [mala]);
    }
    assert.deepEqual(accionesSinFijar(`        uses: actions/checkout@${sha} # v7.0.1`), []);
    assert.deepEqual(accionesSinFijar('      - uses: ./.github/actions/local'), []);
  });

  for (const archivo of readdirSync(workflows).filter((n) => /\.ya?ml$/.test(n))) {
    it(archivo, () => {
      assert.deepEqual(accionesSinFijar(readFileSync(join(workflows, archivo), 'utf8')), []);
    });
  }
});
