# Historia de usuario

| Id | Nombre corto de HU | Prioridad | PHU | Estado |
| --- | --- | --- | --- | --- |
| HU-INICIAL-CAMBIARPASSWORD | Cambiar mi contraseña | Media |  | En progreso |

> **Avance:** implementada en backend, web y mobile con pruebas automáticas; el flujo web y el correo, también en un navegador real. Falta la validación mobile en emulador.

**Como:** usuario con la sesión iniciada
**Quiero:** cambiar mi contraseña indicando la actual
**Para:** mantener segura mi cuenta sin pasar por la recuperación por correo

**Descripción:**
En la web, la opción «Cambiar contraseña» del menú de usuario abre una pantalla; en la app se llega desde la barra superior. El usuario escribe su contraseña actual, la nueva y su confirmación. Si todo es correcto, la contraseña cambia, se cierran todas las sesiones de la cuenta (incluida la actual), la persona vuelve a la pantalla de inicio de sesión con un aviso de que debe entrar con la contraseña nueva, y llega un correo que avisa del cambio. Si la contraseña actual es incorrecta, la nueva no cumple la política o es igual a la actual, no cambia nada y el error se muestra junto al campo, sin cerrar la sesión. Tras varios fallos seguidos con la contraseña actual, el cambio queda bloqueado un tiempo.

Depende de [HU-INICIAL-Login](HU-INICIAL-Login.md) (límite de intentos), de [HU-INICIAL-OlvidePassword](HU-INICIAL-OlvidePassword.md) (política, enlaces y envío de correo), de [HU-INICIAL-Refresh](HU-INICIAL-Refresh.md) (revocación de sesiones) y de [HU-INICIAL-Idiomas](HU-INICIAL-Idiomas.md) (textos en español e inglés).

---

## Conversación/Reglas

**Flujo general**

1. El usuario abre «Cambiar contraseña».
2. Escribe la contraseña actual, la nueva y la confirmación.
3. El sistema comprueba la contraseña actual y la política de la nueva.
4. Cambia la contraseña, cierra todas las sesiones y anula los enlaces de recuperación pendientes.
5. Lleva a la persona al inicio de sesión, le avisa que la contraseña cambió y envía el aviso por correo.
6. La persona inicia sesión con la contraseña nueva.

**Reglas de negocio**

- Cada persona cambia solo su propia contraseña. Un administrador no puede cambiar la de otra.
- La contraseña actual es obligatoria, aunque la sesión esté iniciada.
- La contraseña nueva cumple la misma política que en el resto del sistema (mínimo 8 caracteres y máximo 72 bytes, sin otras reglas) y no puede ser igual a la actual.
- Al cambiar la contraseña se cierran de inmediato todas las sesiones de la cuenta, también la actual. No se inicia una sesión nueva sola: hay que volver a entrar.
- Solo una sesión que sigue activa puede hacer el cambio: si fue cerrada mientras tanto, no cambia nada.
- Tras 5 intentos con la contraseña actual incorrecta en 15 minutos, el cambio queda bloqueado para esa cuenta durante 15 minutos. Este bloqueo no impide iniciar sesión ni cierra sesiones, y los fallos del inicio de sesión no lo activan.
- Un cambio correcto reinicia ese contador y levanta el bloqueo por intentos fallidos del inicio de sesión.
- Un error en este flujo nunca se trata como sesión vencida: la persona sigue donde estaba.
- El aviso por correo sale en el idioma de quien hace el cambio, no incluye contraseñas ni enlaces con credenciales y dice qué hacer si no fue esa persona. Si el correo falla, el cambio se mantiene.

---

## Seguridad

- **Datos sensibles:** la contraseña actual, la nueva y el correo de la cuenta.
- **Quién puede hacerlo:** solo una persona autenticada, sobre su propia cuenta. La cuenta sale de la sesión, nunca de un dato enviado por el cliente, y la base de datos impide cambiar la contraseña de otra (OWASP A01).
- **Abuso previsible:**
  - Con una sesión robada o un dispositivo desatendido, tomar la cuenta: se exige la contraseña actual (CA02) y se avisa por correo (CA11).
  - Adivinar la contraseña actual desde una sesión abierta: límite por cuenta (CA06) y límite de peticiones (CA07).
  - Conservar un acceso ya obtenido: se cierran todas las sesiones (CA03, CA10) y se anulan los enlaces pendientes (CA08).
  - Hacer el cambio desde un sitio ajeno con las cookies de la víctima: protección CSRF (CA13).
  - Carreras entre un cambio y un inicio de sesión o un restablecimiento (CA10, CA14).
- **Se acepta:** quien tenga una sesión robada puede bloquear este cambio durante 15 minutos fallando a propósito. La persona conserva la recuperación por correo, que también cierra todas las sesiones.
- Las contraseñas no aparecen en logs, bitácora, correos, URL ni cachés del cliente (OWASP A07, A09).

## Criterios de aceptación

- **CA01:** Dado que la contraseña actual es correcta y la nueva cumple la política, el sistema debe cambiarla y llevar a la persona al inicio de sesión con un aviso de que la contraseña cambió. La contraseña anterior deja de servir para entrar y la nueva funciona.
- **CA02:** Dado que la contraseña actual es incorrecta, el sistema debe rechazar el cambio con un error catalogado junto a ese campo, sin cambiar nada ni cerrar la sesión.
- **CA03:** Dado un cambio correcto, todas las sesiones de la cuenta, web y mobile, incluida la actual, deben dejar de valer de inmediato, y no debe poder renovarse ninguna.
- **CA04:** Dado que la contraseña nueva tiene menos de 8 caracteres, supera 72 bytes o no coincide con su confirmación, el formulario o el sistema deben rechazarla indicando el error bajo el campo.
- **CA05:** Dado que la contraseña nueva es igual a la actual, el sistema debe responder 400 con error catalogado, sin cambiar nada ni cerrar sesiones.
- **CA06:** Dado que hubo 5 intentos con la contraseña actual incorrecta en 15 minutos, el sistema debe responder 423 a los siguientes, aunque la contraseña sea correcta, y permitir iniciar sesión como siempre. Varios intentos simultáneos no deben permitir comprobar más contraseñas que el límite.
- **CA07:** Dado que una IP supera su límite de peticiones para este flujo, el sistema debe responder 429.
- **CA08:** Dado un cambio correcto, cualquier enlace de recuperación pendiente de esa cuenta debe dejar de funcionar.
- **CA09:** Dado que no hay una sesión válida, el sistema debe responder 401 y la pantalla no debe ser accesible.
- **CA10:** Dado que la sesión que pide el cambio se cierra mientras se procesa, o que al mismo tiempo ocurre un inicio de sesión con la contraseña anterior, no debe cambiarse nada desde una sesión cerrada; y, si el cambio se completa, no debe quedar activa ninguna sesión abierta con la contraseña anterior.
- **CA11:** Dado un cambio correcto, el sistema debe enviar un correo de aviso a la cuenta, en el idioma de la petición. Si el envío falla, el cambio se mantiene y el fallo se registra sin datos sensibles.
- **CA12:** Dado un cambio correcto o un intento rechazado por la contraseña actual, la bitácora debe registrar quién fue, el resultado, la IP y el user agent, sin contraseñas.
- **CA13:** Dado una petición web sin la protección CSRF válida, el sistema debe rechazarla sin cambiar la contraseña.
- **CA14:** Dados dos cambios simultáneos de la misma cuenta, solo uno debe tener éxito.
- **CA15:** Dado un usuario con cualquier rol, la opción debe estar disponible en la web y en la app, en español y en inglés.
- **CA16:** Dado un cambio en curso, el formulario no debe permitir enviarlo otra vez; al terminar bien, las contraseñas escritas no deben quedar disponibles en la pantalla ni al volver atrás.

## Desarrollador

Pendiente de asignar.
