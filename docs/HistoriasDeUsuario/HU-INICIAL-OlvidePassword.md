# Historia de usuario

| Id | Nombre corto de HU | Prioridad | PHU | Estado |
| --- | --- | --- | --- | --- |
| HU-INICIAL-OLVIDEPASSWORD | Olvidé mi contraseña | Media |  | En progreso (solo falta la prueba mobile) |

**Como:** usuario que no recuerda su contraseña
**Quiero:** restablecerla mediante un enlace enviado a mi correo
**Para:** recuperar el acceso a mi cuenta sin ayuda del administrador

**Descripción:**
Desde el login, el usuario pide recuperar su contraseña ingresando su correo. El sistema responde siempre lo mismo y sin esperar a saber si la cuenta existe; si existe y está activa, envía después un correo con un enlace de un solo uso válido 30 minutos que abre la pantalla de restablecer en el frontend web. Allí el usuario define una contraseña nueva (mínimo 8 caracteres, sin otras reglas); el sistema la guarda, invalida el enlace y todas las sesiones abiertas del usuario, levanta el bloqueo por intentos fallidos y le avisa por correo que la contraseña cambió. En mobile solo se pide el correo; el enlace se abre en el navegador. Si el enlace es inválido, expiró o ya se usó, se muestra un error y se puede pedir otro. Si la contraseña elegida no sirve, el enlace sigue valiendo.

Depende de [HU-INICIAL-Base](HU-INICIAL-Base.md), de [HU-INICIAL-Login](HU-INICIAL-Login.md) (levantar el bloqueo) y de [HU-INICIAL-Refresh](HU-INICIAL-Refresh.md) (revocar sesiones). Requiere un servicio de correo: en desarrollo, Mailpit en el compose; en producción, un servidor SMTP cuya configuración es obligatoria para desplegar.

---

## Conversación/Reglas

**Flujo general**

1. El usuario pulsa "¿Olvidaste tu contraseña?" e ingresa su correo.
2. El sistema responde con un mensaje genérico y envía el correo si corresponde.
3. El usuario abre el enlace, ingresa y confirma la nueva contraseña.
4. El sistema valida el token y la política, cambia la contraseña y cierra todas las sesiones.
5. El usuario es llevado al login y recibe un correo que avisa del cambio.

**Reglas de negocio**

- Token aleatorio de alta entropía; se guarda solo su hash (en Redis, 30 minutos), un solo uso. Pedir un enlace nuevo invalida el anterior.
- El enlace solo se consume cuando el cambio de contraseña se va a confirmar: un rechazo por la contraseña elegida no lo gasta. Si el sistema falla justo al confirmar, la contraseña no cambia y puede hacer falta pedir otro enlace.
- La respuesta a la solicitud es uniforme y no depende de la cuenta (ni en contenido ni en tiempo): todo lo que depende de ella ocurre después de responder.
- Dos límites distintos. **Por IP:** al superarlo, 429. **Por correo:** 3 solicitudes por hora, contadas antes de saber si la cuenta existe; al superarlo se responde igual que siempre, no se envía nada y el último enlace sigue valiendo.
- El envío no está garantizado: si el correo falla o el servidor se reinicia en ese momento, el usuario pide otro enlace.
- La contraseña nueva cumple la misma política que en [HU-INICIAL-Base](HU-INICIAL-Base.md) (mínimo 8 caracteres, nada más; máximo 72 bytes) y no puede coincidir con la actual.
- Restablecer la contraseña levanta el bloqueo temporal por intentos fallidos, desde cualquier origen.
- La dirección del enlace sale de la configuración del servidor, nunca de datos de la petición.
- El aviso del cambio es el mismo que en [HU-INICIAL-CambiarPassword](HU-INICIAL-CambiarPassword.md): sale en el idioma de quien restablece, sin enlaces ni contraseñas, y si falla el cambio se mantiene.

---

## Criterios de aceptación

- **CA01:** Dado un correo de una cuenta activa, el sistema debe enviar un correo con enlace de un solo uso y responder 200 con mensaje genérico.
- **CA02:** Dado un correo inexistente o de cuenta inactiva, el sistema debe responder 200 con el mismo mensaje y no enviar correo.
- **CA03:** Dado un token válido y una contraseña que cumple la política, el sistema debe cambiarla, invalidar el token y cerrar todas las sesiones del usuario.
- **CA04:** Dado un token inválido, expirado o ya usado, el sistema debe responder 400 con error catalogado y el frontend debe ofrecer pedir otro enlace.
- **CA05:** Dado que la contraseña nueva tiene menos de 8 caracteres, supera 72 bytes o no coincide con su confirmación, el sistema (o el formulario) debe rechazarla indicando el error bajo el campo, sin gastar el enlace.
- **CA06:** Dado que una IP supera su límite de solicitudes o de intentos de restablecimiento, el sistema debe responder 429.
- **CA07:** Dado que se pide un enlace nuevo, el enlace anterior debe dejar de funcionar.
- **CA08:** Dado que se abre el enlace, el frontend debe quitar el token de la URL (`history.replaceState`) y la página debe servirse con `Referrer-Policy: no-referrer`.
- **CA09:** Dado un correo existente y uno inexistente, la solicitud no debe consultar la cuenta ni enviar correo antes de responder, de modo que el tiempo de respuesta no dependa de la cuenta.
- **CA10:** Dado un cambio de contraseña exitoso, el sistema debe registrar un evento `INFO` sin contraseña ni token.
- **CA11:** Dado un correo enviado (o que falló) y un cambio de contraseña, el sistema debe registrar `AUTH_RECUPERACION_SOLICITADA` y `AUTH_PASSWORD_RESTABLECIDA` en la bitácora, con la IP y el user agent de la petición, sin token ni contraseña.
- **CA12:** Dado que un correo supera 3 solicitudes en una hora, el sistema debe responder 200 con el mismo mensaje, no enviar correo y no invalidar el último enlace.
- **CA13:** Dado un login con la contraseña anterior que coincide en el tiempo con el restablecimiento, al terminar no debe quedar ninguna sesión activa abierta con esa contraseña.
- **CA14:** Dado que la contraseña nueva es igual a la actual, el sistema debe responder 400 con error catalogado y el enlace debe seguir sirviendo.
- **CA15:** Dado que el servidor de correo no está disponible, la aplicación debe seguir sana (`/health`), la solicitud debe responder 200 y el fallo debe quedar registrado sin datos sensibles.
- **CA16:** Dado un restablecimiento exitoso de una cuenta bloqueada, el usuario debe poder iniciar sesión de inmediato desde cualquier IP.
- **CA17:** Dado un despliegue de producción sin la configuración de correo o sin la dirección HTTPS de la web, la aplicación no debe arrancar.
- **CA18:** Dados dos restablecimientos simultáneos con el mismo enlace, solo uno debe tener éxito.
- **CA19:** Dado un restablecimiento exitoso, el sistema debe enviar a la cuenta un aviso de que la contraseña cambió, en el idioma de la petición, sin enlaces, tokens ni contraseñas. Un restablecimiento rechazado no debe enviarlo, y si el envío falla el cambio se mantiene y el fallo se registra sin datos sensibles.

## Seguridad

- **Datos sensibles:** correo, contraseña nueva y token de restablecimiento.
- **Quién puede hacerlo:** cualquiera puede pedir un enlace (sin sesión); solo quien tiene el token vigente cambia la contraseña, y solo la de su cuenta (RLS) (OWASP A01).
- **Abuso previsible:** cambiar la contraseña sin que la persona se entere (CA19), enumerar cuentas (CA02, CA09), inundar un buzón (CA06, CA12), adivinar o reutilizar tokens (CA04, CA07, CA18), conservar un acceso robado durante el cambio (CA03, CA13) y envenenar el enlace con cabeceras de la petición (la dirección sale de la configuración). Quien conozca un correo puede consumir su cupo de envíos durante una hora; se acepta.
- Sin tokens ni contraseñas en logs ni en la URL de ninguna API (el token va en el fragmento del enlace y se envía por body al restablecer) (OWASP A07, A09).

## Estado de la validación

**Lo único pendiente es la prueba mobile en un emulador** (UT MOB-02). Backend y
web: tests automáticos y flujo completo en el navegador con Mailpit; producción:
`pnpm test:prod` con el correo caído; mobile: implementado y con tests
automáticos. La prueba en emulador se hará al final, junto con el resto de las
pruebas mobile; al pasarla, la historia queda `Hecha`.

La entrega con un servidor SMTP real no se probó y **no bloquea** la historia:
depende del proveedor que elija cada proyecto, y se comprueba al desplegarlo.

## Desarrollador

Pendiente de asignar.
