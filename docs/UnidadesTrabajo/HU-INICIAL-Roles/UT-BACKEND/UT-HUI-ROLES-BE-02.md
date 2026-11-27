# UT-HUI-ROLES-BE-02 — Cambiar los roles de un usuario

| Campo | Valor |
|-------|-------|
| **Código** | `UT-HUI-ROLES-BE-02` |
| **Nombre** | Cambiar los roles de un usuario |
| **Historia** | HU-INICIAL-Roles-BE |
| **Depende de** | 01 |

## Antes de implementar

SEGURIDAD_AUTH_BACKEND, EXCEPCIONES_BACKEND, BITACORA_BACKEND y `GuardiaAdministradores`.

## Solicitud de negocio

El administrador ajusta los permisos de una persona.

## Objetivo

`PUT /api/v1/usuarios/{codigo}/roles`.

## Alcance

- `CambiarRolesRequest` (`ignoreUnknown = false`): `roles`, al menos uno; los repetidos cuentan una vez. Es `PUT` porque reemplaza el conjunto completo.
- `@PreAuthorize`, y en una transacción: guardia; la cuenta existe (`ERR_US_01`); roles existentes y activos (`ERR_US_03`, 400); `bloquearParaCambio` sobre la cuenta (un login en curso espera y ve los roles nuevos).
- Conjunto igual al actual: 200 sin cambios, sin revocar y sin evento.
- Quitarse el `SUPER_ADMIN` propio: `ERR_US_07` (409).
- Activar, crear o desactivar asignaciones; `sesionService.revocarTodas(codigo, ADMIN)` (si es la cuenta propia, incluye la actual); bitácora `USUARIOS_ROLES_EDITADOS` con las listas `agregados` y `quitados`.
- Funciona con cuentas activas, inactivas y pendientes. Responde 200 con `UsuarioResponse`.
- Actualizar `docs/api/openapi.json`; nombre de método único entre controllers.

**Archivos esperados (crear/tocar):**

- `projects/backend/src/main/java/com/example/backend/features/usuarios/controller/UsuarioController.java`
- `projects/backend/src/main/java/com/example/backend/features/usuarios/service/UsuarioAdminService.java`
- `projects/backend/src/main/java/com/example/backend/features/usuarios/dto/CambiarRolesRequest.java`
- `projects/backend/src/main/java/com/example/backend/features/usuarios/repository/UsuarioRolRepository.java`
- `projects/backend/src/main/java/com/example/backend/features/usuarios/UsuarioErrorCodes.java`
- `projects/backend/src/main/java/com/example/backend/features/usuarios/UsuarioAcciones.java`
- `projects/backend/src/test/java/com/example/backend/features/usuarios/CambiarRolesControllerTest.java`
- `docs/errors/usuarios/ERRORES_USUARIOS.md`
- `docs/errors/README.md`
- `docs/api/openapi.json`

## Seguridad

Es la operación de mayor privilegio: guardia, RLS, no quitarse el propio rol y revocación inmediata para que no sobrevivan permisos retirados.

## Fuera de alcance (no tocar)

- Crear o editar roles.
- Cambiar el estado de la cuenta.

## Resultado esperado

Los roles cambian y rigen desde ese instante.

## Validación

1. El usuario queda exactamente con lo pedido; sus access token dan 401 y al volver a entrar tiene los roles nuevos.
2. Mismo conjunto: 200 sin revocar ni registrar. Vacío o inexistente: 400 sin cambios. Quitarse el propio `SUPER_ADMIN`: 409.
3. Quitar y volver a dar un rol no duplica filas.
4. Funciona en cuentas inactivas y pendientes.
5. Acceso denegado: 401, 403 por rol, 403 sin CSRF, 404, y 401 con la sesión del actor revocada.
