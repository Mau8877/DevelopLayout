# UT-HUI-BASE-BE-02 — Rol de base de datos app_runtime y configuración del datasource

| Campo | Valor |
|-------|-------|
| **Código** | `UT-HUI-BASE-BE-02` |
| **Nombre** | Rol de base de datos app_runtime y configuración del datasource |
| **Historia** | HU-INICIAL-Base-BE |
| **Depende de** | UT-HUI-BASE-BE-01 |

## Antes de implementar

Leer `backend/BASE_DE_DATOS_PRIVADA_RLS.md` (sección 3), `INFRAESTRUCTURA` en `docs/INFRAESTRUCTURA.md` y `.claude/rules/SEGURIDAD_OWASP.md`.

## Solicitud de negocio

Si la app se conecta como superusuario, RLS no protege nada.

## Objetivo

La aplicación corre con el rol `app_runtime`, sin `SUPERUSER` ni `BYPASSRLS`, y Flyway con el rol dueño.

## Alcance

- Migración `V{timestamp}__create_app_runtime_role.sql`: `CREATE ROLE app_runtime LOGIN` con contraseña tomada de una variable de Flyway (`${APP_RUNTIME_PASSWORD}`, vía `spring.flyway.placeholders`), `NOSUPERUSER NOBYPASSRLS NOCREATEDB NOCREATEROLE`, `GRANT SELECT,INSERT,UPDATE,DELETE` sobre `usuarios` y `usuarios_roles`, solo `SELECT` sobre `roles`, `GRANT USAGE` en secuencias del schema `authentication`, y `ALTER DEFAULT PRIVILEGES IN SCHEMA authentication` para tablas futuras.
- Variables nuevas `APP_RUNTIME_PASSWORD` en `.env.example` raíz y `projects/backend/.env.example` (placeholder `CAMBIAME`), y `SPRING_DATASOURCE_USERNAME=app_runtime` en `docker-compose.yml` y `docker-compose.prod.yml`; Flyway sigue con `SPRING_FLYWAY_USER`.
- Sin valor por defecto de la contraseña en `application.properties`.
- Permisos sobre las tablas nuevas: `sesiones` con `SELECT, INSERT, UPDATE` (sin `DELETE`: las sesiones se marcan `REVOCADA`/`EXPIRADA`, no se borran); `bitacora` solo con `INSERT` y `SELECT`; `EXECUTE` sobre `purgar_bitacora`. Los `ALTER DEFAULT PRIVILEGES` de la UT no otorgan `DELETE` ni `UPDATE` por defecto.
- En la misma migración: `GRANT USAGE ON SCHEMA authentication TO app_runtime`, `ALTER DEFAULT PRIVILEGES IN SCHEMA authentication` (los privilegios por defecto ya se definen por schema), `REVOKE CREATE ON SCHEMA public FROM PUBLIC` y `app_runtime` sin `CREATE` en ningún schema. Los `GRANT` sobre tablas y la función usan nombres calificados (`authentication.usuarios`, `authentication.purgar_bitacora`).

**Archivos esperados (crear/tocar):**

- `projects/backend/src/main/resources/db/migration/V{timestamp}__create_app_runtime_role.sql`
- `docker-compose.yml`
- `docker-compose.prod.yml`
- `.env.example`
- `projects/backend/.env.example`
- `projects/backend/src/main/resources/application.properties`

## Fuera de alcance (no tocar)

- Crear las tablas (UT-HUI-BASE-BE-01).
- El test que prueba RLS (UT-HUI-BASE-BE-05).
- Cualquier cambio de contraseñas de Postgres o Redis ya existentes.

## Resultado esperado

Con el stack arriba, `SELECT current_user` desde la app devuelve `app_runtime` y la app no puede hacer DDL.

## Validación

1. `docker compose up` y revisar el log del backend: Flyway migra con el dueño y la app arranca.
2. `psql -U app_runtime -c "CREATE TABLE x(id int)"` falla por permisos.
3. `SELECT rolsuper, rolbypassrls FROM pg_roles WHERE rolname='app_runtime'` devuelve `f, f`.
4. `psql -U app_runtime -c "DELETE FROM bitacora"` y `"UPDATE bitacora SET accion='x'"` fallan.
5. `psql -U app_runtime -c "CREATE TABLE authentication.x(id int)"` falla por permisos.

## Seguridad

A02/A04: contraseña solo por variable de entorno; mínimo privilegio para el usuario de runtime.
