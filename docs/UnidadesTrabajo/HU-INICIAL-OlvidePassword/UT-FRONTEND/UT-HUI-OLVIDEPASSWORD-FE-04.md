# UT-HUI-OLVIDEPASSWORD-FE-04 — Tests de recuperación web

| Campo | Valor |
|-------|-------|
| **Código** | `UT-HUI-OLVIDEPASSWORD-FE-04` |
| **Nombre** | Tests de recuperación web |
| **Historia** | HU-INICIAL-OlvidePassword-FE |
| **Depende de** | UT-HUI-OLVIDEPASSWORD-FE-03 |

## Antes de implementar

Leer `frontend/TESTING_FRONTEND.md` y `.claude/rules/SEGURIDAD_OWASP.md`.

## Solicitud de negocio

Evitar regresiones de seguridad en el token.

## Objetivo

Tests de pantallas y schemas.

## Alcance

- `tests/ForgotPasswordScreen.test.tsx`, `tests/ResetPasswordScreen.test.tsx` (verifica `replaceState` y que el token no queda en `location`), `tests/resetPasswordSchema.test.ts`.

**Archivos esperados (crear/tocar):**

- `projects/frontend/src/features/auth/tests/ForgotPasswordScreen.test.tsx`
- `projects/frontend/src/features/auth/tests/ResetPasswordScreen.test.tsx`
- `projects/frontend/src/features/auth/tests/resetPasswordSchema.test.ts`

## Fuera de alcance (no tocar)

- Código de producción.

## Resultado esperado

Pasan.

## Validación

1. `pnpm test` y `pnpm lint` en verde.

## Seguridad

Cubre CA05 y CA08.
