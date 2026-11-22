# UT-HUI-BASE-BE-01 — Migración de tablas de autenticación, sesiones y bitácora con RLS

| Campo | Valor |
|-------|-------|
| **Código** | `UT-HUI-BASE-BE-01` |
| **Nombre** | Migración de tablas de autenticación, sesiones y bitácora con RLS |
| **Historia** | HU-INICIAL-Base-BE |
| **Depende de** | — |

## Antes de implementar

Leer `backend/PERSISTENCIA_BD_BACKEND.md`, `backend/BASE_DE_DATOS_PRIVADA_RLS.md` y `.claude/rules/SEGURIDAD_OWASP.md`.

## Solicitud de negocio

Sin tablas de usuarios y roles no hay dónde guardar quién puede entrar.

## Objetivo

Queda creado el schema `authentication` y en él quedan creadas `usuario`, `rol`, `usuario_rol`, `sesion` y `bitacora` con RLS activado y forzado.

## Alcance

- Migración `V{timestamp}__create_auth_tables.sql` (formato de timestamp de la rule, no secuencial).
- Columnas de `BaseModel` en las tres tablas: `id BIGINT IDENTITY` (PK), `codigo_usuario` / `codigo_rol` / `codigo_usuario_rol` (`UUID NOT NULL UNIQUE`), `created_at`, `updated_at`, `deleted_at`.
- `usuario`: `email` (índice único parcial `WHERE deleted_at IS NULL`, comparado en minúsculas), `password_hash`, `estado` (`ACTIVO`/`INACTIVO`, `VARCHAR`).
- `rol`: `nombre` (único), `descripcion`, `is_active`; siembra `SUPER_ADMIN` y `USUARIO`. Marcada `-- RLS-OPT-OUT: catálogo de roles, sin datos sensibles`.
- `usuario_rol`: FK `usuario_id` y `rol_id` con índice cada una, `is_active`.
- RLS `ENABLE` + `FORCE` en `usuario` y `usuario_rol`. Política de `usuario`: `codigo_usuario = current_setting('app.current_user', true)::uuid OR lower(email) = lower(current_setting('app.login_email', true))`. Política de `usuario_rol`: por `app.current_user`. Ambas con `WITH CHECK`.
- Fila en `docs/seguridad/RLS_EXCEPCIONES.md` por `rol`.
- `sesion` (columnas de `BaseModel` con `codigo_sesion`): `usuario_id` (FK con índice), `usuario_codigo` (`UUID`, índice, para RLS), `refresh_hash` (`CHAR(64)`, único), `refresh_hash_anterior` (nullable, índice), `estado` (`ACTIVA`/`REVOCADA`/`EXPIRADA`), `tipo_cliente` (`WEB`/`MOBILE`), `ip`, `user_agent` (255), `expira_en`, `ultimo_uso_en`, `revocada_en`, `motivo_revocacion`. Nunca guarda el refresh token en claro.
- RLS de `sesion` (`ENABLE` + `FORCE`): `USING` y `WITH CHECK` con `usuario_codigo = app.current_user` o `app.is_admin = 'true'`; `USING` además permite `refresh_hash = app.refresh_hash` o `refresh_hash_anterior = app.refresh_hash` (contexto que fija el flujo de refresh/logout con el token que presenta el cliente, una sola fila).
- `bitacora` (columnas de `BaseModel` con `codigo_bitacora`): `usuario_codigo` (nullable), `accion`, `modulo`, `entidad`, `entidad_codigo` (nullable), `resultado` (`EXITO`/`FALLO`), `ip`, `user_agent`, `detalle` (`jsonb`); índices por `created_at`, `accion` y `usuario_codigo`.
- RLS de `bitacora`: política `FOR INSERT WITH CHECK (true)` (excepción justificada: el evento puede ocurrir sin usuario, p. ej. un login fallido, y la tabla es de solo inserción) y política `FOR SELECT USING (current_setting('app.is_admin', true) = 'true')`. Sin políticas de `UPDATE` ni `DELETE`.
- Función `authentication.purgar_bitacora(meses integer)` `SECURITY DEFINER` con `SET search_path = pg_catalog, authentication`, `REVOKE ALL ... FROM PUBLIC` y `GRANT EXECUTE` solo a `app_runtime` (en la UT-HUI-BASE-BE-02): elimina filas con `created_at` anterior a `now() - meses` y devuelve cuántas borró.
- Fila en `docs/seguridad/RLS_EXCEPCIONES.md` por el `INSERT` abierto de `bitacora`.
- Primera sentencia de la migración: `CREATE SCHEMA IF NOT EXISTS authentication;`. Todas las tablas, índices, políticas y funciones de esta UT se escriben calificados (`authentication.usuario`, `authentication.sesion`, ...), sin depender del `search_path`.
- Ninguna tabla queda en `public`.

**Archivos esperados (crear/tocar):**

- `projects/backend/src/main/resources/db/migration/V{timestamp}__create_auth_tables.sql`
- `docs/seguridad/RLS_EXCEPCIONES.md`

## Fuera de alcance (no tocar)

- Models Java, repositorios y enums (UT-HUI-BASE-BE-03).
- Rol `app_runtime` y sus `GRANT` (UT-HUI-BASE-BE-02).
- Usuario administrador (UT-HUI-BASE-BE-04).
- Una tabla separada de refresh tokens: el refresh token vive en `sesion` (UT de Base BE-01 y Login).

## Resultado esperado

Flyway aplica la migración en una base vacía y las tres tablas existen con RLS forzado.

## Validación

1. `./mvnw test` con Flyway sobre Testcontainers: la migración corre sin error.
2. `SELECT relrowsecurity, relforcerowsecurity FROM pg_class WHERE relnamespace = 'authentication'::regnamespace AND relname IN ('usuario','usuario_rol')` devuelve `t, t` en ambas.
3. `SELECT nombre FROM authentication.rol` devuelve `SUPER_ADMIN` y `USUARIO`.
4. `INSERT` en `bitacora` con rol `app_runtime` funciona; `UPDATE` y `DELETE` fallan por permisos; `SELECT` sin `app.is_admin` no devuelve filas.
5. `SELECT authentication.purgar_bitacora(12)` borra solo filas anteriores a 12 meses.
6. `SELECT table_name FROM information_schema.tables WHERE table_schema = 'authentication'` lista `usuario`, `rol`, `usuario_rol`, `sesion` y `bitacora`; con `table_schema = 'public'` solo aparece `flyway_schema_history`.

## Seguridad

A01/A02: RLS forzado y sin política `USING (true)`; sin datos por defecto sensibles en la migración (no se siembra ninguna contraseña).
