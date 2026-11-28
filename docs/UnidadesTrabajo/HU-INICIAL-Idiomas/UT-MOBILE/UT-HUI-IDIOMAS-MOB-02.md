# UT-HUI-IDIOMAS-MOB-02 — Selector y textos comunes

| Campo | Valor |
|-------|-------|
| **Código** | `UT-HUI-IDIOMAS-MOB-02` |
| **Nombre** | Selector y textos comunes |
| **Historia** | HU-INICIAL-Idiomas-MOB |
| **Depende de** | 01 |

## Antes de implementar

IDIOMAS_MOBILE y COMPONENTIZACION_MOBILE.

## Solicitud de negocio

Cambiar de idioma y ver lo común traducido.

## Objetivo

Selector en login y menú, y `shared/`, `layouts/` y `core/` sin texto fijo.

## Alcance

- Widget `SelectorIdioma` en `shared/widgets/`.
- Migrar widgets comunes, shells por rol, mensajes propios de red y formato de fechas.
- Nombre del producto en una sola constante (`core/marca.dart`) y reglas de `init-project` al día.

**Archivos esperados (crear/tocar):**

- `projects/mobile/lib/shared/widgets/selector_idioma.dart`
- `projects/mobile/lib/shared/widgets/`
- `projects/mobile/lib/layouts/`
- `projects/mobile/lib/core/`
- `projects/mobile/lib/shared/language/es.json`
- `projects/mobile/lib/shared/language/en.json`
- `scripts/init-project.mjs`
- `scripts/init-project.test.mjs`

## Seguridad

Sin impacto de seguridad: solo textos y preferencia.

## Fuera de alcance (no tocar)

- Pantallas de las features.

## Resultado esperado

Lo común de la app está en ambos idiomas.

## Validación

1. Prueba del selector y de un widget común en `en`.
2. Pruebas de `init-project` en verde.
