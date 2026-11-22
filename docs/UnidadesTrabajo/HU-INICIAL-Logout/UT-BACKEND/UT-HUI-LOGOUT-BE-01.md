# UT-HUI-LOGOUT-BE-01 — Revocar token, AuthService.logout y endpoint POST /logout

| Campo | Valor |
|-------|-------|
| **Código** | `UT-HUI-LOGOUT-BE-01` |
| **Nombre** | Revocar token, AuthService.logout y endpoint POST /logout |
| **Historia** | HU-INICIAL-Logout-BE |
| **Depende de** | Refresh: RefreshTokenService (HU-INICIAL-Refresh) |

## Antes de implementar

Leer `backend/SEGURIDAD_AUTH_BACKEND.md`, `backend/ENDPOINTS_BACKEND.md` y `.claude/rules/SEGURIDAD_OWASP.md`.

## Solicitud de negocio

El usuario debe poder invalidar su sesión.

## Objetivo

Existe `POST /api/v1/auth/logout` que revoca el refresh token y borra las cookies.

## Alcance

- Editar `RefreshTokenService` agregando solo `revocar(String tokenEnClaro)`: borra la clave y la saca del set del usuario; idempotente.
- Editar `AuthService` agregando solo `logout(String refreshToken)` con log `INFO`.
- Editar `AuthController` agregando `POST /logout`: web lee la cookie y responde borrando ambas cookies (`Max-Age=0`); mobile lee el body. Siempre 200 (idempotente).
- `SecurityConfig`: `permitAll` SOLO para `POST /api/v1/auth/logout`, para que funcione con el access token vencido (justificado en comentario); el CSRF sigue activo en web.

**Archivos esperados (crear/tocar):**

- `projects/backend/src/main/java/com/example/backend/features/auth/service/RefreshTokenService.java`
- `projects/backend/src/main/java/com/example/backend/features/auth/service/AuthService.java`
- `projects/backend/src/main/java/com/example/backend/features/auth/controller/AuthController.java`
- `projects/backend/src/main/java/com/example/backend/config/SecurityConfig.java`

## Fuera de alcance (no tocar)

- Cerrar todas las sesiones de un usuario por acción de admin.
- Cambios en el cliente.

## Resultado esperado

Tras logout el refresh token deja de funcionar y las cookies se borran.

## Validación

1. `curl` logout → 200 con `Set-Cookie` de `Max-Age=0`; luego `/refresh` con el token anterior → 401; logout repetido → 200.
2. Sin `X-XSRF-TOKEN` → 403.

## Seguridad

A07: invalidación del lado servidor; CSRF activo.
