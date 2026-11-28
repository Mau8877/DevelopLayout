# UT-HUI-CAMBIARPASSWORD-BE-05 — Endpoint, límite por IP y contrato

| Campo | Valor |
|-------|-------|
| **Código** | `UT-HUI-CAMBIARPASSWORD-BE-05` |
| **Nombre** | Endpoint, límite por IP y contrato |
| **Historia** | HU-INICIAL-CambiarPassword-BE |
| **Depende de** | 04 |

## Antes de implementar

ENDPOINTS_BACKEND, RESPONSES_BACKEND, SEGURIDAD_AUTH_BACKEND (rate limit) y TESTING_BACKEND.

## Solicitud de negocio

La web y la app necesitan una forma segura de pedir el cambio.

## Objetivo

`POST /api/v1/auth/password/change` publicado, limitado y documentado.

## Alcance

- `CambiarPasswordRequest(passwordActual, passwordNueva)`: `record` que rechaza propiedades desconocidas. `passwordActual`: obligatoria y hasta 72. `passwordNueva`: de 8 a 72 caracteres y como máximo 72 bytes (`@MaximoBytes`). Mensajes por clave.
- Método delgado en `AuthController`: usuario con `@CurrentUserId` y sesión con el `sid` del token, igual que Mis sesiones. En web (sin `X-Client-Type: mobile`) borra las cookies al responder con éxito, **no** ante un error.
- La ruta es privada: no se agrega a `permitAll` y el CSRF sigue activo en web.
- `RateLimitFilter`: límite propio por IP para esta ruta (ráfaga de 10, 5 por minuto; `app.ratelimit.change-capacity` y `change-refill-per-minute`).
- Actualizar `docs/api/openapi.json` y la rule de seguridad (estado de los flujos y reglas de este).

**Archivos esperados (crear/tocar):**

- `projects/backend/src/main/java/com/example/backend/features/auth/dto/CambiarPasswordRequest.java`
- `projects/backend/src/main/java/com/example/backend/features/auth/controller/AuthController.java`
- `projects/backend/src/main/java/com/example/backend/security/RateLimitFilter.java`
- `projects/backend/src/main/resources/application.properties`
- `projects/backend/src/test/java/com/example/backend/features/auth/CambioPasswordControllerTest.java`
- `projects/backend/src/test/java/com/example/backend/security/RateLimitFilterTest.java`
- `docs/api/openapi.json`
- `.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md`

## Seguridad

A01, A05 y A07; API3 (sin mass assignment: la cuenta no llega en el body) y API6 (límite propio del flujo sensible).

## Fuera de alcance (no tocar)

- La lógica del servicio.
- Tocar `SecurityConfig` para abrir la ruta.
- La web y la app.

## Resultado esperado

El contrato queda disponible para los clientes.

## Validación

1. Acceso denegado: sin token, 401. Web sin CSRF: 403 y la contraseña no cambia. Se prueba el CSRF contra un servidor real.
2. Éxito web: 200 y dos cookies borradas; mobile: 200 sin cookies.
3. Un error (400, 423) no borra las cookies.
4. Una propiedad extra (por ejemplo `email`) da 400. Cada error se comprueba en español y en inglés.
5. Superar el límite por IP da 429 con el sobre estándar.
6. La prueba del contrato OpenAPI queda en verde.
