# Historia de usuario

| Id | Nombre corto de HU | Prioridad | PHU | Estado |
| --- | --- | --- | --- | --- |
| HU-INICIAL-IDIOMAS | Usar el sistema en español o inglés | Alta |  | En progreso |

> **Avance:** Implementada en backend, web y mobile con pruebas automáticas. Falta la validación mobile en emulador.

**Como:** persona que usa el sistema
**Quiero:** verlo en español o en inglés y poder cambiar de idioma
**Para:** entender cada pantalla, mensaje y correo en un idioma que manejo

**Descripción:**
La web y la app muestran todo en español neutro o en inglés neutro. Al abrirlas por primera vez usan el idioma del navegador o del teléfono; si no es ninguno de los dos, el español. La persona puede cambiarlo cuando quiera, también antes de iniciar sesión, y se recuerda en ese dispositivo. Los mensajes del servidor y los correos llegan en el mismo idioma. Cambiar de idioma no cierra la sesión ni cambia lo que cada persona puede hacer.

Las reglas de escritura y de dónde viven los textos están en [IDIOMAS.md](../../.claude/rules/IDIOMAS.md). Depende de las historias anteriores, incluida [HU-INICIAL-Convenciones](HU-INICIAL-Convenciones.md), porque traduce sus pantallas, mensajes y correos.

---

## Conversación/Reglas

**Flujo general**

1. La persona abre la web o la app y la ve en el idioma de su navegador o teléfono.
2. Si quiere otro, lo elige en el selector.
3. Todo lo visible cambia al instante y la elección se recuerda.
4. Los mensajes del servidor y los correos que provoque llegan en ese idioma.

**Reglas de negocio**

- Idiomas disponibles: español neutro (por defecto) e inglés neutro. Agregar otro no exige cambiar pantallas.
- Español neutro: trato de tú, sin voseo ni modismos. Inglés neutro: ortografía de Estados Unidos, sin jerga.
- La preferencia es del dispositivo, no de la cuenta.
- La invitación y su reenvío salen en el idioma del administrador que los envía; la recuperación, en el de quien la pide.
- El nombre del producto y los códigos de error no se traducen.
- Ningún texto visible queda escrito en el código: sin esto no se da por terminada una pantalla.

---

## Seguridad

- **Datos sensibles:** ninguno nuevo. La preferencia de idioma no identifica a nadie.
- **Quién puede hacerlo:** cualquiera, con o sin sesión.
- **Abuso previsible:** mandar un idioma manipulado (solo se elige de una lista cerrada, CA06); aprovechar las traducciones para inyectar contenido (los datos van como marcadores y se escapan); delatar cuentas con mensajes o tiempos distintos por idioma (CA07).

## Criterios de aceptación

- **CA01:** Dado que no hay una preferencia guardada, la web y la app deben usar el idioma del navegador o del teléfono si es español o inglés, y español en cualquier otro caso.
- **CA02:** Dado que la persona cambia el idioma con el selector, disponible también sin sesión, todo lo visible debe cambiar al instante, sin recargar, sin cerrar la sesión y sin perder lo escrito en un formulario.
- **CA03:** Dado que la persona eligió un idioma, al volver a abrir la web o la app en el mismo dispositivo debe mantenerse.
- **CA04:** Dado un idioma elegido, todos los textos de las pantallas deben verse en él, incluidos validaciones, avisos, estados vacíos y textos de accesibilidad.
- **CA05:** Dado un idioma elegido, los mensajes del servidor (éxito, errores, validaciones, límite de peticiones y falta de sesión o de permiso) deben llegar en él, con el mismo código de error.
- **CA06:** Dado una petición sin idioma o con uno no soportado, el servidor debe responder en español.
- **CA07:** Dado que se pide recuperar la contraseña, el correo debe llegar en el idioma de quien lo pide y la respuesta debe seguir siendo la misma exista o no la cuenta.
- **CA08:** Dado un alta o un reenvío de invitación, el correo debe llegar en el idioma del administrador.
- **CA09:** Dado un idioma elegido, las fechas, horas y plurales deben mostrarse según ese idioma.
- **CA10:** Dado cualquier texto en español o en inglés, debe cumplir las reglas de idioma neutro; en particular, el español no puede tener voseo.
- **CA11:** Dado un cambio que agrega un texto fijo, usa una clave inexistente o deja un idioma sin una clave, las comprobaciones automáticas deben fallar.
- **CA12:** Dado el uso de la app móvil, debe cumplir los mismos criterios de selección, textos y mensajes que la web.

## Desarrollador

Pendiente de asignar.
