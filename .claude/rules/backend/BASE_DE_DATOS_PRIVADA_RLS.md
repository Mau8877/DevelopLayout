---
globs: projects/backend/**/*
---

# Base de datos privada + Row-Level Security (RLS)

Complementa a [PERSISTENCIA_BD_BACKEND.md](PERSISTENCIA_BD_BACKEND.md) y a
[SEGURIDAD_OWASP.md](../SEGURIDAD_OWASP.md) (A01).

## 1. La base es siempre privada

- Postgres solo es alcanzable desde la red interna de Docker (backend). En
  desarrollo se publica únicamente en `127.0.0.1`; en producción **no se
  publica ningún puerto**.
- Ningún cliente (frontend, mobile) habla con la base: todo pasa por la API.
- Las credenciales salen del `.env`, sin valor por defecto.

## 2. RLS activado por defecto en TODA tabla

Toda tabla nueva se crea con RLS **activado y forzado**, y con al menos una
política. Es el default; la excepción es la que se justifica.

```sql
ALTER TABLE pedidos ENABLE ROW LEVEL SECURITY;
ALTER TABLE pedidos FORCE ROW LEVEL SECURITY;   -- aplica también al dueño de la tabla

CREATE POLICY pedidos_aislamiento ON pedidos
    USING (usuario_codigo = current_setting('app.current_user', true)::uuid)
    WITH CHECK (usuario_codigo = current_setting('app.current_user', true)::uuid);
```

- Sin `app.current_user` seteado, `current_setting(..., true)` devuelve `NULL`
  → la política no coincide con ninguna fila → **falla cerrado** (no se ve
  ni se escribe nada). Nunca definir una política `USING (true)` "para
  salir del paso".
- Las políticas se escriben **en la misma migración Flyway** que crea la
  tabla (no "después"). Una migración que crea una tabla sin RLS ni
  `RLS-OPT-OUT` no se mergea.

## 3. Roles de base de datos (sin esto RLS no protege nada)

> ! Un superusuario y cualquier rol con `BYPASSRLS` **ignoran RLS**. El
> ! `POSTGRES_USER` del contenedor es superusuario, así que la app **no debe
> ! conectarse con él** en runtime.

| Rol | Uso | Privilegios |
| --- | --- | --- |
| Dueño del esquema (`POSTGRES_USER`) | Solo Flyway (`SPRING_FLYWAY_USER`) | DDL; dueño de las tablas |
| `app_runtime` | La aplicación en runtime (`SPRING_DATASOURCE_USERNAME`) | Solo `SELECT/INSERT/UPDATE/DELETE` sobre las tablas necesarias; **sin** DDL, **sin** `BYPASSRLS`, **sin** `SUPERUSER` |

`application.properties` ya separa ambos usuarios
(`spring.flyway.user` vs `spring.datasource.username`). Crear el rol
`app_runtime`, sus `GRANT` y los `ALTER DEFAULT PRIVILEGES` es parte de la
migración/script inicial del esquema de autenticación.

## 4. Contexto del usuario en cada transacción

El `service` (dentro de `@Transactional`) fija el usuario autenticado antes
de tocar tablas con RLS, con alcance de transacción (`is_local = true`, así
no se filtra entre requests que reusan la misma conexión del pool):

```sql
SELECT set_config('app.current_user', :codigo, true);
```

Esto se implementa **una sola vez** (un componente en `security/`, no
repetido por service) cuando se implemente la autenticación. Hasta
entonces, ninguna tabla de negocio puede salir a producción.

## 5. Excepciones (`RLS-OPT-OUT`)

Una tabla puede quedar sin RLS **solo** si es de datos no sensibles y
compartidos por todos (catálogos: países, estados, roles base) o tabla
técnica (`flyway_schema_history`). Se documenta así:

1. Comentario en la migración: `-- RLS-OPT-OUT: <razón concreta>`.
2. Fila en `docs/seguridad/RLS_EXCEPCIONES.md` (tabla, razón, fecha, quién).
3. El rol `app_runtime` tiene en esa tabla solo `SELECT` (nunca escritura)
   salvo que la UT justifique lo contrario.

"No me dio tiempo" o "es más fácil" **no** son razones válidas.

## 6. Tests

- Un test de integración con Testcontainers por tabla con RLS: usuario A no
  ve/modifica filas de usuario B, y sin contexto no se ve nada.
- Los tests deben conectarse con el rol `app_runtime` (no el superusuario),
  o RLS no se estaría probando.
