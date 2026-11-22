# UT-HUI-LOGIN-BE-08 — AuthController: csrf, login y me, cookies y permisos

| Campo | Valor |
|-------|-------|
| **Código** | `UT-HUI-LOGIN-BE-08` |
| **Nombre** | AuthController: csrf, login y me, cookies y permisos |
| **Historia** | HU-INICIAL-Login-BE |
| **Depende de** | UT-HUI-LOGIN-BE-06, UT-HUI-LOGIN-BE-07 |

## Antes de implementar

Leer `backend/SEGURIDAD_AUTH_BACKEND.md`, `backend/ENDPOINTS_BACKEND.md`, `backend/RESPONSES_BACKEND.md` y `.claude/rules/SEGURIDAD_OWASP.md`.

## Solicitud de negocio

Expone el flujo al cliente web y mobile con el contrato HTTP definido.

## Objetivo

Existen `GET /api/v1/auth/csrf`, `POST /api/v1/auth/login` y `GET /api/v1/auth/me`.

## Alcance

- `features/auth/controller/AuthController.java` delgado: `@Valid`, un método de service, `ApiResponse`.
- `GET /csrf` → 204 y cookie `XSRF-TOKEN`. `POST /login` → 200 con `UsuarioSesionResponse` y cookies `accessToken` (Path `/api`, 15 min) y `refreshToken` (Path `/api/v1/auth`, 7 días), `HttpOnly`, `SameSite=Strict`, `Secure` según `app.cookies.secure`; con `X-Client-Type: mobile` devuelve los tokens en el body y no setea cookies. `GET /me` → 200 con el usuario de la sesión.
- `features/auth/AuthCookies.java`: helper para crear/borrar las cookies (reutilizable por refresh y logout).
- `LoginRequest` con `@Email` y `@Size(min=8,max=72)` en `password`; rechaza propiedades desconocidas.
- `SecurityConfig`: `permitAll` SOLO para `GET /api/v1/auth/csrf` y `POST /api/v1/auth/login` (públicos por diseño: aún no hay sesión); `/me` queda autenticado. Justificación en un comentario junto a la regla.
- Rate limit más estricto para `/api/v1/auth/login` por IP, además del global.
- Pasa al service la IP (`request.getRemoteAddr()`), el user agent y el tipo de cliente (header `X-Client-Type`).

**Archivos esperados (crear/tocar):**

- `projects/backend/src/main/java/com/example/backend/features/auth/controller/AuthController.java`
- `projects/backend/src/main/java/com/example/backend/features/auth/AuthCookies.java`
- `projects/backend/src/main/java/com/example/backend/features/auth/dto/LoginRequest.java`
- `projects/backend/src/main/java/com/example/backend/features/auth/dto/LoginResponse.java`
- `projects/backend/src/main/java/com/example/backend/config/SecurityConfig.java`
- `projects/backend/src/main/java/com/example/backend/security/RateLimitFilter.java`
- `projects/backend/src/main/resources/application.properties`

## Fuera de alcance (no tocar)

- `/refresh`, `/logout`, `/password/*` (otras HU).
- Cambiar la lógica de `AuthService`.

## Resultado esperado

Con `curl` se obtiene el CSRF, se inicia sesión (cookies seteadas) y se consulta `/me`.

## Validación

1. `curl -c jar -i /api/v1/auth/csrf` → 204 con `XSRF-TOKEN`.
2. Login con `X-XSRF-TOKEN` → 200 y `Set-Cookie` con `HttpOnly`/`SameSite=Strict`.
3. `/me` con cookies → 200; sin cookies → 401.
4. Login con header mobile devuelve tokens en el body y ninguna cookie.

## Seguridad

A01/A07/API6: `permitAll` mínimo y justificado, cookies seguras, CSRF activo, rate limit específico.
