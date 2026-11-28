// Toda historia propia del proyecto termina con una UT de seguridad (UNIDADES_DE_TRABAJO.md).
// Las `HU-INICIAL-*` de la plantilla son anteriores: las cubre docs/seguridad/AUDITORIA_OWASP.md.
import { describe, it } from 'node:test';
import assert from 'node:assert/strict';
import { existsSync, mkdirSync, mkdtempSync, readdirSync, rmSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const raiz = join(dirname(fileURLToPath(import.meta.url)), '..', '..');

/** Carpetas `HU{NN} - Nombre` a las que les falta la UT de seguridad o su índice. */
function sinUtDeSeguridad(base) {
  if (!existsSync(base)) return [];
  return readdirSync(base, { withFileTypes: true })
    .filter((e) => e.isDirectory() && /^HU\d{2} - /.test(e.name))
    .filter((e) => {
      const numero = e.name.slice(2, 4);
      const carpeta = join(base, e.name, 'UT-SEGURIDAD');
      return !(
        existsSync(join(carpeta, 'README.md')) &&
        readdirSync(carpeta).some((a) => new RegExp(`^UT-HU${numero}-SEG-\\d{2}\\.md$`).test(a))
      );
    })
    .map((e) => e.name);
}

describe('UT de seguridad en cada historia', () => {
  it('detecta la que falta y acepta la completa; las iniciales no entran', () => {
    const base = mkdtempSync(join(tmpdir(), 'ut-seg-'));
    try {
      for (const carpeta of ['HU01 - Completa/UT-SEGURIDAD', 'HU02 - Sin seguridad/UT-BACKEND',
        'HU03 - Con otro numero/UT-SEGURIDAD', 'HU-INICIAL-Algo/UT-BACKEND']) {
        mkdirSync(join(base, carpeta), { recursive: true });
      }
      writeFileSync(join(base, 'HU01 - Completa/UT-SEGURIDAD/README.md'), '');
      writeFileSync(join(base, 'HU01 - Completa/UT-SEGURIDAD/UT-HU01-SEG-01.md'), '');
      writeFileSync(join(base, 'HU03 - Con otro numero/UT-SEGURIDAD/README.md'), '');
      writeFileSync(join(base, 'HU03 - Con otro numero/UT-SEGURIDAD/UT-HU01-SEG-01.md'), '');

      assert.deepEqual(sinUtDeSeguridad(base).sort(), ['HU02 - Sin seguridad', 'HU03 - Con otro numero']);
    } finally {
      rmSync(base, { recursive: true, force: true });
    }
  });

  it('ninguna historia del proyecto está sin la suya', () => {
    assert.deepEqual(sinUtDeSeguridad(join(raiz, 'docs', 'UnidadesTrabajo')), []);
  });
});
