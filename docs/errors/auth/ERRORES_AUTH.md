# Errores — Autenticación (`AUTH`)

## `ERR_AUTH_01`

- **HTTP:** 401
- **Excepción:** `UnauthorizedException`
- **Significado:** el correo no existe o la contraseña es incorrecta; ambos casos devuelven el mismo error a propósito (anti-enumeración).
- **Dónde se lanza:** `AuthService.login()`

## `ERR_AUTH_02`

- **HTTP:** 423
- **Excepción:** `LockedException`
- **Significado:** se alcanzó el máximo de intentos fallidos de ese correo desde esa IP, o el tope de la cuenta sumando todas las IP; bloqueo temporal (15 minutos por defecto).
- **Dónde se lanza:** `AuthService.login()`

## `ERR_AUTH_03`

- **HTTP:** 403
- **Excepción:** `ForbiddenException`
- **Significado:** las credenciales son correctas pero el usuario no está en estado `ACTIVO`.
- **Dónde se lanza:** `AuthService.login()`

## `ERR_AUTH_04`

- **HTTP:** 401
- **Excepción:** `UnauthorizedException`
- **Significado:** el refresh token no sirve para renovar: falta, no tiene una firma válida, ya fue usado (la sesión se revoca), su sesión está cerrada o vencida, o el usuario ya no está activo o no tiene roles.
- **Dónde se lanza:** `SesionService.rotar()` y `AuthService.refresh()`

## `ERR_AUTH_05`

- **HTTP:** 400
- **Excepción:** `ValidationException`
- **Significado:** el enlace para restablecer la contraseña no sirve: no existe, venció, ya se usó, se pidió otro después, la cuenta dejó de estar activa o la contraseña cambió mientras tanto.
- **Dónde se lanza:** `RecuperacionService.restablecer()`

## `ERR_AUTH_06`

- **HTTP:** 400
- **Excepción:** `ValidationException`
- **Significado:** la contraseña nueva es igual a la actual. El enlace sigue sirviendo.
- **Dónde se lanza:** `RecuperacionService.restablecer()`

## `ERR_AUTH_07`

- **HTTP:** 401
- **Excepción:** ninguna (lo escribe `RestAuthenticationEntryPoint` a nivel de filtros)
- **Significado:** la request llegó sin credenciales válidas: falta el access token, es inválido, está vencido o su sesión fue revocada.
- **Dónde se lanza:** `RestAuthenticationEntryPoint.commence()`; también `JwtService.validar()` lanza `UnauthorizedException` con este código.

## `ERR_AUTH_08`

- **HTTP:** 403
- **Excepción:** ninguna (lo escribe `RestAccessDeniedHandler` a nivel de filtros)
- **Significado:** el usuario está autenticado pero no tiene permiso, o una request web con cookies no trajo un token CSRF válido.
- **Dónde se lanza:** `RestAccessDeniedHandler.handle()` (reglas de ruta, CSRF y rechazos de `@PreAuthorize`, que `GlobalExceptionHandler` relanza).

## `ERR_AUTH_09`

- **HTTP:** 404
- **Excepción:** `NotFoundException`
- **Significado:** la sesión que se quiere cerrar no existe. En «Mis sesiones» incluye la de otro usuario: no se distingue de una inexistente.
- **Dónde se lanza:** `SesionAdminService.cerrar()` y `MisSesionesService.cerrar()`

## `ERR_AUTH_10`

- **HTTP:** 400
- **Excepción:** `ValidationException`
- **Significado:** se intentó cerrar desde «Mis sesiones» la sesión desde la que se hace la petición; para eso está «Cerrar sesión».
- **Dónde se lanza:** `MisSesionesService.cerrar()`

## `ERR_AUTH_11`

- **HTTP:** 400
- **Excepción:** `ValidationException`
- **Significado:** la invitación no sirve: venció, ya se usó, fue reemplazada o anulada, o la cuenta está inactiva.
- **Dónde se lanza:** `InvitacionService.aceptar()`
