# Historia de usuario

| Id | Nombre corto de HU | Prioridad | PHU | Estado |
| --- | --- | --- | --- | --- |
| HU-INICIAL-SESIONESADMIN | Administrar sesiones de usuarios | Media |  | Pendiente |

**Como:** administrador
**Quiero:** ver y cerrar las sesiones de cualquier usuario
**Para:** cortar el acceso de una cuenta comprometida o dada de baja sin esperar a que el usuario actúe

**Descripción:**
El administrador busca a un usuario por su correo exacto y ve sus sesiones activas. Puede cerrar una sesión puntual o todas las del usuario. Un usuario sin rol `SUPER_ADMIN` no puede usar estas funciones. Cada cierre se registra en la bitácora con el administrador como actor y el usuario afectado como objetivo. En esta versión no hay un listado general de usuarios: se busca por correo exacto.

Depende de [HU-INICIAL-Refresh](HU-INICIAL-Refresh.md) (reutiliza la revocación de sesiones). La gestión de sesiones por parte de cada usuario ([HU-INICIAL-Sesiones](HU-INICIAL-Sesiones.md)) está pospuesta: por ahora solo el `SUPER_ADMIN` ve y cierra sesiones.

---

## Conversación/Reglas

**Reglas de negocio**

- Solo `SUPER_ADMIN` (`@PreAuthorize` en el service).
- La búsqueda es por correo exacto, sin búsqueda parcial (evita listar usuarios por fuerza bruta).
- Motivo de revocación `SUPER_ADMIN`; el evento `AUTH_SESION_CERRADA_ADMIN` guarda el `codigo` del usuario afectado.
- Las respuestas siguen la paginación obligatoria con `pageSize` máximo 100.

---

## Criterios de aceptación

- **CA01:** Dado un administrador y un correo existente, el sistema debe listar las sesiones activas de ese usuario, paginadas.
- **CA02:** Dado un correo inexistente, el sistema debe responder con lista vacía (sin revelar nada más).
- **CA03:** Dado un administrador y el `codigo` de una sesión, el sistema debe revocarla (motivo `SUPER_ADMIN`) y rechazar de inmediato su access token.
- **CA04:** Dado un administrador y un usuario, el sistema debe poder revocar todas sus sesiones activas.
- **CA05:** Dado un usuario sin rol `SUPER_ADMIN`, el sistema debe responder 403 en todas las operaciones.
- **CA06:** Dado cada cierre, el sistema debe registrar en la bitácora al administrador (actor) y al usuario afectado.
- **CA07:** Dado un `DELETE`/`POST` web sin `X-XSRF-TOKEN` válido, el sistema debe responder 403.

## Seguridad

- BFLA (API5): autorización por función en el service; test de acceso denegado con rol `USUARIO`.

## Desarrollador

Pendiente de asignar.
