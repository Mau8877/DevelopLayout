# Historia de usuario

| Id | Nombre corto de HU | Prioridad | PHU | Estado |
| --- | --- | --- | --- | --- |
| HU-INICIAL-CONVENCIONES | Direcciones en inglés y código ordenado | Alta |  | En progreso |

> **Avance:** Implementada en backend, web y mobile con pruebas automáticas. Falta la validación mobile en emulador.

**Como:** persona que reutiliza la plantilla
**Quiero:** que todas las direcciones estén en inglés y que cada carpeta exponga su contenido de la misma forma
**Para:** que los proyectos nuevos partan de convenciones uniformes

**Descripción:**
Las direcciones de la web, las rutas de la app y las de la API pasan a inglés. Lo que ve la persona en las pantallas no cambia, y nadie pierde permisos ni funciones. En la web, cada carpeta de código tiene un archivo que reúne lo que ofrece, para importar siempre igual. Las direcciones anteriores dejan de existir: la plantilla todavía no está desplegada, así que no se agregan redirecciones.

Va antes de [HU-INICIAL-Idiomas](HU-INICIAL-Idiomas.md), que modifica casi todos los archivos.

---

## Conversación/Reglas

**Flujo general**

1. La persona navega por la web o la app y las direcciones están en inglés.
2. Quien programa importa de una carpeta por su archivo de entrada.

**Reglas de negocio**

- Cambian las direcciones (segmentos, acciones y filtros de la URL). No cambian los campos de las respuestas, los códigos de error, los valores como `EXITO` ni la base de datos.
- Las direcciones de los enlaces de los correos ya estaban en inglés y no cambian.
- Backend, web y app se publican juntos: con direcciones distintas dejan de entenderse.

---

## Seguridad

- **Datos sensibles:** ninguno nuevo.
- **Quién puede hacerlo:** nadie gana ni pierde permisos.
- **Abuso previsible:** que una dirección renombrada quede sin su permiso, su límite de peticiones o su protección (CA04); que las reglas que nombran direcciones (seguridad, límites, proxy) sigan apuntando a las viejas.

## Criterios de aceptación

- **CA01:** Dado cualquier pantalla de la web, su dirección debe estar en inglés y en kebab-case, también las rutas de la app.
- **CA02:** Dado cualquier operación de la API, sus segmentos, acciones y filtros de la URL deben estar en inglés, con los mismos métodos, datos, estados y códigos de error.
- **CA03:** Dado una dirección anterior, la API debe responder que no existe y la web, mostrar su página de no encontrado.
- **CA04:** Dado el cambio de direcciones, cada operación debe exigir la misma sesión, permiso, protección CSRF y límite de peticiones que antes.
- **CA05:** Dado el menú, no debe ofrecer enlaces a pantallas que no existen.
- **CA06:** Dado una carpeta de código de una feature o una carpeta transversal propia, debe tener un archivo de entrada que exporte todo su contenido.
- **CA07:** Dado un archivo nuevo sin exportar, una carpeta sin archivo de entrada o un import que se salte el de una feature, las comprobaciones deben fallar.
- **CA08:** Dado el cambio de estructura, la web debe seguir funcionando y pasar sus comprobaciones.

## Desarrollador

Pendiente de asignar.
