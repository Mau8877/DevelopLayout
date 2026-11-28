# UT-HUI-CONVENCIONES-MOB-01 — Rutas de la app y llamadas en inglés

| Campo | Valor |
|-------|-------|
| **Código** | `UT-HUI-CONVENCIONES-MOB-01` |
| **Nombre** | Rutas de la app y llamadas en inglés |
| **Historia** | HU-INICIAL-Convenciones-MOB |
| **Depende de** | — (se integra junto con la UT BE-01) |

## Antes de implementar

NAVEGACION_MOBILE y CONSUMO_API_MOBILE.

## Solicitud de negocio

La app usa las direcciones nuevas.

## Objetivo

Rutas de `go_router` en inglés y llamadas a la API nueva.

## Alcance

- `/mis-sesiones` → `/my-sessions` y `/cargando` → `/loading`, incluido el `redirect` y sus pruebas.
- `services/` con las rutas nuevas de la API (sesiones propias).

**Archivos esperados (crear/tocar):**

- `projects/mobile/lib/core/routing/`
- `projects/mobile/lib/features/`
- `projects/mobile/lib/layouts/`
- `projects/mobile/test/`

## Seguridad

El `redirect` por sesión se comporta igual.

## Fuera de alcance (no tocar)

- Barriles en Dart: esta historia solo los define en la web.
- Pruebas en emulador (al final del proyecto).

## Resultado esperado

La app navega y consume la API en inglés.

## Validación

1. Sin sesión, `/my-sessions` lleva a `/login`.
2. `flutter analyze` y `flutter test` en verde.
