# UT-HUI-SESIONESADMIN-FE-02 — Pantalla Sesiones de usuarios (admin)

| Campo | Valor |
|-------|-------|
| **Código** | `UT-HUI-SESIONESADMIN-FE-02` |
| **Nombre** | Pantalla Sesiones de usuarios (admin) |
| **Historia** | HU-INICIAL-SesionesAdmin-FE |
| **Depende de** | UT-HUI-SESIONESADMIN-FE-01 |

## Antes de implementar

Leer `frontend/RUTAS_NAVEGACION_FRONTEND.md` y `.claude/rules/SEGURIDAD_OWASP.md`.

## Solicitud de negocio

El administrador necesita una interfaz.

## Objetivo

Existe la ruta del grupo admin con búsqueda y acciones.

## Alcance

- `screens/SesionesAdminScreen.tsx`: campo de correo exacto, tabla de sesiones, "Cerrar" y "Cerrar todas" con confirmación, y copiar el código del usuario.
- `routes/_authenticated/_admin.tsx`: guard (`SUPER_ADMIN`, si no → `/panel`) y `AdminLayout`.
- `AppSidebar` navega con `Link`.
- `routes/_authenticated/_admin/sesiones-usuarios.tsx` y entrada en `adminNavItems`.

**Archivos esperados (crear/tocar):**

- `projects/frontend/src/features/auth/screens/SesionesAdminScreen.tsx`
- `projects/frontend/src/routes/_authenticated/_admin/sesiones-usuarios.tsx`
- `projects/frontend/src/layouts/admin/adminNavItems.ts`
- `projects/frontend/src/routes/_authenticated/_admin.tsx`
- `projects/frontend/src/components/layout/AppSidebar.tsx`

## Fuera de alcance (no tocar)

- Mobile (el panel de administración es web).

## Resultado esperado

El admin busca y cierra sesiones.

## Validación

1. Manual con admin; un usuario normal no ve la entrada (y el backend igual lo rechaza).
2. `pnpm lint` y `pnpm build` en verde.

## Seguridad

A01: ocultar no es seguridad; el backend autoriza.
