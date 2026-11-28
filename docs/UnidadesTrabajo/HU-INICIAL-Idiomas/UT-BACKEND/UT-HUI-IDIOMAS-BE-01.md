# UT-HUI-IDIOMAS-BE-01 — Mensajes por idioma: infraestructura y mensajes comunes

| Campo | Valor |
|-------|-------|
| **Código** | `UT-HUI-IDIOMAS-BE-01` |
| **Nombre** | Mensajes por idioma: infraestructura y mensajes comunes |
| **Historia** | HU-INICIAL-Idiomas-BE |
| **Depende de** | — |

## Antes de implementar

IDIOMAS_BACKEND, EXCEPCIONES_BACKEND y SEGURIDAD_AUTH_BACKEND (rate limit).

## Solicitud de negocio

El servidor tiene que responder en el idioma de quien pregunta.

## Objetivo

El backend elige idioma por petición y traduce sus errores transversales.

## Alcance

- `config/IdiomaConfig`: `MessageSource` UTF-8 con los paquetes de `i18n/{modulo}/messages`, sin idioma del sistema como respaldo; `AcceptHeaderLocaleResolver` con `es` y `en`, y `es` por defecto. El validador usa el mismo `MessageSource`.
- `common/idioma/Mensajes`: un único punto para traducir clave + argumentos, también para quien responde fuera del `DispatcherServlet`.
- `BusinessException` y subtipos: código + **clave** + argumentos. `getMessage()` devuelve la clave (útil en logs).
- `GlobalExceptionHandler`, `RateLimitFilter`, `RestAuthenticationEntryPoint` y `RestAccessDeniedHandler` traducen. Mensajes `ERR_SYS_*` en `i18n/common/`.
- CORS: agregar `Accept-Language` a las cabeceras permitidas.
- **Transición:** mientras existan frases sin migrar, una clave que no existe se devuelve tal cual. La UT 04 quita ese respaldo.

**Archivos esperados (crear/tocar):**

- `projects/backend/src/main/java/com/example/backend/config/IdiomaConfig.java`
- `projects/backend/src/main/java/com/example/backend/common/idioma/Mensajes.java`
- `projects/backend/src/main/java/com/example/backend/common/exception/BusinessException.java`
- `projects/backend/src/main/java/com/example/backend/common/exception/GlobalExceptionHandler.java`
- `projects/backend/src/main/java/com/example/backend/security/RateLimitFilter.java`
- `projects/backend/src/main/java/com/example/backend/security/RestAuthenticationEntryPoint.java`
- `projects/backend/src/main/java/com/example/backend/security/RestAccessDeniedHandler.java`
- `projects/backend/src/main/java/com/example/backend/config/SecurityConfig.java`
- `projects/backend/src/main/resources/i18n/common/messages.properties`
- `projects/backend/src/main/resources/i18n/common/messages_en.properties`
- `projects/backend/src/test/java/com/example/backend/common/idioma/IdiomaPeticionTest.java`

## Seguridad

El idioma del cliente solo elige de una lista cerrada. Los errores siguen sin exponer detalles internos.

## Fuera de alcance (no tocar)

- Mensajes de autenticación, usuarios y bitácora (UT 02 y 03).
- Correos (UT 04).

## Resultado esperado

Los errores transversales llegan en español o inglés.

## Validación

1. `ERR_SYS_*`, 401, 403 y 429 en `es`, en `en` y sin cabecera (español), con el mismo código.
2. Un `Accept-Language` inválido o con intento de ruta responde en español.
3. Acceso denegado: el 401 y el 403 siguen siendo los mismos estados.
4. El preflight de CORS acepta `Accept-Language`.
