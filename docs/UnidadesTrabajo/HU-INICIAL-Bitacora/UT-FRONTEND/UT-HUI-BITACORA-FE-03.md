# UT-HUI-BITACORA-FE-03 — Tests de la pantalla de bitácora

| Campo | Valor |
|-------|-------|
| **Código** | `UT-HUI-BITACORA-FE-03` |
| **Nombre** | Tests de la pantalla de bitácora |
| **Historia** | HU-INICIAL-Bitacora-FE |
| **Depende de** | UT-HUI-BITACORA-FE-02 |

## Antes de implementar

Leer `frontend/TESTING_FRONTEND.md` y `.claude/rules/SEGURIDAD_OWASP.md`.

## Solicitud de negocio

Evitar regresiones.

## Objetivo

Tests.

## Alcance

- `features/bitacora/tests/BitacoraScreen.test.tsx` (filtros, paginación, detalle como texto).

**Archivos esperados (crear/tocar):**

- `projects/frontend/src/features/bitacora/tests/BitacoraScreen.test.tsx`

## Fuera de alcance (no tocar)

- Código de producción.

## Resultado esperado

Pasan.

## Validación

1. `pnpm test` en verde.

## Seguridad

Cubre CA07.
