# UT-HUI-LOGIN-FE-05 — Guard de rutas autenticadas y redirección por rol

| Campo | Valor |
|-------|-------|
| **Código** | `UT-HUI-LOGIN-FE-05` |
| **Nombre** | Guard de rutas autenticadas y redirección por rol |
| **Historia** | HU-INICIAL-Login-FE |
| **Depende de** | UT-HUI-LOGIN-FE-03, UT-HUI-LOGIN-FE-04 |

## Antes de implementar

Leer `frontend/RUTAS_NAVEGACION_FRONTEND.md` y `.claude/rules/SEGURIDAD_OWASP.md`.

## Solicitud de negocio

Las rutas privadas no deben mostrarse sin sesión; la autorización real es del backend.

## Objetivo

`_authenticated.tsx` valida la sesión con `/auth/me` y redirige a `/login` si no hay.

## Alcance

- `routes/_authenticated.tsx`: `beforeLoad` con `ensureQueryData` de la sesión; sin sesión → `throw redirect({ to: '/login' })`.
- Expone el usuario al contexto de las rutas hijas.
- El grupo de rutas (`_admin`, `_usuario`) decide el layout según los roles.
- `panel.tsx` se mantiene como hijo para no romper el generador.

**Archivos esperados (crear/tocar):**

- `projects/frontend/src/routes/_authenticated.tsx`
- `projects/frontend/src/routes/_authenticated/panel.tsx`

## Fuera de alcance (no tocar)

- Ocultar botones como control de seguridad.
- Renovación de sesión (HU-INICIAL-Refresh).

## Resultado esperado

Una ruta privada sin sesión lleva a `/login`; con sesión carga.

## Validación

1. Sin cookies, `/panel` redirige a `/login`; tras login abre `/panel`; recargar mantiene la sesión.
2. `pnpm test`: guard con sesión mockeada.

## Seguridad

A01: el guard es de UX; el backend autoriza cada request.
