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
ALTER TABLE orders.pedido ENABLE ROW LEVEL SECURITY;
ALTER TABLE orders.pedido FORCE ROW LEVEL SECURITY;   -- aplica también al dueño de la tabla

CREATE POLICY pedido_aislamiento ON orders.pedido
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
migración inicial del schema `authentication`; cada schema nuevo concede
su propio `USAGE` y sus privilegios por defecto en la migración que lo crea
(ver [SCHEMAS_BD_BACKEND.md](SCHEMAS_BD_BACKEND.md)).

## 4. Contexto del usuario en cada transacción

El `service` (dentro de `@Transactional`) fija el usuario autenticado antes
de tocar tablas con RLS, con alcance de transacción (`is_local = true`, así
no se filtra entre requests que reusan la misma conexión del pool):

```sql
SELECT set_config('app.current_user', :codigo, true);
```

Está implementado **una sola vez** en `security/RlsContext.java`, que exige una
transacción activa y ofrece `usarCorreoDeLogin` (`app.login_email`),
`usarRefreshSesion` (`app.refresh_sid`, solo con la firma del token ya
validada), `usarUsuario` (`app.current_user`) y
`usarAdmin` (`app.is_admin`).
Ningún service llama a `set_config` por su cuenta. La única excepción es el
alta del administrador inicial, que usa la conexión dueña (ver
`docs/seguridad/RLS_EXCEPCIONES.md`).

### Contexto de administrador

- `usarAdmin()` solo se llama desde un service cuyo método exige el rol con
  `@PreAuthorize`, **antes** de tocar datos.
- Con él se ven y revocan sesiones, se lee la bitácora y se administran usuarios:
  leerlos, darlos de alta, cambiar su estado y asignar roles al crearlos.
- **Dale al administrador la política más estrecha posible** (`FOR SELECT` si
  solo lee, y `GRANT` por columnas si no debe cambiar todo).
- Sin contexto de administrador, cada usuario **lee** su fila y sus roles; solo
  puede actualizar su propia fila y no puede insertarse roles. `app.login_email`
  es **de solo lectura**.
- `app_runtime` no borra usuarios ni asignaciones, no cambia correos y, por un
  trigger, solo el propio usuario cambia su contraseña.
- **Pendiente:** el administrador no modifica asignaciones ya creadas (llegará
  con el cambio de roles).

## 5. Excepciones (`RLS-OPT-OUT`)

Una tabla puede quedar sin RLS **solo** si es de datos no sensibles y
compartidos por todos (catálogos: países, estados, roles base) o tabla
técnica (`flyway_schema_history`). Se documenta así:

1. Comentario en la migración: `-- RLS-OPT-OUT: <razón concreta>`.
2. Fila en `docs/seguridad/RLS_EXCEPCIONES.md` (tabla, razón, fecha, quién).
3. El rol `app_runtime` tiene en esa tabla solo `SELECT` (nunca escritura)
   salvo que la UT justifique lo contrario.

"No me dio tiempo" o "es más fácil" **no** son razones válidas.

## Migraciones que modifican datos

Con `FORCE ROW LEVEL SECURITY`, un `UPDATE`/`DELETE` de una migración ve **0
filas sin avisar** si el dueño no es superusuario. Dentro de la misma migración:
`ALTER TABLE ... NO FORCE ROW LEVEL SECURITY`, el cambio y de nuevo `FORCE`. Se
prueba con un dueño sin superusuario (`MigracionRefreshFirmadoTest`).

## 6. Tests

- Una función `SECURITY DEFINER` sobre una tabla con `FORCE ROW LEVEL SECURITY`
  se prueba también con un **dueño sin superusuario** (bases administradas): el
  superusuario ignora RLS y esconde políticas faltantes. La purga de la
  bitácora borraba 0 filas sin avisar (`PurgaDuenoSinSuperusuarioTest`).
- Un test de integración con Testcontainers por tabla con RLS: usuario A no
  ve/modifica filas de usuario B, y sin contexto no se ve nada.
- Los tests deben conectarse con el rol `app_runtime` (no el superusuario),
  o RLS no se estaría probando.
