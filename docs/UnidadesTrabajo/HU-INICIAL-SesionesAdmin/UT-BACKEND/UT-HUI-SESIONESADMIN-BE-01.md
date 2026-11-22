# UT-HUI-SESIONESADMIN-BE-01 — SesionAdminService: listar y cerrar sesiones de usuarios

| Campo | Valor |
|-------|-------|
| **Código** | `UT-HUI-SESIONESADMIN-BE-01` |
| **Nombre** | SesionAdminService: listar y cerrar sesiones de usuarios |
| **Historia** | HU-INICIAL-SesionesAdmin-BE |
| **Depende de** | Refresh: SesionService.revocar (HU-INICIAL-Refresh) |

## Antes de implementar

Leer `backend/SEGURIDAD_AUTH_BACKEND.md`, `backend/BASE_DE_DATOS_PRIVADA_RLS.md`, `backend/BITACORA_BACKEND.md` y `.claude/rules/SEGURIDAD_OWASP.md`.

## Solicitud de negocio

Un administrador debe poder cortar el acceso de una cuenta comprometida.

## Objetivo

`SesionAdminService` lista y revoca sesiones de cualquier usuario, solo para `SUPER_ADMIN`.

## Alcance

- `features/auth/service/SesionAdminService.java` con `@PreAuthorize("hasRole('SUPER_ADMIN')")` en cada método: `listarPorEmail(String email, Pageable)`, `cerrar(UUID codigoSesion)`, `cerrarTodas(UUID codigoUsuario)`.
- Agregar `RlsContext.usarAdmin()` (`app.is_admin = true`), que solo se invoca desde servicios con la autorización ya verificada.
- Motivo `ADMIN`; evento `AUTH_SESION_CERRADA_ADMIN` con el actor (admin) y el `codigo` del usuario afectado en `detalle`.
- Correo exacto, sin búsqueda parcial; inexistente → lista vacía. Sesión inexistente → `NotFoundException` `ERR_AUTH_09`.
- `ERR_AUTH_09` (sesión no encontrada, 404) en `AuthErrorCodes` y su sección en `docs/errors/auth/ERRORES_AUTH.md` + índice.

**Archivos esperados (crear/tocar):**

- `projects/backend/src/main/java/com/example/backend/features/auth/service/SesionAdminService.java`
- `projects/backend/src/main/java/com/example/backend/security/RlsContext.java`
- `projects/backend/src/main/java/com/example/backend/features/auth/AuthAcciones.java`
- `projects/backend/src/main/java/com/example/backend/features/auth/AuthErrorCodes.java`
- `docs/errors/auth/ERRORES_AUTH.md`
- `docs/errors/README.md`

## Fuera de alcance (no tocar)

- Endpoints (UT 02).
- Listado general de usuarios (no existe en esta versión).

## Resultado esperado

Un admin lista y cierra sesiones ajenas; un usuario sin rol recibe acceso denegado.

## Validación

1. Test: con rol `USUARIO` lanza `AccessDeniedException`; con `SUPER_ADMIN` revoca y la clave `auth:revoked:{sid}` aparece; el evento guarda actor y afectado.

## Seguridad

A01/API5: BFLA por rol en el service, RLS con contexto de admin controlado.
