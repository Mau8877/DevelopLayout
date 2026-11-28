# UT-HUI-IDIOMAS-BE-04 — Correos por idioma y control automático

| Campo | Valor |
|-------|-------|
| **Código** | `UT-HUI-IDIOMAS-BE-04` |
| **Nombre** | Correos por idioma y control automático |
| **Historia** | HU-INICIAL-Idiomas-BE |
| **Depende de** | 02, 03 |

## Antes de implementar

IDIOMAS_BACKEND (correos) y SEGURIDAD_AUTH_BACKEND (recuperación).

## Solicitud de negocio

El correo debe llegar en el idioma correcto, y nadie debe volver a escribir frases fijas.

## Objetivo

Correos en dos idiomas y pruebas que impiden retroceder.

## Alcance

- `CorreoCuentaService` recibe el idioma y toma asunto y cuerpo de `i18n/auth/`.
- Recuperación: el idioma se captura en la petición y viaja al ejecutor como dato. Invitación y reenvío: idioma de la petición del administrador.
- Pruebas: mismas claves y marcadores en `es` y `en`; toda clave usada existe; sin frases en excepciones, validaciones ni respuestas; sin voseo en los archivos en español.
- Quitar el respaldo de transición de la UT 01.

**Archivos esperados (crear/tocar):**

- `projects/backend/src/main/java/com/example/backend/features/auth/service/CorreoCuentaService.java`
- `projects/backend/src/main/java/com/example/backend/features/auth/service/RecuperacionService.java`
- `projects/backend/src/main/java/com/example/backend/features/auth/service/InvitacionEnvio.java`
- `projects/backend/src/main/java/com/example/backend/common/idioma/Mensajes.java`
- `projects/backend/src/main/resources/i18n/auth/messages.properties`
- `projects/backend/src/main/resources/i18n/auth/messages_en.properties`
- `projects/backend/src/test/java/com/example/backend/common/idioma/ArchivosDeIdiomaTest.java`
- `projects/backend/src/test/java/com/example/backend/features/auth/CorreoPorIdiomaTest.java`

## Seguridad

El enlace sigue saliendo de la configuración; `forgot` no cambia su respuesta ni espera al envío. Los logs no incluyen el texto del correo.

## Fuera de alcance (no tocar)

- Correos en HTML o plantillas visuales.

## Resultado esperado

Los correos respetan el idioma y las comprobaciones protegen la regla.

## Validación

1. Recuperación e invitación en `es` y `en`, con el enlace correcto.
2. La recuperación en inglés desde el hilo del ejecutor llega en inglés.
3. Quitar una clave en inglés, usar una inexistente o escribir voseo hace fallar las pruebas.
4. `./mvnw test` completo y `pnpm test:prod` en verde.
