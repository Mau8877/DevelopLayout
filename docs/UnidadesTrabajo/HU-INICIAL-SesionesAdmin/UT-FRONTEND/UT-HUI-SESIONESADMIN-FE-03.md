# UT-HUI-SESIONESADMIN-FE-03 — Tests de la pantalla de admin

| Campo | Valor |
|-------|-------|
| **Código** | `UT-HUI-SESIONESADMIN-FE-03` |
| **Nombre** | Tests de la pantalla de admin |
| **Historia** | HU-INICIAL-SesionesAdmin-FE |
| **Depende de** | UT-HUI-SESIONESADMIN-FE-02 |

## Antes de implementar

Leer `frontend/TESTING_FRONTEND.md` y `.claude/rules/SEGURIDAD_OWASP.md`.

## Solicitud de negocio

Evitar regresiones.

## Objetivo

Tests.

## Alcance

- `tests/SesionesAdminScreen.test.tsx`.

**Archivos esperados (crear/tocar):**

- `projects/frontend/src/features/auth/tests/SesionesAdminScreen.test.tsx`

## Fuera de alcance (no tocar)

- Código de producción.

## Resultado esperado

Pasan.

## Validación

1. `pnpm test` en verde.

## Seguridad

Cubre CA01 y CA03.
