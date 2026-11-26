# UT-HUI-SESIONESADMIN-FE-01 — Hooks de administración de sesiones

| Campo | Valor |
|-------|-------|
| **Código** | `UT-HUI-SESIONESADMIN-FE-01` |
| **Nombre** | Hooks de administración de sesiones |
| **Historia** | HU-INICIAL-SesionesAdmin-FE |
| **Depende de** | — |

## Antes de implementar

Leer `frontend/TANSTACK_QUERY_FRONTEND.md` y `.claude/rules/SEGURIDAD_OWASP.md`.

## Solicitud de negocio

La pantalla de admin necesita sus datos.

## Objetivo

Hooks de búsqueda y cierre.

## Alcance

- `api/useSesionesAdmin.ts` (búsqueda por correo), `useCerrarSesionAdmin.ts` y `useCerrarTodasAdmin.ts`; claves `['auth','admin','sesiones','list',{ busqueda, page, pageSize }]`, donde `busqueda` es un número de búsqueda: **el correo no entra en la clave**.

**Archivos esperados (crear/tocar):**

- `projects/frontend/src/features/auth/api/useSesionesAdmin.ts`
- `projects/frontend/src/features/auth/api/useCerrarSesionAdmin.ts`
- `projects/frontend/src/features/auth/api/useCerrarTodasAdmin.ts`
- `projects/frontend/src/features/auth/api/keys.ts` y `index.ts`

## Fuera de alcance (no tocar)

- Pantalla.

## Resultado esperado

Los hooks llaman a los endpoints de admin.

## Validación

1. `pnpm test` con `apiClient` mockeado.

## Seguridad

A01: sin lógica de permisos en el cliente.
