# UT-HUI-IDIOMAS-FE-03 — Migrar los textos comunes

| Campo | Valor |
|-------|-------|
| **Código** | `UT-HUI-IDIOMAS-FE-03` |
| **Nombre** | Migrar los textos comunes |
| **Historia** | HU-INICIAL-Idiomas-FE |
| **Depende de** | 01 |

## Antes de implementar

IDIOMAS.md, IDIOMAS_FRONTEND y la guía de `project:init` en `scripts/README.md`.

## Solicitud de negocio

Menús, tablas, fechas y errores comunes aparecen en todas las pantallas.

## Objetivo

Sin texto fijo fuera de las features.

## Alcance

- Migrar `layouts/`, `components/` (incluidos los textos en inglés de la salida de shadcn), tablas, errores propios de `apiClient` y utilidades.
- Fechas: `formatDateTime`, `formatRelativeDate` y la configuración de dayjs usan el idioma vigente. `resumirDispositivo` traduce sus textos.
- El nombre del producto pasa a una sola constante (`utils/marca.ts`).
- `scripts/init-project.mjs`: ajustar las reglas que apuntaban a los textos movidos (su prueba sobre la plantilla lo exige).

**Archivos esperados (crear/tocar):**

- `projects/frontend/src/layouts/`
- `projects/frontend/src/components/`
- `projects/frontend/src/utils/`
- `projects/frontend/src/store/apiClient.ts`
- `projects/frontend/src/shared/language/es.json`
- `projects/frontend/src/shared/language/en.json`
- `scripts/init-project.mjs`
- `scripts/init-project.test.mjs`

## Seguridad

Sin impacto de seguridad: solo cambia de dónde salen los textos.

## Fuera de alcance (no tocar)

- Pantallas de las features.

## Resultado esperado

Lo común se ve en ambos idiomas.

## Validación

1. Pruebas de componentes en `es` y una en `en` por grupo; fechas en ambos idiomas.
2. Cambiar el idioma actualiza las fechas visibles.
3. `node --test "scripts/**/*.test.mjs"` en verde.
