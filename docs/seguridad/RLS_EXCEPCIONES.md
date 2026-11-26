# Tablas sin RLS (`RLS-OPT-OUT`)

Registro de las tablas que quedan **sin** Row-Level Security. Regla y
criterios en `.claude/rules/backend/BASE_DE_DATOS_PRIVADA_RLS.md`.

| Tabla | Razón | Fecha | Responsable |
| --- | --- | --- | --- |
| `flyway_schema_history` | Tabla técnica de Flyway, sin datos de negocio | — | — |
| `authentication.rol` | Catálogo de roles (`SUPER_ADMIN`, `USUARIO`), sin datos sensibles; `app_runtime` solo tiene `SELECT` | 2026-11-25 | Mau8877 |

# Políticas abiertas a propósito

| Objeto | Razón | Fecha | Responsable |
| --- | --- | --- | --- |
| `authentication.bitacora`, política `bitacora_insercion` (`FOR INSERT WITH CHECK (true)`) | El evento puede ocurrir sin usuario (p. ej. un login fallido) y la tabla es de solo inserción: `app_runtime` no tiene `UPDATE` ni `DELETE`, y `SELECT` exige `app.is_admin` | 2026-11-25 | Mau8877 |

| `authentication.usuario`, política `usuario_lectura_admin` (`FOR SELECT` con `app.is_admin`) | El administrador busca usuarios para gestionar sus sesiones. Solo lectura; el contexto lo fija `RlsContext.usarAdmin()` tras la autorización | 2026-11-26 | Mau8877 |
| `authentication.bitacora`, política `bitacora_purga_lectura` (solo el dueño, durante la purga) | Un dueño sin superusuario necesita leer las filas que purga. No aplica a `app_runtime` | 2026-11-26 | Mau8877 |

# Escrituras fuera del rol `app_runtime`

| Operación | Razón | Fecha | Responsable |
| --- | --- | --- | --- |
| Alta del `SUPER_ADMIN` inicial (`AdminInicialInitializer`) | En el arranque no hay usuario autenticado y RLS está forzado; se usa la conexión dueña (`spring.flyway.*`) y se fija `app.current_user` al código del usuario nuevo. Hoy el dueño es `POSTGRES_USER` (superusuario), que además ignora RLS | 2026-11-25 | Mau8877 |
| Función `authentication.purgar_bitacora` (`SECURITY DEFINER`) | Única vía para borrar bitácora; activa el contexto `app.purga` que habilita la política `bitacora_purga`. Solo `app_runtime` puede ejecutarla | 2026-11-25 | Mau8877 |
