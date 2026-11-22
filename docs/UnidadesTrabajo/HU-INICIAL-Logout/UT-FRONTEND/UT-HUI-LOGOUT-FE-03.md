# UT-HUI-LOGOUT-FE-03 — Tests de logout web

| Campo | Valor |
|-------|-------|
| **Código** | `UT-HUI-LOGOUT-FE-03` |
| **Nombre** | Tests de logout web |
| **Historia** | HU-INICIAL-Logout-FE |
| **Depende de** | UT-HUI-LOGOUT-FE-02 |

## Antes de implementar

Leer `frontend/TESTING_FRONTEND.md` y `.claude/rules/SEGURIDAD_OWASP.md`.

## Solicitud de negocio

Evitar regresiones.

## Objetivo

Tests del hook y del botón.

## Alcance

- `tests/useLogout.test.ts` y test del callback en el layout.

**Archivos esperados (crear/tocar):**

- `projects/frontend/src/features/auth/tests/useLogout.test.ts`

## Fuera de alcance (no tocar)

- Código de producción.

## Resultado esperado

Pasan.

## Validación

1. `pnpm test` en verde.

## Seguridad

Cubre CA03 y CA04.
