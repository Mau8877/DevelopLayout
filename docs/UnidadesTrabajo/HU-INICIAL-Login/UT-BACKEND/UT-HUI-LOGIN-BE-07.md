# UT-HUI-LOGIN-BE-07 — AuthService.login y me

| Campo | Valor |
|-------|-------|
| **Código** | `UT-HUI-LOGIN-BE-07` |
| **Nombre** | AuthService.login y me |
| **Historia** | HU-INICIAL-Login-BE |
| **Depende de** | UT-HUI-LOGIN-BE-02, UT-HUI-LOGIN-BE-03, UT-HUI-LOGIN-BE-04, UT-HUI-LOGIN-BE-05, UT-HUI-LOGIN-BE-05, HU-INICIAL-Base: BitacoraService (UT-HUI-BASE-BE-06) |

## Antes de implementar

Leer `backend/SEGURIDAD_AUTH_BACKEND.md`, `backend/LOGGING_BACKEND.md`, `backend/EXCEPCIONES_BACKEND.md`, `backend/GUIA_ERRORES_BACKEND.md` y `.claude/rules/SEGURIDAD_OWASP.md`.

## Solicitud de negocio

Es la lógica de entrar al sistema, con las defensas anti-enumeración y de fuerza bruta.

## Objetivo

`AuthService` valida credenciales y devuelve usuario, access token y refresh token, y resuelve la sesión actual.

## Alcance

- `features/auth/service/AuthService.java` con solo `login(...)` y `me(UUID)`.
- Orden: normalizar correo → `estaBloqueado` (si sí, `LockedException` `ERR_AUTH_02`) → cargar usuario; si no existe, comparar contra un hash BCrypt ficticio para igualar el tiempo → verificar contraseña → si falla, `registrarFallo` + `WARN` + `UnauthorizedException` `ERR_AUTH_01` (mismo mensaje para inexistente y contraseña incorrecta) → si `estado != ACTIVO`, `ForbiddenException` `ERR_AUTH_03` → `reiniciar`, emitir access y refresh, log `INFO`.
- Devuelve un `LoginResult` interno; el controller decide cookie o body.
- `UsuarioSesionResponse` (`codigo`, `email`, `roles`) sin `passwordHash` ni `id`.
- Logs con `module=auth` y `errorCode`; nunca contraseña ni tokens, y en `WARN` el correo va como hash corto.
- `ERR_AUTH_01/02/03` en `AuthErrorCodes` y sus secciones en `docs/errors/auth/ERRORES_AUTH.md` + índice.
- Crea la sesión con `SesionService.crear(usuario, tipoCliente, ip, userAgent)` y usa el `sid` al emitir el JWT (`LoginCommand` lleva IP, user agent y tipo de cliente).
- Bitácora (`BitacoraService`, constantes en `AuthAcciones`): `AUTH_LOGIN_OK` en la transacción del login; `AUTH_LOGIN_FALLO` y `AUTH_CUENTA_BLOQUEADA` con `registrarEnTransaccionPropia` (el error provoca rollback), con el correo solo como hash corto en `detalle`.

**Archivos esperados (crear/tocar):**

- `projects/backend/src/main/java/com/example/backend/features/auth/service/AuthService.java`
- `projects/backend/src/main/java/com/example/backend/features/auth/dto/UsuarioSesionResponse.java`
- `projects/backend/src/main/java/com/example/backend/features/auth/AuthErrorCodes.java`
- `docs/errors/auth/ERRORES_AUTH.md`
- `docs/errors/README.md`
- `projects/backend/src/main/java/com/example/backend/features/auth/AuthAcciones.java`

## Fuera de alcance (no tocar)

- Cookies, `ResponseEntity` y rutas (UT-HUI-LOGIN-BE-08).
- Refresh/rotación y logout.
- Recuperación de contraseña.

## Resultado esperado

Credenciales correctas devuelven tokens; incorrectas, bloqueo e inactivo producen los errores esperados con tiempos equivalentes.

## Validación

1. Tests de servicio: éxito, contraseña mala (401), correo inexistente (401 mismo mensaje), 5 fallos → 423, inactivo → 403.
2. Test de tiempo: la diferencia entre correo inexistente y contraseña mala está dentro de un margen razonable (ambos ejecutan BCrypt).
3. Logs capturados: sin contraseña ni token.
4. Tras un login exitoso hay una fila en `sesiones` (`ACTIVA`) y un evento `AUTH_LOGIN_OK`; tras uno fallido, un evento `AUTH_LOGIN_FALLO` sin contraseña, aunque la operación haya hecho rollback.

## Seguridad

A07/A09/API2/API3: anti-enumeración por mensaje y tiempo, lockout, DTO sin campos sensibles.
