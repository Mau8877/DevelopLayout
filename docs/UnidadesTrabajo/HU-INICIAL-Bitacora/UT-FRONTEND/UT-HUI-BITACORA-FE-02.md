# UT-HUI-BITACORA-FE-02 — Pantalla Bitácora (admin)

| Campo | Valor |
|-------|-------|
| **Código** | `UT-HUI-BITACORA-FE-02` |
| **Nombre** | Pantalla Bitácora (admin) |
| **Historia** | HU-INICIAL-Bitacora-FE |
| **Depende de** | UT-HUI-BITACORA-FE-01 |

## Antes de implementar

Leer `frontend/RUTAS_NAVEGACION_FRONTEND.md`, `frontend/TAILWIND_STYLES_FRONTEND.md` y `.claude/rules/SEGURIDAD_OWASP.md`.

## Solicitud de negocio

El administrador necesita auditar desde el panel.

## Objetivo

Existe la pantalla con tabla filtrable y paginada.

## Alcance

- `features/bitacora/screens/BitacoraScreen.tsx` con `DataTable`/`DataCards` (paginación del servidor), filtros por acción, módulo, resultado, usuario y fechas, y el detalle en un diálogo de solo lectura (texto, nunca HTML).
- `routes/_authenticated/_admin/bitacora.tsx` y entrada en `adminNavItems`.

**Archivos esperados (crear/tocar):**

- `projects/frontend/src/features/bitacora/screens/BitacoraScreen.tsx`
- `projects/frontend/src/routes/_authenticated/_admin/bitacora.tsx`
- `projects/frontend/src/layouts/admin/adminNavItems.ts`

## Fuera de alcance (no tocar)

- Mobile (el panel de administración es web).
- Exportar a archivo.

## Resultado esperado

El admin filtra y pagina la bitácora sin recargar.

## Validación

1. Manual con admin; `pnpm lint` y `pnpm build` en verde.

## Seguridad

A05: el detalle se muestra como texto.
