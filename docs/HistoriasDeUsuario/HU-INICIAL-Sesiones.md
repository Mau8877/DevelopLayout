# Historia de usuario

| Id | Nombre corto de HU | Prioridad | PHU | Estado |
| --- | --- | --- | --- | --- |
| HU-INICIAL-SESIONES | Gestionar mis sesiones | Media |  | Pospuesta |

> **Pospuesta:** por ahora las sesiones solo las ve y administra el `SUPER_ADMIN` (HU-INICIAL-SesionesAdmin). Esta HU queda documentada y sin unidades de trabajo hasta que se decida abrirla a todos los usuarios.

**Como:** usuario con sesión iniciada (web o mobile)
**Quiero:** ver mis sesiones activas y cerrar una o todas las demás
**Para:** controlar desde dónde está abierta mi cuenta y cortar un acceso que ya no reconozco o uso

**Descripción:**
El usuario abre "Mis sesiones" y ve las sesiones activas de su cuenta con el tipo de cliente (web o mobile), un resumen del dispositivo, la IP, cuándo se creó, su última actividad y cuál es la actual. Puede cerrar una sesión puntual o cerrar todas las demás de una vez. Una sesión cerrada deja de funcionar de inmediato, también su access token. No puede ver ni cerrar sesiones de otros usuarios (el sistema responde como si no existieran). La sesión actual no se cierra desde esta pantalla: para eso está [cerrar sesión](HU-INICIAL-Logout.md).

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

---

## Criterios de aceptación

- **CA01:** Dado un usuario autenticado, el sistema debe listar solo sus sesiones activas con tipo de cliente, dispositivo, IP, fecha de creación, última actividad e indicador de sesión actual.
- **CA02:** Dado una sesión propia distinta de la actual, el sistema debe revocarla (motivo `USUARIO`), rechazar de inmediato su access token y registrar el evento.
- **CA03:** Dado un `codigo` de sesión de otro usuario o inexistente, el sistema debe responder 404 y no revelar si existe.
- **CA04:** Dado "cerrar todas las demás", el sistema debe revocar todas las sesiones activas del usuario excepto la actual, que debe seguir funcionando.
- **CA05:** Dado el `codigo` de la sesión actual, el sistema debe responder 400 con un error catalogado indicando que use cerrar sesión.
- **CA06:** Dado un request sin sesión válida responde 401, y un `DELETE` web sin `X-XSRF-TOKEN` válido responde 403.
- **CA07:** Dado el listado, la respuesta no debe incluir el hash del refresh token ni el `id` interno.

## Seguridad

- BOLA (API1): cada operación verifica que la sesión pertenece al usuario (y RLS lo refuerza). Sin enumeración por 404 uniforme.

## Desarrollador

Pendiente de asignar.
