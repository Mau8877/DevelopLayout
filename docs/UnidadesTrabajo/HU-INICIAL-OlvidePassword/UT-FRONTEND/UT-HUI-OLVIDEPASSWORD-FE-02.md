# UT-HUI-OLVIDEPASSWORD-FE-02 — Pantalla Olvidé mi contraseña

| Campo | Valor |
|-------|-------|
| **Código** | `UT-HUI-OLVIDEPASSWORD-FE-02` |
| **Nombre** | Pantalla Olvidé mi contraseña |
| **Historia** | HU-INICIAL-OlvidePassword-FE |
| **Depende de** | UT-HUI-OLVIDEPASSWORD-FE-01 |

## Antes de implementar

Leer `frontend/RUTAS_NAVEGACION_FRONTEND.md`, `frontend/FORMULARIOS_ZOD_TANSTACK_FRONTEND.md` y `.claude/rules/SEGURIDAD_OWASP.md`.

## Solicitud de negocio

El usuario debe poder pedir el enlace desde el login.

## Objetivo

Existe `ForgotPasswordScreen` y el enlace desde `LoginScreen`.

## Alcance

- `screens/ForgotPasswordScreen.tsx` y `routes/forgot-password.tsx` (pública). Tras un 200 muestra siempre el mismo mensaje («si la cuenta existe, te enviamos un enlace»), sin afirmar que se envió. Un 429 muestra el aviso de espera; un error de red, el de `ApiError`.
- Botón deshabilitado mientras se envía.
- Editar `LoginScreen` agregando el enlace «¿Olvidaste tu contraseña?» (con `Link`).

**Archivos esperados (crear/tocar):**

- `projects/frontend/src/features/auth/screens/ForgotPasswordScreen.tsx`
- `projects/frontend/src/features/auth/index.ts`
- `projects/frontend/src/features/auth/screens/LoginScreen.tsx`
- `projects/frontend/src/routes/forgot-password.tsx`
- `projects/frontend/src/routeTree.gen.ts` (generado)

## Fuera de alcance (no tocar)

- Pantalla de restablecer (UT 03).

## Resultado esperado

El usuario pide el enlace y ve el mensaje uniforme.

## Validación

1. Manual con Mailpit: llega el correo; correo inexistente muestra el mismo mensaje.
2. `pnpm lint` y `pnpm build` en verde.

## Seguridad

A07: mensaje uniforme.
