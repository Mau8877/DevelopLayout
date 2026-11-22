# UT-HUI-OLVIDEPASSWORD-BE-03 — PasswordResetTokenService

| Campo | Valor |
|-------|-------|
| **Código** | `UT-HUI-OLVIDEPASSWORD-BE-03` |
| **Nombre** | PasswordResetTokenService |
| **Historia** | HU-INICIAL-OlvidePassword-BE |
| **Depende de** | Redis (existente), HU-INICIAL-Refresh: SesionService.revocar |

## Antes de implementar

Leer `backend/SEGURIDAD_AUTH_BACKEND.md` y `.claude/rules/SEGURIDAD_OWASP.md`.

## Solicitud de negocio

El enlace de recuperación necesita un token de un solo uso que caduque.

## Objetivo

Existe un servicio que emite, valida y consume tokens de restablecimiento.

## Alcance

- `features/auth/service/PasswordResetTokenService.java`: `emitir(UUID codigo)` (32 bytes `SecureRandom`, guarda `auth:reset:{sha256(token)}` → `codigo` con TTL 30 min; invalida el anterior del mismo usuario con un índice `auth:reset:user:{codigo}`), `consumir(String token)` (devuelve `codigo` y borra la clave; `ValidationException` `ERR_AUTH_05` si no existe).
- `app.auth.reset-ttl` (30 min).
- `ERR_AUTH_05` en `AuthErrorCodes` y su sección en docs/errors + índice.

**Archivos esperados (crear/tocar):**

- `projects/backend/src/main/java/com/example/backend/features/auth/service/PasswordResetTokenService.java`
- `projects/backend/src/main/java/com/example/backend/features/auth/AuthErrorCodes.java`
- `docs/errors/auth/ERRORES_AUTH.md`
- `docs/errors/README.md`
- `projects/backend/src/main/resources/application.properties`

## Fuera de alcance (no tocar)

- Cambiar contraseñas (UT 05).
- Envío de correo.

## Resultado esperado

Un token sirve una vez, caduca a los 30 min y emitir uno nuevo anula el anterior.

## Validación

1. Tests con Redis: consumir dos veces falla, TTL ≈ 30 min, el token viejo falla tras emitir otro.
2. Redis no contiene el token en claro.

## Seguridad

A07/A04: entropía alta, solo hash persistido, un solo uso.
