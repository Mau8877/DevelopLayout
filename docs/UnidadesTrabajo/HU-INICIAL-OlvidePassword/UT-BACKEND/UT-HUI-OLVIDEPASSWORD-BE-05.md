# UT-HUI-OLVIDEPASSWORD-BE-05 — Restablecer contraseña: POST /auth/password/reset

| Campo | Valor |
|-------|-------|
| **Código** | `UT-HUI-OLVIDEPASSWORD-BE-05` |
| **Nombre** | Restablecer contraseña: POST /auth/password/reset |
| **Historia** | HU-INICIAL-OlvidePassword-BE |
| **Depende de** | UT-HUI-OLVIDEPASSWORD-BE-03, UT-HUI-OLVIDEPASSWORD-BE-04 |

## Antes de implementar

Leer `backend/SEGURIDAD_AUTH_BACKEND.md`, `backend/LOGGING_BACKEND.md`, `backend/GUIA_ERRORES_BACKEND.md` y `.claude/rules/SEGURIDAD_OWASP.md`.

## Solicitud de negocio

El usuario debe poder definir una contraseña nueva con el enlace recibido.

## Objetivo

Existe `POST /api/v1/auth/password/reset` que cambia la contraseña y cierra todas las sesiones.

## Alcance

- Editar `AuthService` agregando solo `restablecer(String token, String nuevaPassword)`: consume el token, fija `RlsContext.usarUsuario(codigo)`, rechaza (400 `ERR_AUTH_06`) si la contraseña nueva coincide con la actual, guarda el hash BCrypt, llama a `SesionService.revocarTodas(usuario, RESET_PASSWORD)`, `LoginAttemptService.reiniciar` y registra `INFO` sin contraseña ni token.
- Editar `AuthController`: `POST /password/reset` (`ResetPasswordRequest`: `token`, `password` con `@Size(min=8,max=72)`; el token va en el body, nunca en la URL de la API). `permitAll` solo para esta ruta, con comentario.
- `ERR_AUTH_06` en `AuthErrorCodes` y su sección en docs/errors + índice.
- Límite propio por IP.
- Editar `SesionService` agregando solo `revocarTodas(UUID usuario, MotivoRevocacion motivo)`: revoca todas las sesiones `ACTIVA` del usuario usando `revocar`.
- Bitácora: `AUTH_PASSWORD_RESTABLECIDA` (misma transacción), sin token ni contraseña.

**Archivos esperados (crear/tocar):**

- `projects/backend/src/main/java/com/example/backend/features/auth/service/AuthService.java`
- `projects/backend/src/main/java/com/example/backend/features/auth/controller/AuthController.java`
- `projects/backend/src/main/java/com/example/backend/features/auth/dto/ResetPasswordRequest.java`
- `projects/backend/src/main/java/com/example/backend/features/auth/AuthErrorCodes.java`
- `projects/backend/src/main/java/com/example/backend/config/SecurityConfig.java`
- `docs/errors/auth/ERRORES_AUTH.md`
- `docs/errors/README.md`
- `projects/backend/src/main/java/com/example/backend/features/auth/service/SesionService.java`
- `projects/backend/src/main/java/com/example/backend/features/auth/AuthAcciones.java`

## Fuera de alcance (no tocar)

- Enviar el correo (UT 04).
- Cambio de contraseña estando logueado.

## Resultado esperado

Con un token válido y una contraseña nueva, se puede iniciar sesión con ella y las sesiones anteriores dejan de funcionar.

## Validación

1. Flujo completo con Mailpit y `curl`: forgot → reset → login con la nueva (200) y con la vieja (401).
2. Reutilizar el token → 400 `ERR_AUTH_05`.
3. Contraseña de 7 caracteres → 400; igual a la actual → 400 `ERR_AUTH_06`.
4. Un refresh token anterior → 401.

## Seguridad

A07/A09: token de un solo uso, sesiones revocadas, nada sensible en logs.
