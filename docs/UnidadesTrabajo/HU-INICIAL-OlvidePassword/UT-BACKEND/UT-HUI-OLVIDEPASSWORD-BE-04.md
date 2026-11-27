# UT-HUI-OLVIDEPASSWORD-BE-04 — Solicitar recuperación: POST /auth/password/forgot

| Campo | Valor |
|-------|-------|
| **Código** | `UT-HUI-OLVIDEPASSWORD-BE-04` |
| **Nombre** | Solicitar recuperación: POST /auth/password/forgot |
| **Historia** | HU-INICIAL-OlvidePassword-BE |
| **Depende de** | UT-HUI-OLVIDEPASSWORD-BE-02, UT-HUI-OLVIDEPASSWORD-BE-03 |

## Antes de implementar

Leer `backend/SEGURIDAD_AUTH_BACKEND.md` (Conexiones, Rate limit), `backend/BITACORA_BACKEND.md`, `backend/ENDPOINTS_BACKEND.md` y `.claude/rules/SEGURIDAD_OWASP.md` (API6).

## Solicitud de negocio

Permite recuperar el acceso sin revelar qué correos existen ni inundar un buzón.

## Objetivo

Existe `POST /api/v1/auth/password/forgot`, que responde antes de hacer nada que dependa de la cuenta.

## Alcance

- `features/auth/service/RecuperacionService.java` (servicio nuevo: `AuthService` no crece). `solicitar(email, origen)`, **sin `@Transactional`**:
  1. En el hilo de la petición, y **sin mirar la base**: reserva en el límite por correo.
  2. Si hay cupo, entrega el resto a un ejecutor y retorna. Sin cupo, o con la cola llena: `WARN` con hash corto y retorna igual. La respuesta es siempre la misma.
- En segundo plano: transacción corta de lectura con `RlsContext.usarCorreoDeLogin`; si la cuenta existe y está activa, **ya sin conexión tomada** emite el token y envía el correo. Todo error se captura ahí y se registra (`ERROR`, sin correo ni token).
- `features/auth/service/SolicitudesRecuperacion.java`: contador en Redis `auth:forgot:{sha256(email)}` con incremento y expiración **atómicos** (Lua), ventana de 1 h, `app.auth.forgot-max-per-hour=3`. Si se supera no se emite token: el último enlace sigue valiendo.
- Ejecutor propio y **acotado** (2 hilos, cola de 100, rechazo explícito), que al apagar espera los envíos pendientes unos segundos. Declararlo sin que sustituya al ejecutor por defecto de Spring (`defaultCandidate = false`) y propagar el `module` del MDC.
- Bitácora `AUTH_RECUPERACION_SOLICITADA` (`EXITO` o `FALLO` según el envío), solo si la cuenta existe. En segundo plano no hay petición en curso: agregar a `BitacoraService` una sobrecarga de `registrarEnTransaccionPropia` que reciba actor y origen (IP y user agent), capturados por el controller. `detalle` solo con `correoRef`.
- `AuthController`: `POST /password/forgot` con `ForgotPasswordRequest` (`@NotBlank @Email @Size(max=255)`) → 200, mensaje genérico, `data: null`.
- `RateLimitFilter`: bucket propio por IP (`app.ratelimit.forgot-capacity=5`, `forgot-refill-per-minute=1`) → 429 `ERR_SYS_02`. Ajustar sus tests unitarios y **subir el límite en `application-test.properties`** (los tests comparten IP).
- `SecurityConfig`: `permitAll` solo para este `POST`, con comentario. En web el CSRF sigue activo.

**Archivos esperados (crear/tocar):**

- `projects/backend/src/main/java/com/example/backend/features/auth/service/RecuperacionService.java`
- `projects/backend/src/main/java/com/example/backend/features/auth/service/SolicitudesRecuperacion.java`
- `projects/backend/src/main/java/com/example/backend/config/RecuperacionEjecutorConfig.java`
- `projects/backend/src/main/java/com/example/backend/features/auth/controller/AuthController.java`
- `projects/backend/src/main/java/com/example/backend/features/auth/dto/ForgotPasswordRequest.java`
- `projects/backend/src/main/java/com/example/backend/features/auth/AuthAcciones.java`
- `projects/backend/src/main/java/com/example/backend/common/bitacora/BitacoraService.java`
- `projects/backend/src/main/java/com/example/backend/common/bitacora/OrigenSolicitud.java`
- `projects/backend/src/main/java/com/example/backend/config/SecurityConfig.java`
- `projects/backend/src/main/java/com/example/backend/security/RateLimitFilter.java`
- `projects/backend/src/main/resources/application.properties`
- `projects/backend/src/test/resources/application-test.properties`
- `projects/backend/src/test/java/com/example/backend/security/RateLimitFilterTest.java`

## Seguridad

A07/API6: misma respuesta y mismo trabajo en la petición para cualquier correo; límites por IP y por correo. API4: cola acotada. A09: sin correo ni token en logs. Ningún envío ocurre con una conexión tomada.

## Fuera de alcance (no tocar)

- Restablecer la contraseña (UT 05).
- Frontend y mobile.
- Reintentos o cola persistente: un envío perdido se resuelve pidiendo otro enlace.

## Resultado esperado

Cualquier correo recibe la misma respuesta inmediata; solo las cuentas activas reciben el enlace.

## Validación

1. Con Mailpit: una cuenta activa recibe el mensaje; un correo inexistente no; las respuestas son idénticas.
2. Con el envío bloqueado a propósito, la respuesta vuelve igual.
3. Cuarta solicitud en una hora para el mismo correo: 200, sin correo nuevo y el enlace anterior sigue sirviendo.
4. Superar el límite por IP: 429.
