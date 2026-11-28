# Historia de usuario

| Id | Nombre corto de HU | Prioridad | PHU | Estado |
| --- | --- | --- | --- | --- |
| HU-INICIAL-USUARIOS | Administrar usuarios | Alta |  | Hecha |

**Como:** administrador
**Quiero:** ver, invitar, desactivar y reactivar usuarios
**Para:** decidir quién puede entrar al sistema sin conocer ni repartir contraseñas

**Descripción:**
Desde la web, un `SUPER_ADMIN` ve la lista de usuarios con su correo, sus roles y su estado, y puede buscarlos y filtrarlos. Para dar de alta a alguien indica su correo y uno o más roles: el sistema crea la cuenta y envía por correo una invitación válida 24 horas con la que esa persona elige su contraseña. El administrador nunca ve ni elige contraseñas. Si la invitación venció o no llegó, puede reenviarla. Puede desactivar a un usuario, lo que le cierra de inmediato todas las sesiones y le impide entrar, y reactivarlo después. Los usuarios no se eliminan. Si algo no se puede hacer (correo ya usado, intento de desactivarse a sí mismo, invitación ya aceptada), el sistema lo rechaza explicando por qué.

La función es solo web. Las personas invitadas pueden usar después la web y la app.

Depende de [HU-INICIAL-Base](HU-INICIAL-Base.md), de [HU-INICIAL-Login](HU-INICIAL-Login.md), de [HU-INICIAL-OlvidePassword](HU-INICIAL-OlvidePassword.md) (enlace de un solo uso y envío de correo), de [HU-INICIAL-SesionesAdmin](HU-INICIAL-SesionesAdmin.md) (cierre de sesiones por el administrador) y de [HU-INICIAL-Bitacora](HU-INICIAL-Bitacora.md). Cambiar los roles de un usuario ya creado es [HU-INICIAL-Roles](HU-INICIAL-Roles.md).

---

## Conversación/Reglas

**Flujo general (alta)**

1. El administrador abre «Usuarios» y elige crear uno.
2. Indica el correo y al menos un rol.
3. El sistema crea la cuenta con la invitación pendiente, envía el correo e informa si pudo enviarlo.
4. La persona abre el enlace, elige su contraseña y queda en el login.
5. La invitación pasa a aceptada.

**Reglas de negocio**

- Cada cuenta se muestra como **Activa** o **Inactiva** y, aparte, indica si la **invitación está pendiente** (la persona todavía no eligió su contraseña).
- Mientras la invitación está pendiente no hay ninguna contraseña con la que se pueda iniciar sesión.
- La invitación es de un solo uso y vence a las 24 horas. Reenviarla invalida la anterior. Como máximo 3 reenvíos por usuario y hora.
- La contraseña que elige la persona cumple la misma política que en [HU-INICIAL-Base](HU-INICIAL-Base.md).
- Si la persona invitada usa «Olvidé mi contraseña» y define una contraseña por ese camino, también demuestra que controla el correo: la invitación queda aceptada y su enlace deja de servir.
- Si el correo de invitación no se puede enviar, la cuenta queda creada; el administrador se entera y puede reenviar.
- El correo identifica a la cuenta: es único sin distinguir mayúsculas, también frente a cuentas inactivas (en ese caso se reactiva, no se crea otra). No se puede cambiar.
- Al crear una cuenta se asigna al menos uno de los roles existentes (`SUPER_ADMIN`, `USUARIO`). No se crean roles nuevos.
- Desactivar cierra todas las sesiones y anula los enlaces de invitación y de recuperación pendientes. Reactivar no revive sesiones ni enlaces: una cuenta con invitación pendiente necesita que se reenvíe.
- Desactivar una cuenta ya inactiva, o reactivar una activa, no es un error y no registra otro evento.
- Nadie puede desactivar su propia cuenta.
- **Siempre queda al menos un administrador que puede entrar.** Un cambio solo se ejecuta si quien lo pide sigue siendo un administrador activo en ese momento; así, dos administradores que se desactivan uno a otro a la vez no quedan los dos fuera.

---

## Criterios de aceptación

- **CA01:** Dado un `SUPER_ADMIN`, el sistema debe listar los usuarios paginados (20 por página por defecto, máximo 100) con correo, roles, estado, si la invitación está pendiente y la fecha de alta.
- **CA02:** Dado una búsqueda por parte del correo o un filtro por estado o por rol, el sistema debe devolver solo los usuarios que cumplan todo lo pedido; sin resultados, una lista vacía.
- **CA03:** Dado un correo no usado y al menos un rol existente, el sistema debe crear la cuenta con la invitación pendiente, enviar el correo con el enlace y responder 201.
- **CA04:** Dado un correo inválido, ningún rol o un rol que no existe, el sistema debe responder 400 indicando el campo, sin crear nada.
- **CA05:** Dado un correo ya usado por una cuenta activa o inactiva, sin distinguir mayúsculas, el sistema debe responder 409 y no crear otra, también si dos altas del mismo correo llegan a la vez.
- **CA06:** Dado que el correo de invitación no se puede enviar, el sistema debe conservar la cuenta creada, informar al administrador que no se envió y permitir reenviar.
- **CA07:** Dado un enlace de invitación vigente y una contraseña que cumple la política, el sistema debe guardar la contraseña, dar la invitación por aceptada e inutilizar el enlace, y la persona debe poder iniciar sesión.
- **CA08:** Dado un enlace de invitación vencido, ya usado, reemplazado o anulado, el sistema debe rechazarlo sin cambiar nada.
- **CA09:** Dado una cuenta con la invitación pendiente, cualquier intento de inicio de sesión debe fallar con el mismo mensaje que unas credenciales incorrectas.
- **CA10:** Dado un reenvío de una invitación pendiente, el sistema debe enviar un enlace nuevo e invalidar el anterior.
- **CA11:** Dado un reenvío para una cuenta inactiva o con la invitación ya aceptada, o más de 3 reenvíos del mismo usuario en una hora, el sistema debe responder 409 explicando el motivo, sin enviar nada.
- **CA12:** Dado que se desactiva una cuenta activa, el sistema debe cerrar todas sus sesiones, rechazar de inmediato sus access token, anular sus enlaces pendientes e impedir que inicie sesión.
- **CA13:** Dado que se reactiva una cuenta inactiva, el sistema debe permitirle iniciar sesión de nuevo, sin recuperar sesiones ni enlaces anteriores.
- **CA14:** Dado que se desactiva una cuenta ya inactiva o se reactiva una activa, el sistema debe responder 200 sin registrar otro evento.
- **CA15:** Dado que un administrador intenta desactivar su propia cuenta, el sistema debe responder 409 y no cambiar nada.
- **CA16:** Dados dos administradores que intentan desactivarse uno a otro al mismo tiempo, exactamente uno debe quedar activo.
- **CA17:** Dado que quien pide un cambio dejó de ser un administrador activo antes de que se ejecute, el sistema debe rechazarlo sin cambiar nada.
- **CA18:** Dado un usuario sin el rol `SUPER_ADMIN`, cualquier operación de esta función debe responder 403; sin sesión, 401; y en web, sin el token CSRF, 403. Un código de usuario inexistente responde 404.
- **CA19:** Dado un alta, un reenvío, una aceptación, una desactivación o una reactivación, el sistema debe registrarlo en la bitácora con quién lo hizo y sobre qué cuenta, sin contraseñas, enlaces ni correos.
- **CA20:** Dado una desactivación, el sistema debe pedir confirmación antes de ejecutarla y actualizar la lista después.
- **CA21:** Dado cualquier respuesta de esta función, no debe incluir hashes, contraseñas, enlaces de invitación ni identificadores internos.
- **CA22:** Dado que quien acepta una invitación elige una de las contraseñas muy usadas, el sistema debe responder 400, explicarlo y mantener la invitación pendiente.

## Seguridad

- **Datos sensibles:** correos y roles de todos los usuarios, y enlaces de invitación (que permiten tomar una cuenta).
- **Quién puede hacerlo:** solo `SUPER_ADMIN`, comprobado en el servidor y reforzado en la base de datos con permisos limitados a estas operaciones (OWASP A01, API5). La aceptación de la invitación es sin sesión: la credencial es el enlace.
- **Abuso previsible:** usar una sesión de administrador robada o ya revocada para crear cuentas o echar a los demás (CA16, CA17, CA19); inundar un buzón con reenvíos (CA11); adivinar o reutilizar invitaciones (CA08); dejar el sistema sin administradores (CA15, CA16); colar campos que el cliente no debe controlar, como el estado o la contraseña (API3). El correo que se busca no queda en direcciones web ni en logs. Quien administre puede crear otros administradores: es parte del rol y queda en la bitácora.

## Desarrollador

Pendiente de asignar.
