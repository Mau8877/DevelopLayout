# UT-HUI-PRUEBASNAVEGADOR-FE-05 — CI, estabilidad y proyectos nuevos

| Campo | Valor |
|-------|-------|
| **Código** | `UT-HUI-PRUEBASNAVEGADOR-FE-05` |
| **Nombre** | CI, estabilidad y proyectos nuevos |
| **Historia** | HU-INICIAL-PruebasNavegador-FE |
| **Depende de** | 02, 03, 04 |

## Antes de implementar

CI_CD (acciones fijadas por SHA y permisos) y la guía de `project:init`.

## Solicitud de negocio

Una prueba que solo corre a mano se abandona.

## Objetivo

Las pruebas corren en el CI y sirven en un proyecto nuevo.

## Alcance

- Job `e2e` en `ci.yml` cuando cambian web, backend, `e2e/` o `scripts/e2e.mjs`: Java, Node, `playwright install --with-deps chromium`; acciones fijadas por SHA; informe y trazas como artefacto solo si falla.
- Ejecutar tres veces seguidas; toda prueba inestable se corrige (no se tapa con reintentos).
- Comprobar con una copia inicializada con `project:init` que corren sin editar nada.
- Actualizar CI_CD, TESTING_FRONTEND y `scripts/README.md`.

**Archivos esperados (crear/tocar):**

- `.github/workflows/ci.yml`
- `.claude/rules/CI_CD.md`
- `.claude/rules/frontend/TESTING_FRONTEND.md`
- `scripts/README.md`

## Seguridad

A03/A08: sin secretos del repositorio, permisos de solo lectura y artefactos con datos desechables.

## Fuera de alcance (no tocar)

- Probar Firefox o WebKit.
- Ejecutarlas en el pre-commit.

## Resultado esperado

El CI detecta fallos de punta a punta.

## Validación

1. Tres ejecuciones locales seguidas en verde.
2. En una copia inicializada pasan sin cambios.
3. `node --test "scripts/**/*.test.mjs"` en verde. La primera ejecución del job solo se puede confirmar en GitHub.
