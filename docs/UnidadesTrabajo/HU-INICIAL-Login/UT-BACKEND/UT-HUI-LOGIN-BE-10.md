# UT-HUI-LOGIN-BE-10 — Límite de intentos atómico por origen y cuenta, sin agotar conexiones

| Campo | Valor |
|-------|-------|
| **Código** | `UT-HUI-LOGIN-BE-10` |
| **Nombre** | Límite de intentos atómico por origen y cuenta, sin agotar conexiones |
| **Historia** | HU-INICIAL-Login-BE |
| **Depende de** | UT-HUI-LOGIN-BE-09 |

## Antes de implementar

`backend/SEGURIDAD_AUTH_BACKEND.md`, `backend/BITACORA_BACKEND.md`, `backend/TESTING_BACKEND.md`.

## Solicitud de negocio

El límite de intentos no puede saltarse en paralelo, usarse para dejar fuera a otra persona ni tumbar el servicio.

## Objetivo

`LoginAttemptService` reserva cada intento de forma atómica por correo+IP y por cuenta, y el login nunca toma dos conexiones a la vez.

## Alcance

- `LoginAttemptService`: script Lua en Redis (`reservar`, `confirmarExito`, `liberar`); `app.auth.max-failed-attempts-per-account` (100).
- `AuthService.login()` sin `@Transactional`: lectura corta, BCrypt fuera de transacción y escritura en otra transacción.
- `SesionService`: el evento de reutilización usa la misma transacción que la revocación.
- `spring.jpa.open-in-view=false`: la conexión no se retiene durante todo el request.

**Archivos esperados (crear/tocar):**

- `projects/backend/src/main/java/com/example/backend/features/auth/service/LoginAttemptService.java` y `AuthService.java`, `SesionService.java`
- `projects/backend/src/main/resources/application.properties`
- `projects/backend/src/test/java/com/example/backend/features/auth/AuthAbusoTest.java` y `AuthPoolTest.java`

## Seguridad

A07/API4: fuerza bruta y denegación de servicio. Falla cerrado si Redis no responde.

## Fuera de alcance (no tocar)

CAPTCHA, dispositivos de confianza y límites en nginx.

## Resultado esperado

40 intentos simultáneos evalúan 5 contraseñas; otra IP no se ve afectada; el pool no se agota.

## Validación

1. `AuthAbusoTest` y `AuthPoolTest` en verde (pool de 3 conexiones con 24 logins simultáneos).
