# UT-HUI-OLVIDEPASSWORD-FE-01 — Schemas y hooks de recuperación

| Campo | Valor |
|-------|-------|
| **Código** | `UT-HUI-OLVIDEPASSWORD-FE-01` |
| **Nombre** | Schemas y hooks de recuperación |
| **Historia** | HU-INICIAL-OlvidePassword-FE |
| **Depende de** | — |

## Antes de implementar

Leer `frontend/FORMULARIOS_ZOD_TANSTACK_FRONTEND.md`, `frontend/TANSTACK_QUERY_FRONTEND.md` y `.claude/rules/SEGURIDAD_OWASP.md`.

## Solicitud de negocio

El formulario y las llamadas deben compartir validación.

## Objetivo

Existen los schemas y los hooks `useForgotPassword` y `useResetPassword`.

## Alcance

- `schemas/forgotPasswordSchema.ts` (correo) y `schemas/resetPasswordSchema.ts` (contraseña mínimo 8, máximo 72 bytes, y confirmación igual).
- `api/useForgotPassword.ts` (`POST /auth/password/forgot`) y `api/useResetPassword.ts` (`POST /auth/password/reset`, token en el body).

**Archivos esperados (crear/tocar):**

- `projects/frontend/src/features/auth/schemas/forgotPasswordSchema.ts`
- `projects/frontend/src/features/auth/schemas/resetPasswordSchema.ts`
- `projects/frontend/src/features/auth/api/useForgotPassword.ts`
- `projects/frontend/src/features/auth/api/useResetPassword.ts`

## Fuera de alcance (no tocar)

- Pantallas y rutas.

## Resultado esperado

Los schemas validan y los hooks llaman a los endpoints.

## Validación

1. `pnpm test`: schemas (contraseña de 7 caracteres falla, sin otras reglas) y hooks con `apiClient` mockeado.

## Seguridad

A05: validación en cliente; token solo por body.
