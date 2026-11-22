# UT-HUI-REFRESH-BE-02 — AuthService.refresh y endpoint POST /refresh

| Campo | Valor |
|-------|-------|
| **Código** | `UT-HUI-REFRESH-BE-02` |
| **Nombre** | AuthService.refresh y endpoint POST /refresh |
| **Historia** | HU-INICIAL-Refresh-BE |
| **Depende de** | UT-HUI-REFRESH-BE-01, UT-HUI-REFRESH-BE-01 |

## Antes de implementar

Leer `backend/SEGURIDAD_AUTH_BACKEND.md`, `backend/ENDPOINTS_BACKEND.md`, `backend/LOGGING_BACKEND.md` y `.claude/rules/SEGURIDAD_OWASP.md`.

## Solicitud de negocio

El cliente debe renovar su sesión sin volver a pedir contraseña.

## Objetivo

Existe `POST /api/v1/auth/refresh` que entrega nuevos tokens.

## Alcance

- Editar `AuthService` agregando solo `refresh(String refreshToken)`: rota, recarga usuario y roles activos desde la base con `RlsContext.usarUsuario`, rechaza (401 `ERR_AUTH_04`) si el usuario está inactivo o sin roles, emite access nuevo, log `INFO` (y `WARN` ante reutilización).
- Editar `AuthController` agregando `POST /refresh`: web lee la cookie `refreshToken` (nunca el body) y setea ambas cookies nuevas; con `X-Client-Type: mobile` lee el body y devuelve los tokens en el body.
- Editar `SecurityConfig`: `permitAll` SOLO para `POST /api/v1/auth/refresh` (la autenticación es el refresh token en sí; el CSRF sigue activo en web). Justificación en comentario.
- Rate limit propio para `/refresh`.
- El access token nuevo lleva el mismo `sid` de la sesión; si la sesión ya no es `ACTIVA` no se emite.
- `AuthService.refresh` recibe IP y user agent y los pasa a `SesionService.rotar`.

**Archivos esperados (crear/tocar):**

- `projects/backend/src/main/java/com/example/backend/features/auth/service/AuthService.java`
- `projects/backend/src/main/java/com/example/backend/features/auth/controller/AuthController.java`
- `projects/backend/src/main/java/com/example/backend/features/auth/dto/RefreshRequest.java`
- `projects/backend/src/main/java/com/example/backend/config/SecurityConfig.java`
- `projects/backend/src/main/java/com/example/backend/security/RateLimitFilter.java`

## Fuera de alcance (no tocar)

- Logout.
- Cambiar la emisión del JWT.

## Resultado esperado

Con la cookie de refresh y el CSRF, `POST /refresh` devuelve nuevas cookies; con un token malo responde 401.

## Validación

1. `curl -b jar -H "X-XSRF-TOKEN: ..." -X POST /api/v1/auth/refresh` → 200 y nuevas cookies; repetir con la cookie anterior → 401.
2. Sin `X-XSRF-TOKEN` → 403.
3. Con header mobile y body válido → tokens en el body.

## Seguridad

A07/API6: `permitAll` justificado, CSRF activo, límite propio.
