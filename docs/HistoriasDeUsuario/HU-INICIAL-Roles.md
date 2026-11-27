# Historia de usuario

| Id | Nombre corto de HU | Prioridad | PHU | Estado |
| --- | --- | --- | --- | --- |
| HU-INICIAL-ROLES | Asignar roles | Alta |  | Hecha |

**Como:** administrador
**Quiero:** cambiar los roles de un usuario
**Para:** darle o quitarle permisos según lo que le corresponda hacer

**Descripción:**
Desde la lista de usuarios de la web, un `SUPER_ADMIN` elige una cuenta y marca los roles que debe tener entre los existentes (`SUPER_ADMIN`, `USUARIO`). Al guardar, el sistema aplica el cambio y cierra todas las sesiones de esa persona, para que los permisos nuevos rijan de inmediato; la próxima vez que entre tendrá los roles nuevos. Un usuario siempre conserva al menos un rol. El administrador no puede quitarse su propio rol de administrador. Si el cambio no se puede hacer, el sistema lo rechaza explicando por qué y no modifica nada.

La función es solo web.

Depende de [HU-INICIAL-Usuarios](HU-INICIAL-Usuarios.md) (lista de usuarios y reglas sobre administradores).

---

## Conversación/Reglas

**Flujo general**

1. El administrador elige un usuario y abre sus roles.
2. Marca los que debe tener y confirma, sabiendo que se cerrarán las sesiones de esa persona.
3. El sistema guarda el conjunto de roles, cierra sus sesiones y registra el cambio.
4. La lista muestra los roles nuevos.

**Reglas de negocio**

- Se envía el conjunto completo de roles que debe quedar, no órdenes de agregar o quitar.
- Debe quedar al menos un rol. Solo existen `SUPER_ADMIN` y `USUARIO`; no se crean ni se editan roles.
- Se pueden cambiar los roles de cuentas activas, inactivas o con la invitación pendiente.
- Si el conjunto pedido es igual al actual, no cambia nada: no se cierran sesiones ni se registra un evento.
- Nadie puede quitarse su propio rol `SUPER_ADMIN`. Si un administrador cambia otro rol propio, también se cierran sus sesiones, incluida la actual, y tiene que entrar de nuevo.
- Vale la misma regla que en [HU-INICIAL-Usuarios](HU-INICIAL-Usuarios.md): el cambio solo se ejecuta si quien lo pide sigue siendo un administrador activo en ese momento, y siempre queda al menos un administrador que puede entrar. Esto cubre también los cruces entre quitar un rol y desactivar una cuenta.
- Volver a asignar un rol que la persona tuvo antes funciona igual que asignarlo por primera vez.

---

## Criterios de aceptación

- **CA01:** Dado un `SUPER_ADMIN` y un conjunto de roles existentes distinto del actual, el sistema debe dejar al usuario exactamente con esos roles y responder 200.
- **CA02:** Dado un cambio de roles aplicado, el sistema debe cerrar todas las sesiones del usuario, rechazar de inmediato sus access token y, en su siguiente inicio de sesión, aplicar los roles nuevos.
- **CA03:** Dado un conjunto vacío o un rol que no existe, el sistema debe responder 400 indicando el campo, sin cambiar nada.
- **CA04:** Dado un conjunto igual al actual, el sistema debe responder 200 sin cerrar sesiones ni registrar un evento.
- **CA05:** Dado que un administrador intenta quitarse su propio rol `SUPER_ADMIN`, el sistema debe responder 409 y no cambiar nada.
- **CA06:** Dados dos administradores que intentan quitarse el rol uno a otro al mismo tiempo, o que uno quita el rol mientras el otro lo desactiva, al terminar debe quedar al menos un administrador activo con su rol.
- **CA07:** Dado que quien pide el cambio dejó de ser un administrador activo antes de que se ejecute, el sistema debe rechazarlo sin cambiar nada.
- **CA08:** Dado un rol que se había quitado y se vuelve a asignar, el sistema debe aplicarlo sin error ni asignaciones duplicadas.
- **CA09:** Dado un usuario sin el rol `SUPER_ADMIN`, la operación debe responder 403; sin sesión, 401; y en web, sin el token CSRF, 403. Un código de usuario inexistente responde 404.
- **CA10:** Dado un cambio de roles, el sistema debe registrar en la bitácora quién lo hizo, sobre qué cuenta y qué roles se agregaron y se quitaron.
- **CA11:** Dado un cambio de roles, el sistema debe pedir confirmación avisando que se cerrarán las sesiones, y actualizar la lista después.

## Seguridad

- **Datos sensibles:** los roles definen qué puede hacer cada persona; cambiarlos es la operación de mayor privilegio del sistema.
- **Quién puede hacerlo:** solo `SUPER_ADMIN`, comprobado en el servidor y reforzado en la base de datos (OWASP A01, API5). La lista de roles solo se acepta en esta operación y en el alta de un usuario (API3).
- **Abuso previsible:** elevar privilegios sin ser administrador (CA09); usar una sesión revocada para cambiar roles (CA07); dejar el sistema sin administradores (CA05, CA06); conservar permisos retirados con una sesión abierta (CA02). Todo cambio queda en la bitácora (A09).

## Desarrollador

Pendiente de asignar.
