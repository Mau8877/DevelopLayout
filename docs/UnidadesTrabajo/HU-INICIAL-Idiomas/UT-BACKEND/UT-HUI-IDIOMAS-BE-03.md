# UT-HUI-IDIOMAS-BE-03 — Mensajes de usuarios y bitácora

| Campo | Valor |
|-------|-------|
| **Código** | `UT-HUI-IDIOMAS-BE-03` |
| **Nombre** | Mensajes de usuarios y bitácora |
| **Historia** | HU-INICIAL-Idiomas-BE |
| **Depende de** | 01 |

## Antes de implementar

IDIOMAS.md e IDIOMAS_BACKEND.

## Solicitud de negocio

La administración también tiene que entenderse en ambos idiomas.

## Objetivo

Todo mensaje de `features/usuarios` y `features/bitacora` sale de sus archivos de idioma.

## Alcance

- `i18n/usuarios/` y `i18n/bitacora/`: excepciones, validaciones y éxitos.
- `common/util/Paginacion` y cualquier otro mensaje transversal pasan a `i18n/common/`.

**Archivos esperados (crear/tocar):**

- `projects/backend/src/main/java/com/example/backend/features/usuarios/`
- `projects/backend/src/main/java/com/example/backend/features/bitacora/`
- `projects/backend/src/main/java/com/example/backend/common/util/Paginacion.java`
- `projects/backend/src/main/resources/i18n/usuarios/messages.properties`
- `projects/backend/src/main/resources/i18n/usuarios/messages_en.properties`
- `projects/backend/src/main/resources/i18n/bitacora/messages.properties`
- `projects/backend/src/main/resources/i18n/bitacora/messages_en.properties`
- `projects/backend/src/test/java/com/example/backend/features/usuarios/`

## Seguridad

No cambia ninguna autorización.

## Fuera de alcance (no tocar)

- Traducir lo que se guarda en la bitácora.

## Resultado esperado

La administración responde en ambos idiomas.

## Validación

1. Cada `ERR_US_*` y `ERR_BIT_*` en ambos idiomas.
2. Acceso denegado: un `USUARIO` recibe 403 en el idioma pedido.
