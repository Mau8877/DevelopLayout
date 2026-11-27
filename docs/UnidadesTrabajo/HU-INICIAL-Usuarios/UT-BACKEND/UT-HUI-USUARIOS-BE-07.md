# UT-HUI-USUARIOS-BE-07 — Reenvío de invitación

| Campo | Valor |
|-------|-------|
| **Código** | `UT-HUI-USUARIOS-BE-07` |
| **Nombre** | Reenvío de invitación |
| **Historia** | HU-INICIAL-Usuarios-BE |
| **Depende de** | 06 |

## Antes de implementar

EXCEPCIONES_BACKEND, BITACORA_BACKEND y `SolicitudesRecuperacion`.

## Solicitud de negocio

Si la invitación venció o no llegó, el administrador la vuelve a enviar.

## Objetivo

`POST /api/v1/usuarios/{codigo}/reenviar-invitacion` con límite por usuario.

## Alcance

- En una transacción: guardia, la cuenta existe (`ERR_US_01`, 404), está `ACTIVO` y pendiente (`ERR_US_05`, 409 con el motivo).
- Después, el límite en Redis: 3 reenvíos por usuario y hora (`app.auth.invite-resend-max-per-hour=3`); superado, `ERR_US_06` (409). El alta no cuenta. Si Redis falla, no se envía.
- Envío con `InvitacionEnvio` (invalida el enlace anterior) y bitácora `USUARIOS_INVITACION_REENVIADA` con resultado `EXITO` o `FALLO` (`registrarEnTransaccionPropia`, sin transacción abierta).
- Respuesta 200 con `{ invitacionEnviada }`.
- Si la persona acepta entre la comprobación y el envío, el enlace nuevo no sirve: la aceptación exige que siga pendiente.
- Actualizar `docs/api/openapi.json` en esta misma UT (`OpenApiContractTest`). Los nombres de los métodos de controller son únicos entre todos los controllers; si no, springdoc renombra operaciones que ya existían.

**Archivos esperados (crear/tocar):**

- `projects/backend/src/main/java/com/example/backend/features/usuarios/controller/UsuarioController.java`
- `projects/backend/src/main/java/com/example/backend/features/usuarios/service/UsuarioAdminService.java`
- `projects/backend/src/main/java/com/example/backend/features/usuarios/service/LimiteReenvios.java`
- `projects/backend/src/main/java/com/example/backend/features/usuarios/UsuarioErrorCodes.java`
- `projects/backend/src/main/resources/application.properties`
- `projects/backend/src/test/java/com/example/backend/features/usuarios/ReenvioInvitacionControllerTest.java`
- `docs/errors/usuarios/ERRORES_USUARIOS.md`
- `docs/errors/README.md`
- `docs/api/openapi.json`

## Seguridad

API4/API6: límite propio para no inundar un buzón. A09: cada reenvío queda registrado.

## Fuera de alcance (no tocar)

- Aceptar la invitación.

## Resultado esperado

El administrador puede reenviar dentro del límite.

## Validación

1. Reenvío válido: correo nuevo, el token anterior deja de existir y hay evento.
2. Cuenta inactiva, ya aceptada o cuarto reenvío en la hora: 409 sin correo. Código inexistente: 404.
3. Acceso denegado: 401, 403 por rol y 403 sin CSRF.
