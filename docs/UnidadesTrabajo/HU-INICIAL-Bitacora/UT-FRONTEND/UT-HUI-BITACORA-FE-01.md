# UT-HUI-BITACORA-FE-01 — Hooks y filtros de bitácora

| Campo | Valor |
|-------|-------|
| **Código** | `UT-HUI-BITACORA-FE-01` |
| **Nombre** | Hooks y filtros de bitácora |
| **Historia** | HU-INICIAL-Bitacora-FE |
| **Depende de** | — |

## Antes de implementar

Leer `frontend/TANSTACK_QUERY_FRONTEND.md`, `frontend/FORMULARIOS_ZOD_TANSTACK_FRONTEND.md` y `.claude/rules/SEGURIDAD_OWASP.md`.

## Solicitud de negocio

La tabla necesita datos y filtros validados.

## Objetivo

Existen `useBitacora` y el schema de filtros.

## Alcance

- `features/bitacora/api/keys.ts` y `useBitacora.ts` (`['bitacora','list',params]`, `placeholderData` para paginar sin parpadeo).
- `schemas/bitacoraFiltrosSchema.ts` (rango de fechas coherente).
- `types/bitacora.ts`.

**Archivos esperados (crear/tocar):**

- `projects/frontend/src/features/bitacora/api/keys.ts`
- `projects/frontend/src/features/bitacora/api/useBitacora.ts`
- `projects/frontend/src/features/bitacora/schemas/bitacoraFiltrosSchema.ts`
- `projects/frontend/src/features/bitacora/types/bitacora.ts`
- `projects/frontend/src/features/bitacora/index.ts`

## Fuera de alcance (no tocar)

- Pantalla.

## Resultado esperado

El hook consulta con filtros.

## Validación

1. `pnpm test`: schema y hook con `apiClient` mockeado.

## Seguridad

A05: validación de filtros.
