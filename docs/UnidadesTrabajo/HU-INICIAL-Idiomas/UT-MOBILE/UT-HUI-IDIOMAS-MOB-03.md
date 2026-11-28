# UT-HUI-IDIOMAS-MOB-03 — Migrar autenticación y diagnóstico

| Campo | Valor |
|-------|-------|
| **Código** | `UT-HUI-IDIOMAS-MOB-03` |
| **Nombre** | Migrar autenticación y diagnóstico |
| **Historia** | HU-INICIAL-Idiomas-MOB |
| **Depende de** | 02 |

## Antes de implementar

IDIOMAS_MOBILE y FORMULARIOS_MOBILE.

## Solicitud de negocio

Todas las pantallas de la app quedan traducidas.

## Objetivo

`features/auth` y `features/health` sin texto fijo.

## Alcance

- `features/{auth,health}/language/{es,en}.json`, declarados en `pubspec.yaml` y en `app_texts.dart`.
- `validator` con `context.t`; providers y services devuelven claves o el mensaje del servidor.
- Actualizar las pruebas al español neutro y agregar los casos en inglés.

**Archivos esperados (crear/tocar):**

- `projects/mobile/lib/features/auth/`
- `projects/mobile/lib/features/health/`
- `projects/mobile/pubspec.yaml`
- `projects/mobile/lib/shared/language/app_texts.dart`
- `projects/mobile/test/features/`

## Seguridad

Los mensajes de login y recuperación siguen siendo los del servidor.

## Fuera de alcance (no tocar)

- Funciones nuevas.

## Resultado esperado

La app completa está en ambos idiomas.

## Validación

1. Cada pantalla con una prueba en `en`.
2. Acceso denegado: credenciales incorrectas muestran el mensaje del servidor.
