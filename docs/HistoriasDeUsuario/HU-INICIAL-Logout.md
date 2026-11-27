# Historia de usuario

| Id | Nombre corto de HU | Prioridad | PHU | Estado |
| --- | --- | --- | --- | --- |
| HU-INICIAL-LOGOUT | Cerrar sesión | Alta |  | En progreso (solo falta la prueba mobile) |

**Como:** usuario con sesión iniciada
**Quiero:** cerrar mi sesión
**Para:** que nadie más pueda usar mi cuenta desde este dispositivo

**Descripción:**
El usuario elige cerrar sesión. El sistema revoca la sesión (estado `REVOCADA`, motivo `LOGOUT`), agrega su identificador a la lista de sesiones revocadas de Redis (así su access token deja de valer de inmediato) y, en web, borra las cookies de acceso y refresh. El cliente limpia su estado y la caché de datos, y muestra el login. Si el refresh token ya no existe o el servidor no responde, el cliente igual cierra la sesión localmente. En web, si el servidor no recibe la petición, las cookies `HttpOnly` no se pueden borrar desde el cliente: al recargar la página la sesión puede reaparecer mientras siga vigente.

Depende de [HU-INICIAL-Login](HU-INICIAL-Login.md).

---

## Criterios de aceptación

- **CA01:** Dado un refresh token válido, el sistema debe invalidarlo, borrar las cookies en web y responder 200.
- **CA02:** Dado que el refresh token ya no es válido, el sistema debe responder 200 igualmente (operación idempotente).
- **CA03:** Dado que el cierre ocurre, el cliente debe borrar la caché de consultas y, en mobile, los tokens guardados en almacenamiento seguro, y redirigir al login.
- **CA04:** Dado que el servidor no responde, el cliente debe cerrar la sesión local de todas formas.
- **CA05:** Dado que ya se cerró sesión, intentar usar el refresh token anterior debe responder 401.
- **CA06:** Dado un `POST /logout` web sin header `X-XSRF-TOKEN` válido, el sistema debe responder 403.
- **CA07:** Dado que la sesión fue cerrada, el sistema debe rechazar de inmediato (401) cualquier request con el access token de esa sesión y registrar `AUTH_LOGOUT` en la bitácora.

## Seguridad

- El access token de una sesión revocada se rechaza al instante gracias a la lista de revocadas en Redis (si Redis falla, se deniega).

## Estado de la validación

**Lo único pendiente es la prueba mobile en un emulador** (UT MOB). Backend y web
están implementados y probados; mobile está implementado y con tests
automáticos. La prueba en emulador se hará al final, junto con el resto de las
pruebas mobile; al pasarla, la historia queda `Hecha`.

## Desarrollador

Pendiente de asignar.
