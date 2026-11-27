# UT-HUI-OLVIDEPASSWORD-FE-01 — Schemas y hooks de recuperación

| Campo | Valor |
|-------|-------|
| **Código** | `UT-HUI-OLVIDEPASSWORD-FE-01` |
| **Nombre** | Schemas y hooks de recuperación |
| **Historia** | HU-INICIAL-OlvidePassword-FE |
| **Depende de** | UT-HUI-OLVIDEPASSWORD-BE-05 (contrato OpenAPI) |

## Antes de implementar

Leer `frontend/FORMULARIOS_ZOD_TANSTACK_FRONTEND.md`, `frontend/TANSTACK_QUERY_FRONTEND.md` y `.claude/rules/SEGURIDAD_OWASP.md`.

## Solicitud de negocio

El formulario y las llamadas deben compartir validación.

## Objetivo

Existen los schemas y los hooks `useForgotPassword` y `useResetPassword`.

## Alcance

- `pnpm openapi:types` desde el `docs/api/openapi.json` que actualiza la UT BE-05; los tipos de request salen de ahí (`contractTypes.test.ts`).
- `schemas/forgotPasswordSchema.ts` (correo) y `schemas/resetPasswordSchema.ts` (contraseña de 8 caracteres como mínimo y **72 bytes** como máximo, igual que `loginSchema`, y confirmación igual).
- `api/useForgotPassword.ts` (`POST /auth/password/forgot`) y `api/useResetPassword.ts` (`POST /auth/password/reset`, token en el body). El token CSRF ya lo gestiona `apiClient`.
- Al restablecer con éxito, el servidor revoca todas las sesiones: `useResetPassword` deja la query de sesión en `null` y limpia la caché, como al cerrar sesión.
- El token no entra en ninguna `queryKey` ni `mutationKey`.

**Archivos esperados (crear/tocar):**

- `projects/frontend/src/features/auth/schemas/forgotPasswordSchema.ts`
- `projects/frontend/src/features/auth/schemas/resetPasswordSchema.ts`
- `projects/frontend/src/features/auth/api/useForgotPassword.ts`
- `projects/frontend/src/features/auth/api/useResetPassword.ts`
- `projects/frontend/src/shared/api/schema.d.ts` (generado)

## Fuera de alcance (no tocar)

- Pantallas y rutas.

## Resultado esperado

Los schemas validan y los hooks llaman a los endpoints.

## Validación

1. `pnpm test`: schemas (7 caracteres y más de 72 bytes fallan, sin otras reglas) y hooks con `apiClient` simulado, incluido que la sesión queda en `null` tras restablecer.
2. `pnpm lint` y `pnpm build` en verde.

## Seguridad

A05: validación en cliente; token solo por body.
