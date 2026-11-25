# Errores — Autenticación (`AUTH`)

## `ERR_AUTH_01`

- **HTTP:** 401
- **Excepción:** `UnauthorizedException`
- **Significado:** el correo no existe o la contraseña es incorrecta; ambos casos devuelven el mismo error a propósito (anti-enumeración).
- **Dónde se lanza:** `AuthService.login()`

## `ERR_AUTH_02`

- **HTTP:** 423
- **Excepción:** `LockedException`
- **Significado:** el correo superó el máximo de intentos fallidos de login y está bloqueado temporalmente (15 minutos por defecto).
- **Dónde se lanza:** `AuthService.login()`

## `ERR_AUTH_03`

- **HTTP:** 403
- **Excepción:** `ForbiddenException`
- **Significado:** las credenciales son correctas pero el usuario no está en estado `ACTIVO`.
- **Dónde se lanza:** `AuthService.login()`

## `ERR_AUTH_07`

- **HTTP:** 401
- **Excepción:** ninguna (lo escribe `RestAuthenticationEntryPoint` a nivel de filtros)
- **Significado:** la request llegó sin credenciales válidas: falta el access token, es inválido, está vencido o su sesión fue revocada.
- **Dónde se lanza:** `RestAuthenticationEntryPoint.commence()`; también `JwtService.validar()` lanza `UnauthorizedException` con este código.

## `ERR_AUTH_08`

- **HTTP:** 403
- **Excepción:** ninguna (lo escribe `RestAccessDeniedHandler` a nivel de filtros)
- **Significado:** el usuario está autenticado pero no tiene permiso, o una request web con cookies no trajo un token CSRF válido.
- **Dónde se lanza:** `RestAccessDeniedHandler.handle()`
