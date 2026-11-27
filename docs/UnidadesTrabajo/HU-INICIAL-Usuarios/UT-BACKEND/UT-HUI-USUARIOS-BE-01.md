# UT-HUI-USUARIOS-BE-01 — Base de datos: invitación pendiente y permisos del administrador

| Campo | Valor |
|-------|-------|
| **Código** | `UT-HUI-USUARIOS-BE-01` |
| **Nombre** | Base de datos: invitación pendiente y permisos del administrador |
| **Historia** | HU-INICIAL-Usuarios-BE |
| **Depende de** | — |

## Antes de implementar

BASE_DE_DATOS_PRIVADA_RLS, SCHEMAS_BD_BACKEND, PERSISTENCIA_BD_BACKEND y `docs/seguridad/RLS_EXCEPCIONES.md`.

## Solicitud de negocio

La base tiene que permitir lo que hará el administrador y nada más.

## Objetivo

Columna `invitacion_pendiente` y políticas y permisos limitados para administrar usuarios.

## Alcance

Migración nueva `V{yyyyMMddHHmmss}__admin_usuarios.sql`:

1. `ALTER TABLE authentication.usuario ADD COLUMN invitacion_pendiente BOOLEAN NOT NULL DEFAULT false`. No actualiza filas, así que no hace falta `NO FORCE`.
2. **`usuario`:** reemplazar `usuario_aislamiento` (hoy `FOR ALL`) por:
   - `usuario_lectura_propia` `FOR SELECT` (`app.current_user` o `app.login_email`);
   - `usuario_edicion_propia` `FOR UPDATE` solo con `app.current_user` (el correo del login deja de permitir escribir);
   - `usuario_alta_admin` `FOR INSERT` y `usuario_edicion_admin` `FOR UPDATE` con `app.is_admin`.
3. **`usuario_rol`:** reemplazar `usuario_rol_aislamiento` (hoy `FOR ALL`: con su propio contexto, un usuario podría insertarse roles) por `usuario_rol_lectura_propia` `FOR SELECT`, y agregar `usuario_rol_lectura_admin` `FOR SELECT` y `usuario_rol_alta_admin` `FOR INSERT` con `app.is_admin`.
4. **Alta inicial:** `AdminInicialInitializer` escribe con el dueño y `app.current_user`. Para que siga funcionando con un dueño sin superusuario, crear `FOR INSERT TO CURRENT_USER` en `usuario` y `usuario_rol` con la misma condición de fila propia que usa hoy.
5. **Permisos de `app_runtime`:** `REVOKE DELETE` en `usuario` y `usuario_rol` (no se eliminan). En `usuario`, `REVOKE UPDATE` y `GRANT UPDATE (password_hash, estado, invitacion_pendiente, updated_at, deleted_at)`: el correo no se puede cambiar.
6. **Contraseña ajena:** trigger `BEFORE UPDATE` en `usuario` que rechaza un cambio de `password_hash` hecho por `app_runtime` cuando `app.current_user` no es el `codigo_usuario` de la fila. Ni el contexto de administrador cambia contraseñas.

Modelo `Usuario`: campo `invitacionPendiente`; `email` con `updatable = false` (si no, Hibernate lo incluye en cada `UPDATE` y la base lo rechaza).

Documentar las políticas en `RLS_EXCEPCIONES.md`, actualizar el apartado de contexto de administrador de la rule de RLS (deja de ser solo lectura), la tabla de estado de SCHEMAS_BD_BACKEND y el diagrama.

La prueba `contextoDeAdmin_leeUsuariosPeroNoPuedeModificarlos` de `SesionAdminControllerTest` describe el permiso anterior: se reemplaza por las pruebas nuevas.

**Archivos esperados (crear/tocar):**

- `projects/backend/src/main/resources/db/migration/V{yyyyMMddHHmmss}__admin_usuarios.sql`
- `projects/backend/src/main/java/com/example/backend/features/usuarios/model/Usuario.java`
- `projects/backend/src/test/java/com/example/backend/features/usuarios/RlsAdminUsuariosTest.java`
- `projects/backend/src/test/java/com/example/backend/features/auth/SesionAdminControllerTest.java`
- `docs/BASE_DE_DATOS.mermaid`
- `docs/seguridad/RLS_EXCEPCIONES.md`
- `.claude/rules/backend/BASE_DE_DATOS_PRIVADA_RLS.md`
- `.claude/rules/backend/SCHEMAS_BD_BACKEND.md`

## Seguridad

A01: la base limita al administrador a leer, dar de alta y cambiar el estado, y cierra dos huecos que ya existían (escribir con el correo del login e insertarse roles propios).

## Fuera de alcance (no tocar)

- Permiso para modificar `usuario_rol` (lo agrega [HU-INICIAL-Roles](../../../HistoriasDeUsuario/HU-INICIAL-Roles.md)).
- El conteo de administradores de `AdminInicialInitializer` (ver follow-ups).

## Resultado esperado

El esquema soporta invitaciones y rechaza lo que el administrador no debe hacer.

## Validación

1. Como `app_runtime` sin contexto no se lee ni se escribe nada; con `app.is_admin` se lee, se inserta y se cambia `estado`.
2. Acceso denegado: como `app_runtime`, cambiar `email`, borrar filas o cambiar el `password_hash` de otro (también con `app.is_admin`) falla.
3. Con solo `app.login_email` no se puede actualizar; con solo `app.current_user` no se puede insertar en `usuario` ni en `usuario_rol`.
4. Con un dueño **sin** superusuario, la migración se aplica y el alta del administrador inicial funciona.
5. `./mvnw test` completo en verde (login, restablecimiento y sesiones siguen funcionando con las políticas nuevas).
