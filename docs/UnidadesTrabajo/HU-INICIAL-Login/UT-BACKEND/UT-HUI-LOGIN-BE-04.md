# UT-HUI-LOGIN-BE-04 — Bloqueo temporal por intentos fallidos (Redis)

| Campo | Valor |
|-------|-------|
| **Código** | `UT-HUI-LOGIN-BE-04` |
| **Nombre** | Bloqueo temporal por intentos fallidos (Redis) |
| **Historia** | HU-INICIAL-Login-BE |
| **Depende de** | — |

## Antes de implementar

Leer `backend/SEGURIDAD_AUTH_BACKEND.md`, `backend/EXCEPCIONES_BACKEND.md` (`LockedException`) y `.claude/rules/SEGURIDAD_OWASP.md`.

## Solicitud de negocio

Frenar la adivinanza de contraseñas sin bloquear de forma permanente a usuarios legítimos.

## Objetivo

Existe un servicio que cuenta intentos fallidos por correo y bloquea 15 minutos tras 5.

## Alcance

- `features/auth/service/LoginAttemptService.java` con `estaBloqueado(email)`, `registrarFallo(email)` y `reiniciar(email)`.
- Clave Redis `auth:login:fail:{sha256(email normalizado)}` con TTL de 15 min renovado en cada fallo; bloqueo al llegar a 5. El correo no va en claro en la clave.
- `app.auth.max-failed-attempts` (5) y `app.auth.lock-duration` (15 min).
- Si Redis falla, la excepción de infraestructura se propaga (falla cerrado: no se permite el login).

**Archivos esperados (crear/tocar):**

- `projects/backend/src/main/java/com/example/backend/features/auth/service/LoginAttemptService.java`
- `projects/backend/src/main/resources/application.properties`

## Fuera de alcance (no tocar)

- Verificar contraseñas (UT-HUI-LOGIN-BE-07).
- El log `WARN` de login fallido (lo emite `AuthService`, una sola vez).
- Desbloqueo desde recuperación de contraseña (HU-INICIAL-OlvidePassword).

## Resultado esperado

Tras 5 fallos `estaBloqueado` devuelve `true` durante 15 min; `reiniciar` lo limpia.

## Validación

1. Test con Redis de Testcontainers: 4 fallos no bloquean, el 5.º sí.
2. Test: `reiniciar` libera; con TTL corto de test se libera solo.
3. `redis-cli KEYS "auth:login:*"` no muestra el correo.

## Seguridad

A07/API6: límite propio del flujo sensible; privacidad del correo en la clave.
