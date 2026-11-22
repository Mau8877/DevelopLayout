# UT-HUI-BASE-BE-01 — Migración de tablas de autenticación con RLS

| Campo | Valor |
|-------|-------|
| **Código** | `UT-HUI-BASE-BE-01` |
| **Nombre** | Migración de tablas de autenticación con RLS |
| **Historia** | HU-INICIAL-Base-BE |
| **Depende de** | — |

## Antes de implementar

Leer `backend/PERSISTENCIA_BD_BACKEND.md`, `backend/BASE_DE_DATOS_PRIVADA_RLS.md` y `.claude/rules/SEGURIDAD_OWASP.md`.

## Solicitud de negocio

Sin tablas de usuarios y roles no hay dónde guardar quién puede entrar.

## Objetivo

Quedan creadas `usuarios`, `roles` y `usuarios_roles` con RLS activado y forzado.

## Alcance

- Migración `V{timestamp}__create_auth_tables.sql` (formato de timestamp de la rule, no secuencial).
- Columnas de `BaseModel` en las tres tablas: `id BIGINT IDENTITY` (PK), `codigo_usuario` / `codigo_rol` / `codigo_usuario_rol` (`UUID NOT NULL UNIQUE`), `created_at`, `updated_at`, `deleted_at`.
- `usuarios`: `email` (índice único parcial `WHERE deleted_at IS NULL`, comparado en minúsculas), `password_hash`, `estado` (`ACTIVO`/`INACTIVO`, `VARCHAR`).
- `roles`: `nombre` (único), `descripcion`, `is_active`; siembra `ADMIN` y `USUARIO`. Marcada `-- RLS-OPT-OUT: catálogo de roles, sin datos sensibles`.
- `usuarios_roles`: FK `usuario_id` y `rol_id` con índice cada una, `is_active`.
- RLS `ENABLE` + `FORCE` en `usuarios` y `usuarios_roles`. Política de `usuarios`: `codigo_usuario = current_setting('app.current_user', true)::uuid OR lower(email) = lower(current_setting('app.login_email', true))`. Política de `usuarios_roles`: por `app.current_user`. Ambas con `WITH CHECK`.
- Fila en `docs/seguridad/RLS_EXCEPCIONES.md` por `roles`.

**Archivos esperados (crear/tocar):**

- `projects/backend/src/main/resources/db/migration/V{timestamp}__create_auth_tables.sql`
- `docs/seguridad/RLS_EXCEPCIONES.md`

## Fuera de alcance (no tocar)

- Models Java, repositorios y enums (UT-HUI-BASE-BE-03).
- Rol `app_runtime` y sus `GRANT` (UT-HUI-BASE-BE-02).
- Usuario administrador (UT-HUI-BASE-BE-04).
- Cualquier tabla de refresh tokens: viven en Redis, no en Postgres.

## Resultado esperado

Flyway aplica la migración en una base vacía y las tres tablas existen con RLS forzado.

## Validación

1. `./mvnw test` con Flyway sobre Testcontainers: la migración corre sin error.
2. `SELECT relrowsecurity, relforcerowsecurity FROM pg_class WHERE relname IN ('usuarios','usuarios_roles')` devuelve `t, t` en ambas.
3. `SELECT nombre FROM roles` devuelve `ADMIN` y `USUARIO`.

## Seguridad

A01/A02: RLS forzado y sin política `USING (true)`; sin datos por defecto sensibles en la migración (no se siembra ninguna contraseña).
