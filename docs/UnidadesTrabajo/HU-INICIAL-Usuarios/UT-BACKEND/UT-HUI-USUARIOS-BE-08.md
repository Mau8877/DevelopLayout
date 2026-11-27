# UT-HUI-USUARIOS-BE-08 — Aceptar la invitación

| Campo | Valor |
|-------|-------|
| **Código** | `UT-HUI-USUARIOS-BE-08` |
| **Nombre** | Aceptar la invitación |
| **Historia** | HU-INICIAL-Usuarios-BE |
| **Depende de** | 01, 02 |

## Antes de implementar

SEGURIDAD_AUTH_BACKEND (recuperación, endpoints públicos y rate limit) y `RecuperacionService.restablecer`.

## Solicitud de negocio

La persona invitada elige su contraseña con el enlace.

## Objetivo

Endpoint público `POST /api/v1/auth/invitation/accept`.

## Alcance

- `AceptarInvitacionRequest { token, password }`, mismas validaciones que `ResetPasswordRequest`.
- `InvitacionService.aceptar`, con el mismo patrón que el restablecimiento: consultar el token; lectura (cuenta `ACTIVO` y pendiente); BCrypt sin conexión; y escritura con `bloquearParaCambio`, que vuelve a exigir `ACTIVO` y pendiente, guarda el hash, quita `invitacionPendiente`, registra `AUTH_INVITACION_ACEPTADA` y **consume el token al final**.
- Después de confirmar, anular el enlace de recuperación que pudiera haber; si falla, solo se registra en el log.
- Cualquier enlace inservible (vencido, usado, reemplazado, anulado, cuenta inactiva o ya aceptada): `ERR_AUTH_11` (400) con el mismo mensaje, que pide solicitar otra invitación al administrador.
- No inicia sesión: la persona va al login.
- `SecurityConfig`: `permitAll` solo para ese `POST`; CSRF sigue activo en web. `RateLimitFilter`: la ruta usa el límite por IP del restablecimiento.
- Actualizar `docs/api/openapi.json` en esta misma UT (`OpenApiContractTest`). Los nombres de los métodos de controller son únicos entre todos los controllers; si no, springdoc renombra operaciones que ya existían.

**Archivos esperados (crear/tocar):**

- `projects/backend/src/main/java/com/example/backend/features/auth/controller/AuthController.java`
- `projects/backend/src/main/java/com/example/backend/features/auth/service/InvitacionService.java`
- `projects/backend/src/main/java/com/example/backend/features/auth/dto/AceptarInvitacionRequest.java`
- `projects/backend/src/main/java/com/example/backend/features/auth/AuthErrorCodes.java`
- `projects/backend/src/main/java/com/example/backend/features/auth/AuthAcciones.java`
- `projects/backend/src/main/java/com/example/backend/features/usuarios/model/Usuario.java`
- `projects/backend/src/main/java/com/example/backend/config/SecurityConfig.java`
- `projects/backend/src/main/java/com/example/backend/security/RateLimitFilter.java`
- `projects/backend/src/test/java/com/example/backend/features/auth/AceptarInvitacionControllerTest.java`
- `docs/errors/auth/ERRORES_AUTH.md`
- `docs/errors/README.md`
- `docs/api/openapi.json`

## Seguridad

`permitAll` justificado: quien acepta todavía no tiene contraseña y la credencial es el token. A07: un solo uso, sin revelar el motivo del rechazo, límite por IP.

## Fuera de alcance (no tocar)

- Aceptación por «Olvidé mi contraseña» (UT 09).

## Resultado esperado

Una invitación vigente deja la cuenta lista para iniciar sesión.

## Validación

1. Enlace vigente y contraseña válida: 200, login correcto y evento sin contraseña ni enlace.
2. Contraseña fuera de la política: 400 y el enlace sigue sirviendo.
3. Acceso denegado: vencido, usado, reemplazado, anulado o de recuperación: `ERR_AUTH_11` sin cambios; web sin CSRF: 403.
4. Dos aceptaciones simultáneas del mismo enlace: solo una gana.
5. Superado el límite por IP: 429.
