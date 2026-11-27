# UT-HUI-OLVIDEPASSWORD-BE-03 — PasswordResetTokenService

| Campo | Valor |
|-------|-------|
| **Código** | `UT-HUI-OLVIDEPASSWORD-BE-03` |
| **Nombre** | PasswordResetTokenService |
| **Historia** | HU-INICIAL-OlvidePassword-BE |
| **Depende de** | Redis (existente) |

## Antes de implementar

Leer `backend/SEGURIDAD_AUTH_BACKEND.md` y `.claude/rules/SEGURIDAD_OWASP.md`.

## Solicitud de negocio

El enlace necesita un token de un solo uso que caduque y que no se gaste antes de tiempo.

## Objetivo

Existe un servicio que emite, consulta y consume tokens de restablecimiento de forma atómica.

## Alcance

`features/auth/service/PasswordResetTokenService.java`, con claves `auth:reset:{sha256(token)}` → `codigo` del usuario y `auth:reset:user:{codigo}` → hash vigente, ambas con TTL `app.auth.reset-ttl` (30 min):

- `String emitir(UUID usuario)`: 32 bytes de `SecureRandom` en base64url. Un **script Lua** borra el token anterior del usuario y guarda el nuevo: dos solicitudes simultáneas no pueden dejar dos enlaces vigentes.
- `Optional<UUID> consultar(String token)`: **no consume**.
- `boolean consumir(String token, UUID usuario)`: script Lua que borra ambas claves solo si el token sigue siendo de ese usuario; devuelve `true` a un único llamador.
- Si Redis falla, la excepción se propaga (falla cerrado).
- Los scripts leen claves que no van todas en `KEYS`: válido con un Redis único (el de este proyecto), no con Redis Cluster. Dejarlo comentado.

**Archivos esperados (crear/tocar):**

- `projects/backend/src/main/java/com/example/backend/features/auth/service/PasswordResetTokenService.java`
- `projects/backend/src/main/resources/application.properties`
- `projects/backend/src/test/java/com/example/backend/features/auth/service/PasswordResetTokenServiceTest.java`

## Seguridad

A04/A07: entropía alta, solo el hash en Redis, caducidad y un solo uso atómico.

## Fuera de alcance (no tocar)

- Cambiar contraseñas y los códigos de error (UT 05).
- Envío de correo.

## Resultado esperado

Un token se puede consultar sin gastarlo, se consume una sola vez, caduca y queda anulado al emitir otro.

## Validación

1. Tests con Redis: consultar no consume; consumir dos veces da `true` y `false`; el anterior deja de servir; TTL ≈ 30 min.
2. Test concurrente: varias llamadas simultáneas a `consumir` dan un solo `true`.
3. Redis no contiene el token en claro.
