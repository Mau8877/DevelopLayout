# Historia de usuario

| Id | Nombre corto de HU | Prioridad | PHU | Estado |
| --- | --- | --- | --- | --- |
| HU-INICIAL-BASE | Base de autenticación | Alta |  | Pendiente |

**Como:** equipo de desarrollo del proyecto
**Quiero:** que el sistema tenga usuarios, roles y un administrador inicial desde el primer arranque
**Para:** que el resto de las HU de autenticación (iniciar, renovar, cerrar sesión y recuperar contraseña) tengan sobre qué funcionar

**Descripción:**
Esta HU deja lista la base de datos y la configuración mínima de seguridad. Existen usuarios con varios roles posibles (`ADMIN`, `USUARIO`), un administrador inicial creado en el primer arranque a partir de variables de entorno, y la aplicación se conecta a la base con un rol de PostgreSQL sin privilegios de superusuario, para que Row-Level Security (RLS) se aplique de verdad. No hay registro público: solo el administrador inicial existe al comienzo. Si falta alguna variable obligatoria del administrador, la aplicación no arranca.

Es prerequisito de [HU-INICIAL-Login](HU-INICIAL-Login.md), [HU-INICIAL-Refresh](HU-INICIAL-Refresh.md), [HU-INICIAL-Logout](HU-INICIAL-Logout.md) y [HU-INICIAL-OlvidePassword](HU-INICIAL-OlvidePassword.md).

---

## Conversación/Reglas

**Flujo general**

1. Flyway crea las tablas de usuarios, roles y asignación de roles, y siembra los roles `ADMIN` y `USUARIO`.
2. Flyway crea el rol de base de datos `app_runtime` (sin `BYPASSRLS`) y activa RLS en las tablas de negocio según [BASE_DE_DATOS_PRIVADA_RLS](../../.claude/rules/backend/BASE_DE_DATOS_PRIVADA_RLS.md).
3. Al arrancar, si no existe ningún usuario con rol `ADMIN`, el backend crea el administrador inicial con las credenciales de entorno.
4. En arranques siguientes no hace nada (es idempotente).

**Reglas de negocio**

- Todas las tablas creadas (`usuarios`, `roles`, `usuarios_roles`) se mapean con un Model que extiende `BaseModel` (`id`, `codigo` UUID, `created_at`, `updated_at`, `deleted_at`), según [PERSISTENCIA_BD_BACKEND](../../.claude/rules/backend/PERSISTENCIA_BD_BACKEND.md). El correo es único solo entre filas no borradas (índice único parcial `WHERE deleted_at IS NULL`).
- Un usuario puede tener varios roles activos a la vez (modelo relacional `Rol` + `UsuarioRol`, ver [SEGURIDAD_AUTH_BACKEND](../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md#roles-y-permisos)).
- El identificador de acceso es el correo electrónico, único e insensible a mayúsculas.
- Contraseñas con BCrypt; nunca en texto plano ni en logs. Política: **mínimo 8 caracteres y nada más** (sin obligar mayúsculas, números ni símbolos). Máximo 72 bytes por el límite de BCrypt.
- El administrador inicial se crea desde variables de entorno (`INITIAL_ADMIN_EMAIL`, `INITIAL_ADMIN_PASSWORD`), nunca desde una migración, para no versionar una contraseña.
- No existe endpoint de registro público en esta versión.
- RLS en las tablas de autenticación: `usuarios` solo es visible para la fila cuyo `codigo` coincide con `app.current_user` o cuyo correo coincide con `app.login_email` (contexto que fija el flujo de login/recuperación, una sola fila); `usuarios_roles` por `usuario_codigo = app.current_user`; `roles` es catálogo (`RLS-OPT-OUT`, solo `SELECT` para `app_runtime`). Sin contexto no se ve ninguna fila.
- La creación del administrador inicial corre con la conexión dueña del esquema (la misma de Flyway), porque en ese momento no hay usuario autenticado; es la única escritura de runtime fuera de `app_runtime` y se documenta en `docs/seguridad/RLS_EXCEPCIONES.md`.

---

## Criterios de aceptación

- **CA01:** Dado que la base está vacía, el sistema debe crear al arrancar las tablas de usuarios, roles y asignación de roles, con los roles `ADMIN` y `USUARIO` sembrados.
- **CA02:** Dado que no existe ningún administrador y las variables de entorno están definidas, el sistema debe crear el administrador inicial con rol `ADMIN` y contraseña almacenada con BCrypt.
- **CA03:** Dado que ya existe un administrador, el sistema no debe crear ni modificar ningún usuario al arrancar.
- **CA04:** Dado que falta `INITIAL_ADMIN_EMAIL` o `INITIAL_ADMIN_PASSWORD`, o la contraseña tiene menos de 8 caracteres, y no hay administrador, el sistema debe fallar el arranque con un mensaje claro.
- **CA05:** Dado que se crean las tablas `usuarios`, `roles` y `usuarios_roles`, cada una debe tener las columnas de `BaseModel` (`id`, `codigo_*`, `created_at`, `updated_at`, `deleted_at`) y su Model debe extender `BaseModel`.
- **CA06:** Dado que se consulta `usuarios` o `usuarios_roles` sin fijar el contexto de usuario, el sistema no debe devolver ninguna fila; con `app.login_email` solo debe ver la fila de ese correo.
- **CA07:** Dado que la aplicación está en ejecución, debe conectarse a PostgreSQL con el rol `app_runtime`, que no es superusuario ni tiene `BYPASSRLS`.

## Seguridad

- Comprobar que ningún log ni respuesta contiene la contraseña del administrador (OWASP A09).
- RLS activo y verificado con un test de integración que corre como `app_runtime`.

## Desarrollador

Pendiente de asignar.
