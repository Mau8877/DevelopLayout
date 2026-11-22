# UT-HUI-LOGOUT-FE-02 — Acción cerrar sesión en el AppShell

| Campo | Valor |
|-------|-------|
| **Código** | `UT-HUI-LOGOUT-FE-02` |
| **Nombre** | Acción cerrar sesión en el AppShell |
| **Historia** | HU-INICIAL-Logout-FE |
| **Depende de** | UT-HUI-LOGOUT-FE-01 |

## Antes de implementar

Leer `frontend/RUTAS_NAVEGACION_FRONTEND.md`, `frontend/TAILWIND_STYLES_FRONTEND.md` y `.claude/rules/SEGURIDAD_OWASP.md`.

## Solicitud de negocio

El usuario necesita una forma visible de cerrar sesión.

## Objetivo

El `AppShell` tiene la acción "Cerrar sesión" para todos los roles.

## Alcance

- Editar el `AppShell` de `components/layout/` para recibir un callback `onLogout` (no conoce roles ni hooks de feature); `AdminLayout` y `UsuarioLayout` le pasan `useLogout`.

**Archivos esperados (crear/tocar):**

- `projects/frontend/src/components/layout/AppShell.tsx`
- `projects/frontend/src/layouts/admin/AdminLayout.tsx`
- `projects/frontend/src/layouts/usuario/UsuarioLayout.tsx`

## Fuera de alcance (no tocar)

- Menú de perfil u otras opciones.

## Resultado esperado

Pulsar "Cerrar sesión" lleva a `/login`.

## Validación

1. Manual con el stack: al salir `/panel` ya no abre.
2. `pnpm lint` y `pnpm build` en verde.

## Seguridad

A01: el cierre depende del backend, no solo de ocultar UI.
