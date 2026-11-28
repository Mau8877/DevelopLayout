# UT-HUI-IDIOMAS-FE-05 — Migrar usuarios, bitácora, inicio y rutas

| Campo | Valor |
|-------|-------|
| **Código** | `UT-HUI-IDIOMAS-FE-05` |
| **Nombre** | Migrar usuarios, bitácora, inicio y rutas |
| **Historia** | HU-INICIAL-Idiomas-FE |
| **Depende de** | 03, 04 |

## Antes de implementar

IDIOMAS.md e IDIOMAS_FRONTEND.

## Solicitud de negocio

La administración completa la traducción de la web.

## Objetivo

El resto de la web sin texto fijo.

## Alcance

- `language/{es,en}.json` en `usuarios`, `bitacora` y `home` (ya existe la carpeta vacía de `usuarios`).
- Títulos de rutas (`staticData.breadcrumb`) como claves; opciones de filtros y etiquetas de roles y estados.

**Archivos esperados (crear/tocar):**

- `projects/frontend/src/features/usuarios/`
- `projects/frontend/src/features/bitacora/`
- `projects/frontend/src/features/home/`
- `projects/frontend/src/routes/`
- `projects/frontend/src/i18next.d.ts`

## Seguridad

No cambia ninguna petición ni permiso. El texto buscado sigue fuera de URL y caché.

## Fuera de alcance (no tocar)

- Cambiar el diseño de las pantallas.

## Resultado esperado

Toda la web está en los dos idiomas.

## Validación

1. Cada pantalla tiene una prueba que la muestra en `en`.
2. `pnpm test`, `pnpm build` y `pnpm lint` en verde.
