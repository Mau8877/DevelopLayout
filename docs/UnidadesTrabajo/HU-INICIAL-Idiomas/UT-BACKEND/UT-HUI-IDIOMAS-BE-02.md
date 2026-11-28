# UT-HUI-IDIOMAS-BE-02 — Mensajes de autenticación y sesiones

| Campo | Valor |
|-------|-------|
| **Código** | `UT-HUI-IDIOMAS-BE-02` |
| **Nombre** | Mensajes de autenticación y sesiones |
| **Historia** | HU-INICIAL-Idiomas-BE |
| **Depende de** | 01 |

## Antes de implementar

IDIOMAS.md (estilo neutro) e IDIOMAS_BACKEND.

## Solicitud de negocio

Entrar, recuperar la cuenta y manejar sesiones son los mensajes que más se ven.

## Objetivo

Todo mensaje de `features/auth` sale de sus archivos de idioma.

## Alcance

- `i18n/auth/messages*.properties`: excepciones, validaciones de los DTO (`message = "{clave}"`) y mensajes de éxito.
- Los controllers pasan la clave del mensaje de éxito; un solo punto la traduce al armar la respuesta.
- Actualizar las pruebas que comparaban textos y la documentación que los cite.

**Archivos esperados (crear/tocar):**

- `projects/backend/src/main/java/com/example/backend/features/auth/`
- `projects/backend/src/main/resources/i18n/auth/messages.properties`
- `projects/backend/src/main/resources/i18n/auth/messages_en.properties`
- `projects/backend/src/test/java/com/example/backend/features/auth/`

## Seguridad

Las credenciales incorrectas y la cuenta pendiente siguen dando el mismo mensaje en cada idioma (sin enumeración).

## Fuera de alcance (no tocar)

- Cambiar códigos, estados HTTP o reglas.
- Correos.

## Resultado esperado

La autenticación responde en ambos idiomas.

## Validación

1. Un caso de éxito, uno de validación y cada código de autenticación en ambos idiomas.
2. Acceso denegado: correo inexistente y contraseña incorrecta dan texto idéntico en `es` y en `en`.
3. El contrato OpenAPI no cambia.
