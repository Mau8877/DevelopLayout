# UT-HUI-USUARIOS-BE-10 — Desactivar y reactivar usuarios

| Campo | Valor |
|-------|-------|
| **Código** | `UT-HUI-USUARIOS-BE-10` |
| **Nombre** | Desactivar y reactivar usuarios |
| **Historia** | HU-INICIAL-Usuarios-BE |
| **Depende de** | 02, 04, 05 |

## Antes de implementar

SEGURIDAD_AUTH_BACKEND, BITACORA_BACKEND y `SesionAdminService`.

## Solicitud de negocio

El administrador decide quién puede entrar.

## Objetivo

`POST /api/v1/usuarios/{codigo}/desactivar` y `/reactivar`.

## Alcance

**Desactivar**, en una transacción: guardia; no es la propia cuenta (`ERR_US_04`, 409); la cuenta existe (`ERR_US_01`); `bloquearParaCambio` (un login en curso termina antes y su sesión se revoca acá, o espera y ve la cuenta inactiva); si ya está inactiva, 200 sin cambios ni evento; estado `INACTIVO`; `sesionService.revocarTodas(codigo, ADMIN)`; anular los tokens de invitación y de recuperación; bitácora `USUARIOS_USUARIO_DESACTIVADO`.

**Reactivar:** guardia, bloqueo y 200 sin evento si ya está activa; estado `ACTIVO`; **anular otra vez los dos tokens** (una recuperación en curso pudo emitir uno justo cuando se desactivaba) y bitácora `USUARIOS_USUARIO_REACTIVADO`. No toca sesiones ni `invitacionPendiente`.

Las dos responden 200 con `UsuarioResponse`. `Usuario` gana `desactivar()` y `reactivar()`. Si Redis falla, no se confirma nada.
- Actualizar `docs/api/openapi.json` en esta misma UT (`OpenApiContractTest`). Los nombres de los métodos de controller son únicos entre todos los controllers; si no, springdoc renombra operaciones que ya existían.

**Archivos esperados (crear/tocar):**

- `projects/backend/src/main/java/com/example/backend/features/usuarios/controller/UsuarioController.java`
- `projects/backend/src/main/java/com/example/backend/features/usuarios/service/UsuarioAdminService.java`
- `projects/backend/src/main/java/com/example/backend/features/usuarios/model/Usuario.java`
- `projects/backend/src/main/java/com/example/backend/features/usuarios/UsuarioErrorCodes.java`
- `projects/backend/src/main/java/com/example/backend/features/usuarios/UsuarioAcciones.java`
- `projects/backend/src/test/java/com/example/backend/features/usuarios/EstadoUsuarioControllerTest.java`
- `projects/backend/src/test/java/com/example/backend/features/usuarios/AdministradoresConcurrenciaTest.java`
- `docs/errors/usuarios/ERRORES_USUARIOS.md`
- `docs/errors/README.md`
- `docs/api/openapi.json`

## Seguridad

A01: nadie se desactiva a sí mismo y siempre queda un administrador (guardia). Las sesiones caen de inmediato (lista de revocadas) y Redis caído falla cerrado.

## Fuera de alcance (no tocar)

- Eliminar usuarios.
- Cambiar roles.

## Resultado esperado

Desactivar corta el acceso al instante; reactivar lo devuelve sin revivir nada.

## Validación

1. Tras desactivar: el access token da 401, el refresh falla, el login falla y los enlaces pendientes no sirven.
2. Tras reactivar: puede iniciar sesión; las sesiones y enlaces anteriores siguen sin servir.
3. Repetir cualquiera de las dos: 200 sin evento nuevo. La propia cuenta: 409. Código inexistente: 404.
4. **Dos administradores que se desactivan a la vez: exactamente uno queda activo.** Comprobar que la prueba falla si se quita el bloqueo de la guardia.
5. Acceso denegado: 401, 403 por rol, 403 sin CSRF y 401 con la sesión del actor revocada, sin cambios.
