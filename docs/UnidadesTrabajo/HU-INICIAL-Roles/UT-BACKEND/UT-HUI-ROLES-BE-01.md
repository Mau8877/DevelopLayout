# UT-HUI-ROLES-BE-01 — Permiso del administrador para cambiar asignaciones

| Campo | Valor |
|-------|-------|
| **Código** | `UT-HUI-ROLES-BE-01` |
| **Nombre** | Permiso del administrador para cambiar asignaciones |
| **Historia** | HU-INICIAL-Roles-BE |
| **Depende de** | — (con HU-INICIAL-Usuarios implementada) |

## Antes de implementar

BASE_DE_DATOS_PRIVADA_RLS y SCHEMAS_BD_BACKEND.

## Solicitud de negocio

Quitar y volver a dar un rol modifica una asignación existente.

## Objetivo

El administrador puede activar y desactivar asignaciones, y nada más.

## Alcance

- Migración `V{yyyyMMddHHmmss}__admin_roles.sql`: política `usuario_rol_edicion_admin` `FOR UPDATE` con `app.is_admin`; en `usuario_rol`, `REVOKE UPDATE` a `app_runtime` y `GRANT UPDATE (is_active, updated_at, deleted_at)`.
- `UsuarioRol`: `activar()` y `desactivar()`; `usuario` y `rol` con `updatable = false`.
- Quitar un rol **no borra la fila**: queda `is_active = false`, y volver a darlo reactiva la misma fila (el índice único es por usuario y rol).
- Actualizar `RLS_EXCEPCIONES.md`, el diagrama (si cambia lo que muestra) y la tabla de estado de SCHEMAS_BD_BACKEND.

**Archivos esperados (crear/tocar):**

- `projects/backend/src/main/resources/db/migration/V{yyyyMMddHHmmss}__admin_roles.sql`
- `projects/backend/src/main/java/com/example/backend/features/usuarios/model/UsuarioRol.java`
- `projects/backend/src/test/java/com/example/backend/features/usuarios/RlsAdminRolesTest.java`
- `docs/BASE_DE_DATOS.mermaid`
- `docs/seguridad/RLS_EXCEPCIONES.md`
- `.claude/rules/backend/SCHEMAS_BD_BACKEND.md`

## Seguridad

A01: sin el contexto de administrador no se cambia ninguna asignación, ni la propia; ni siquiera él puede reapuntarla a otro usuario o rol.

## Fuera de alcance (no tocar)

- Permisos sobre el catálogo `rol` (sigue de solo lectura).

## Resultado esperado

La base admite el cambio de roles y nada más.

## Validación

1. Con `app.is_admin` se puede cambiar `is_active`.
2. Acceso denegado: con `app.current_user` no se actualiza ninguna fila; cambiar `usuario_id` o `rol_id`, o borrar, falla también como administrador.
3. `./mvnw test` en verde.
