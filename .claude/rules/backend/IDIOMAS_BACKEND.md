---
globs: projects/backend/**/*
---

# Idiomas — Backend

Complementa a [IDIOMAS.md](../IDIOMAS.md). El backend **responde en el idioma que
pide el cliente**: mensajes de éxito, errores, validaciones y correos.

## Dónde viven los textos

Archivos estándar de Spring (`.properties`, UTF-8), por módulo:

```
src/main/resources/i18n/
├── common/    messages.properties (es)   messages_en.properties
├── auth/      messages.properties        messages_en.properties
└── usuarios/  messages.properties        messages_en.properties
```

- `messages.properties` es español neutro (idioma por defecto);
  `messages_en.properties`, inglés neutro.
- La clave empieza por el módulo: `auth.error.credenciales`,
  `usuarios.exito.creado`, `usuarios.validacion.rolRequerido`.
- Un solo `MessageSource`, que registra todos los módulos, con
  `fallbackToSystemLocale=false`. Una clave inexistente es un error, no se
  muestra la clave al cliente.

## Idioma de cada petición

- Lo decide `common/idioma/SelectorDeIdioma`. **No** se usa `request.getLocale()` ni
  `AcceptHeaderLocaleResolver`: ante una cabecera mal formada devuelven el idioma
  del servidor.
- Sale de `Accept-Language`; soportados `es` y `en`; cualquier otro o su
  ausencia es `es`. El valor del cliente **solo elige entre esa lista**: nunca se
  usa para armar una ruta, un nombre de archivo o un texto.
- `Accept-Language` está en las cabeceras permitidas de CORS.

## En el código

- Las excepciones de negocio llevan **código + clave + argumentos**, no una frase.
  `GlobalExceptionHandler` traduce.
- Las anotaciones de validación usan `message = "{clave}"`.
- Los controllers responden con la clave del mensaje de éxito; no escriben
  frases.
- Lo que responde fuera del `DispatcherServlet` (`RateLimitFilter`, el punto de
  entrada de autenticación y el de acceso denegado) usa el mismo `MessageSource`
  y la misma elección de idioma.
- El **código de error no cambia** con el idioma, y el contrato OpenAPI tampoco.
- Logs, bitácora y excepciones internas: sin traducir. La bitácora guarda códigos.

## Correos

El asunto y el cuerpo salen de los `.properties`. El idioma es el de la petición
que originó el envío:

| Correo | Idioma |
| --- | --- |
| Recuperación de contraseña | El de quien la pide |
| Invitación y reenvío | El del administrador que invita |

Si el envío ocurre en otro hilo, el idioma **se captura en la petición** y se pasa
como dato: el hilo del ejecutor no tiene contexto.

No cambia ninguna regla de seguridad: el enlace sale de la configuración y
`forgot` sigue respondiendo igual exista o no la cuenta.

## Control automático (pruebas)

- Mismas claves y marcadores en `es` y `en`.
- Toda clave usada en el código existe.
- Sin frases fijas en excepciones, validaciones ni respuestas.
- Sin voseo en los archivos en español.
- Cada endpoint de error relevante se prueba en ambos idiomas.
