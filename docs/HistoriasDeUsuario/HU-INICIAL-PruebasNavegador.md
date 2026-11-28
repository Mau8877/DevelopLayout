# Historia de usuario

| Id | Nombre corto de HU | Prioridad | PHU | Estado |
| --- | --- | --- | --- | --- |
| HU-INICIAL-PRUEBASNAVEGADOR | Comprobar los flujos en un navegador | Alta |  | Hecha |

> **Validación:** en local (tres corridas seguidas y en un proyecto creado con `project:init`) y en el job `e2e` de GitHub. La limpieza al interrumpir (CA01) se prueba en Linux con señales reales sobre `scripts/e2e.mjs`, con sustitutos de Docker, Java y el navegador (`scripts/e2e-interrupcion.test.mjs`).

**Como:** persona que mantiene un proyecto hecho con la plantilla
**Quiero:** comprobar automáticamente los flujos clave en un navegador real
**Para:** detectar antes de publicar los fallos que las pruebas por separado no ven

**Descripción:**
Con un comando se levanta un entorno desechable (base de datos, almacenamiento temporal, correo de prueba, backend y web), se recorren los flujos clave en un navegador y se borra todo. El mismo recorrido corre en el CI. No toca los datos ni la configuración de quien lo ejecuta.

Depende de [HU-INICIAL-Idiomas](HU-INICIAL-Idiomas.md): las pruebas se escriben sobre los textos finales.

---

## Conversación/Reglas

**Flujo general**

1. La persona ejecuta un comando.
2. Se levanta el entorno desechable.
3. El navegador recorre los flujos.
4. Se informa el resultado y se borra el entorno.

**Reglas de negocio**

- Solo el navegador Chromium. Solo la web.
- Se cubren flujos clave, no cada criterio de cada historia.
- Cada prueba crea sus datos: no depende del orden ni de otra prueba.
- Sin esperas fijas: se espera a que algo ocurra.

---

## Seguridad

- **Datos sensibles:** ninguno real. Las credenciales son de prueba y se generan en cada ejecución.
- **Quién puede hacerlo:** quien desarrolla y el CI.
- **Abuso previsible:** borrar datos reales por un choque de nombres (CA02); dejar credenciales en el repositorio o en los informes (se generan al ejecutar y los informes solo contienen datos desechables); publicar puertos a la red (todo escucha en `127.0.0.1`).

## Criterios de aceptación

- **CA01:** Dado un equipo con Docker, un solo comando debe levantar el entorno, ejecutar las pruebas, informar el resultado y borrar lo que creó, también si fallan o se interrumpen.
- **CA02:** Dado que existen contenedores, volúmenes o archivos de configuración del usuario, las pruebas no deben leerlos, modificarlos ni borrarlos; ante un choque de nombres o puertos deben detenerse sin tocar nada.
- **CA03:** Dado el flujo de sesión, deben comprobarse: entrar, credenciales incorrectas, ruta protegida sin sesión, renovación automática, cerrar sesión y cerrar otra sesión propia y de otro usuario.
- **CA04:** Dado un correo de recuperación o de invitación capturado por el entorno, su enlace debe permitir definir la contraseña e iniciar sesión, una sola vez.
- **CA05:** Dado un administrador, deben comprobarse el alta, la búsqueda, el cambio de roles, desactivar y reactivar, y que la sesión afectada deja de funcionar.
- **CA06:** Dado un usuario sin rol de administrador, las pantallas de administración no deben quedar accesibles.
- **CA07:** Dado el cambio de idioma, deben comprobarse la detección, el selector, que se recuerde, un error del servidor y el idioma de un correo.
- **CA08:** Dado un Pull Request que cambia la web, el backend o las pruebas, el CI debe ejecutarlas, fallar si alguna falla y conservar el informe para diagnosticarlo.
- **CA09:** Dado el mismo código, tres ejecuciones seguidas deben dar el mismo resultado.
- **CA10:** Dado un proyecto creado con `project:init`, las pruebas deben funcionar sin editarlas a mano.

## Desarrollador

Pendiente de asignar.
