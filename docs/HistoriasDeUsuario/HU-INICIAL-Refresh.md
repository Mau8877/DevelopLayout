# Historia de usuario

| Id | Nombre corto de HU | Prioridad | PHU | Estado |
| --- | --- | --- | --- | --- |
| HU-INICIAL-REFRESH | Renovar sesión | Alta |  | Pendiente |

**Como:** usuario con sesión iniciada
**Quiero:** que mi sesión se renueve automáticamente
**Para:** no tener que volver a iniciar sesión cada 15 minutos

**Descripción:**
Cuando el access token vence o el cliente recibe un 401, el cliente pide uno nuevo con el refresh token (cookie `HttpOnly` en web, almacenamiento seguro en mobile). El sistema valida el refresh token contra la tabla `sesion`, lo rota (reemplaza su hash en la misma sesión) y devuelve un nuevo access token (nuevas cookies en web). Si el refresh token es inválido, expiró o ya fue usado, el cliente cierra la sesión local y lleva al usuario al login.

Depende de [HU-INICIAL-Login](HU-INICIAL-Login.md).

---

## Conversación/Reglas

**Flujo general**

1. Una petición recibe 401 por access token vencido.
2. El cliente llama a `refresh` una sola vez, aunque haya varias peticiones fallidas en paralelo.
3. El sistema valida y rota el refresh token y devuelve un nuevo access token (nuevas cookies en web).
4. El cliente reintenta las peticiones fallidas con el nuevo token.

**Reglas de negocio**

- Rotación obligatoria: cada refresh token sirve una sola vez.
- Si se presenta un refresh token ya usado (posible robo), el sistema invalida toda la sesión de ese usuario en ese dispositivo.
- Los roles del nuevo access token se leen de la base, no del token anterior.

---

## Criterios de aceptación

- **CA01:** Dado un refresh token válido, el sistema debe responder 200 con un nuevo access token y un nuevo refresh token, e invalidar el anterior.
- **CA02:** Dado un refresh token inexistente, expirado o ya usado, el sistema debe responder 401 y el cliente debe volver al login.
- **CA03:** Dado un refresh token reutilizado, el sistema debe revocar la sesión asociada (motivo `REUSO`), rechazar también su access token de inmediato y registrar `AUTH_REFRESH_REUSO` en la bitácora.
- **CA04:** Dado que varias peticiones fallan a la vez por token vencido, el cliente debe hacer una sola llamada de refresh.
- **CA05:** Dado que el usuario fue desactivado o perdió sus roles, el sistema debe rechazar el refresh con 401 o 403.
- **CA06:** Dado que la base de datos o Redis no están disponibles, el sistema debe fallar cerrado: no emitir tokens y responder 500 genérico.
- **CA07:** Dado un `POST /refresh` web sin header `X-XSRF-TOKEN` válido, el sistema debe responder 403.
- **CA08:** Dado que la sesión cumplió su vida máxima (30 días desde su creación) o su vencimiento, el sistema debe marcarla `EXPIRADA`, responder 401 y no emitir tokens.

## Seguridad

- Refresh token opaco, guardado solo como hash en `sesion`, nunca en logs (OWASP A07, A09).

## Desarrollador

Pendiente de asignar.
