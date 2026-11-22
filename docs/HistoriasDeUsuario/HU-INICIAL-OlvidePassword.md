# Historia de usuario

| Id | Nombre corto de HU | Prioridad | PHU | Estado |
| --- | --- | --- | --- | --- |
| HU-INICIAL-OLVIDEPASSWORD | Olvidé mi contraseña | Media |  | Pendiente |

**Como:** usuario que no recuerda su contraseña
**Quiero:** restablecerla mediante un enlace enviado a mi correo
**Para:** recuperar el acceso a mi cuenta sin ayuda del administrador

**Descripción:**
Desde el login, el usuario pide recuperar su contraseña ingresando su correo. El sistema siempre responde lo mismo, exista o no la cuenta, y si existe y está activa envía un correo con un enlace de un solo uso válido 30 minutos que abre la pantalla de restablecer en el frontend web. Allí el usuario define una contraseña nueva (mínimo 8 caracteres, sin otras reglas); el sistema la guarda, invalida el token y todas las sesiones abiertas del usuario. En mobile solo se pide el correo; el enlace se abre en el navegador. Si el enlace es inválido, expiró o ya se usó, se muestra un error y se puede pedir otro.

Depende de [HU-INICIAL-Base](HU-INICIAL-Base.md), de [HU-INICIAL-Login](HU-INICIAL-Login.md) (levantar el bloqueo) y de [HU-INICIAL-Refresh](HU-INICIAL-Refresh.md) (revocar sesiones), y requiere un servicio de correo (en desarrollo, Mailpit en el compose).

---

## Conversación/Reglas

**Flujo general**

1. El usuario pulsa "¿Olvidaste tu contraseña?" e ingresa su correo.
2. El sistema responde con un mensaje genérico y envía el correo si corresponde.
3. El usuario abre el enlace, ingresa y confirma la nueva contraseña.
4. El sistema valida el token y la política, cambia la contraseña y cierra todas las sesiones.
5. El usuario es llevado al login.

**Reglas de negocio**

- Token aleatorio de alta entropía; se guarda solo su hash (en Redis con TTL de 30 minutos), un solo uso.
- Respuesta uniforme exista o no el correo (anti enumeración), con rate limit específico por IP y por correo.
- Pedir un enlace nuevo invalida el anterior.
- La contraseña nueva cumple la misma política que en [HU-INICIAL-Base](HU-INICIAL-Base.md) (mínimo 8 caracteres, nada más) y no puede coincidir con la actual.
- Restablecer la contraseña también levanta el bloqueo temporal por intentos fallidos.

---

## Criterios de aceptación

- **CA01:** Dado un correo de una cuenta activa, el sistema debe enviar un correo con enlace de un solo uso y responder 200 con mensaje genérico.
- **CA02:** Dado un correo inexistente o de cuenta inactiva, el sistema debe responder 200 con el mismo mensaje y no enviar correo.
- **CA03:** Dado un token válido y una contraseña que cumple la política, el sistema debe cambiarla, invalidar el token y cerrar todas las sesiones del usuario.
- **CA04:** Dado un token inválido, expirado o ya usado, el sistema debe responder 400 con error catalogado y el frontend debe ofrecer pedir otro enlace.
- **CA05:** Dado que la contraseña nueva tiene menos de 8 caracteres o no coincide con su confirmación, el sistema (o el formulario) debe rechazarla indicando el error bajo el campo.
- **CA06:** Dado que se superan los intentos permitidos de solicitud, el sistema debe responder 429.
- **CA07:** Dado que se pide un enlace nuevo, el enlace anterior debe dejar de funcionar.
- **CA08:** Dado que se abre el enlace, el frontend debe quitar el token de la URL (`history.replaceState`) y la página debe servirse con `Referrer-Policy: no-referrer`.
- **CA09:** Dado un correo existente y uno inexistente, el tiempo de respuesta de la solicitud debe ser equivalente.
- **CA10:** Dado un cambio de contraseña exitoso, el sistema debe registrar un evento `INFO` sin contraseña ni token.
- **CA11:** Dado una solicitud o un cambio de contraseña, el sistema debe registrar `AUTH_RECUPERACION_SOLICITADA` y `AUTH_PASSWORD_RESTABLECIDA` en la bitácora, sin token ni contraseña.

## Seguridad

- Sin tokens ni contraseñas en logs ni en la URL de ninguna API (el token va en el enlace una sola vez y se envía por body al restablecer) (OWASP A07, A09).

## Desarrollador

Pendiente de asignar.
