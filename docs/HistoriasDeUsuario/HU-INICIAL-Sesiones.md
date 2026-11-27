# Historia de usuario

| Id | Nombre corto de HU | Prioridad | PHU | Estado |
| --- | --- | --- | --- | --- |
| HU-INICIAL-SESIONES | Gestionar mis sesiones | Media |  | Pendiente |

**Como:** usuario con sesión iniciada (web o mobile)
**Quiero:** ver mis sesiones activas y cerrar una o todas las demás
**Para:** controlar desde dónde está abierta mi cuenta y cortar un acceso que ya no reconozco o uso

**Descripción:**
El usuario abre "Mis sesiones" y ve las sesiones activas de su cuenta con el tipo de cliente (web o mobile), un resumen del dispositivo, la IP, cuándo se creó, su última actividad y cuál es la actual. Puede cerrar una sesión puntual o cerrar todas las demás de una vez. Una sesión cerrada deja de funcionar de inmediato, también su access token. No puede ver ni cerrar sesiones de otros usuarios (el sistema responde como si no existieran). La sesión actual no se cierra desde esta pantalla: para eso está [cerrar sesión](HU-INICIAL-Logout.md).

La función es para cualquier usuario con sesión, también el administrador (que además puede [administrar las sesiones de todos](HU-INICIAL-SesionesAdmin.md)).

Depende de [HU-INICIAL-Login](HU-INICIAL-Login.md), [HU-INICIAL-Refresh](HU-INICIAL-Refresh.md) y [HU-INICIAL-Logout](HU-INICIAL-Logout.md).

---

## Conversación/Reglas

**Flujo general**

1. El usuario abre "Mis sesiones".
2. El sistema lista sus sesiones `ACTIVA` y marca la actual.
3. El usuario cierra una sesión o "cerrar todas las demás" y confirma.
4. El sistema revoca las sesiones elegidas y registra el evento.

**Reglas de negocio**

- Solo se listan sesiones del usuario autenticado y en estado `ACTIVA`.
- Nunca se expone el hash del refresh token ni el `id` interno de la fila: la sesión se identifica por su `codigo`.
- Intentar cerrar una sesión ajena o inexistente responde 404 (igual en ambos casos).
- Cerrar la sesión actual por este camino responde 400; se usa cerrar sesión.
- Cada cierre se registra en la bitácora (`AUTH_SESION_CERRADA`) con el motivo `USUARIO`.
- Se listan las sesiones activas **no vencidas**, paginadas, con la actual primero. «Cerrar todas las demás» alcanza también a las que siguen activas pero vencidas, porque su último access token podría seguir sirviendo.
- **Solo una sesión que sigue activa puede cerrar otras.** Si la sesión que pide el cierre dejó de estarlo mientras tanto, no se cierra nada. Así, dos sesiones que se cierran una a otra a la vez no terminan cerradas las dos.
- Cerrar una sesión propia que ya estaba cerrada no es un error y no registra otro evento.
- El resumen del dispositivo (navegador y sistema, o «App móvil») lo arma cada cliente a partir del user agent; es orientativo, porque el user agent se puede falsificar.
- «Cerrar todas las demás» cierra las que existen en ese momento: no impide un inicio de sesión posterior. Si alguien conoce la contraseña, hay que cambiarla.

---

## Criterios de aceptación

- **CA01:** Dado un usuario autenticado, el sistema debe listar solo sus sesiones activas no vencidas, paginadas y con la actual primero, con tipo de cliente, dispositivo, IP, fecha de creación, última actividad e indicador de sesión actual.
- **CA02:** Dado una sesión propia distinta de la actual, el sistema debe revocarla (motivo `USUARIO`), rechazar de inmediato su access token y registrar el evento.
- **CA03:** Dado un `codigo` de sesión de otro usuario o inexistente, el sistema debe responder 404 y no revelar si existe.
- **CA04:** Dado "cerrar todas las demás", el sistema debe revocar todas las sesiones activas del usuario excepto la actual, que debe seguir funcionando.
- **CA05:** Dado el `codigo` de la sesión actual, el sistema debe responder 400 con un error catalogado indicando que use cerrar sesión.
- **CA06:** Dado un request sin sesión válida responde 401, y un `DELETE` web sin `X-XSRF-TOKEN` válido responde 403.
- **CA07:** Dado el listado, la respuesta no debe incluir el hash del refresh token ni el `id` interno.
- **CA08:** Dado que la sesión que pide un cierre ya no está activa al momento de ejecutarlo, el sistema debe responder 401 sin cerrar ninguna sesión.
- **CA09:** Dadas dos sesiones del mismo usuario que intentan cerrarse una a otra al mismo tiempo, exactamente una debe quedar activa.
- **CA10:** Dado el `codigo` de una sesión propia que ya no está activa, el sistema debe responder 200 sin registrar otro evento.
- **CA11:** Dado que el usuario cierra una sesión o todas las demás desde web o mobile, el sistema debe pedir confirmación antes y actualizar el listado después.

## Seguridad

- **Datos sensibles:** IP, user agent y fechas de las sesiones propias.
- **Quién puede hacerlo:** cualquier usuario con sesión, solo sobre sus sesiones. Cada operación comprueba que la sesión es suya y RLS lo refuerza; la función **nunca** usa el contexto de administrador, tampoco si quien la usa es administrador (API1, API5).
- **Abuso previsible:** adivinar el `codigo` de sesiones ajenas (404 uniforme, CA03) y usar una sesión robada y ya revocada para cerrar la legítima (CA08, CA09). Una sesión robada **todavía activa** sí puede cerrar las demás: se resuelve cambiando la contraseña (ver [Olvidé mi contraseña](HU-INICIAL-OlvidePassword.md)).

## Desarrollador

Pendiente de asignar.
