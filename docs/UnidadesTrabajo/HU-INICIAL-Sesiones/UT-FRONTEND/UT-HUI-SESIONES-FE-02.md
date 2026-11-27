# UT-HUI-SESIONES-FE-02 — Pantalla Mis sesiones

| Campo | Valor |
|-------|-------|
| **Código** | `UT-HUI-SESIONES-FE-02` |
| **Nombre** | Pantalla Mis sesiones |
| **Historia** | HU-INICIAL-Sesiones-FE |
| **Depende de** | UT-HUI-SESIONES-FE-01 |

## Antes de implementar

Leer `frontend/COMPONENTIZACION_FRONTEND.md`, `frontend/RUTAS_NAVEGACION_FRONTEND.md` y `frontend/TAILWIND_STYLES_FRONTEND.md`.

## Solicitud de negocio

Cualquier usuario debe poder llegar a sus sesiones desde el menú.

## Objetivo

Existe `/mis-sesiones` para todos los roles, con confirmación antes de cerrar.

## Alcance

- `screens/MisSesionesScreen.tsx`: tabla con el `DataTable` existente (dispositivo, cliente, IP, iniciada, último uso), distintivo «Esta sesión» en la actual, y **sin** botón de cierre en ella. Botón «Cerrar todas las demás», deshabilitado si no hay otras.
- Confirmación con `ModalDestructive` antes de cada cierre; toast de resultado; estados de carga, vacío y error con «Reintentar».
- Las columnas son las mismas que en la pantalla de administración: se extraen a `features/auth/components/columnasSesion.tsx` y **ambas** las usan (la de administración pasa a mostrar también el resumen de dispositivo, con el user agent completo en el `title`).
- Ruta `routes/_authenticated/mis-sesiones.tsx`. Es común a todos los roles: extraer a `layouts/LayoutPorRol.tsx` la elección de layout que hoy hace `/panel`, usarla en ambas rutas y actualizar la regla de rutas (el patrón para páginas comunes pasa a ser ese componente; las de un solo rol siguen agrupadas bajo su layout).
- «Mis sesiones» en `adminNavItems` y `usuarioNavItems`.

**Archivos esperados (crear/tocar):**

- `projects/frontend/src/features/auth/screens/MisSesionesScreen.tsx`
- `projects/frontend/src/features/auth/components/columnasSesion.tsx`
- `projects/frontend/src/features/auth/screens/SesionesAdminScreen.tsx`
- `projects/frontend/src/features/auth/index.ts`
- `projects/frontend/src/routes/_authenticated/mis-sesiones.tsx`
- `projects/frontend/src/routes/_authenticated/panel.tsx`
- `projects/frontend/src/layouts/LayoutPorRol.tsx`
- `projects/frontend/src/layouts/index.ts`
- `projects/frontend/src/layouts/admin/adminNavItems.ts`
- `projects/frontend/src/layouts/usuario/usuarioNavItems.ts`
- `projects/frontend/src/routeTree.gen.ts`
- `.claude/rules/frontend/RUTAS_NAVEGACION_FRONTEND.md`

## Seguridad

El guard `_authenticated` es solo experiencia de usuario: la autorización la hace el backend. Ocultar el botón de la actual no sustituye el 400 del servidor.

## Fuera de alcance (no tocar)

- Cambiar el comportamiento de la administración de sesiones.
- Arreglar los demás ítems de ejemplo del menú.

## Resultado esperado

El usuario ve y cierra sus sesiones desde la web.

## Validación

1. En el navegador con dos sesiones: cerrar la otra la deja fuera; la actual sigue.
2. `pnpm build` y `pnpm lint` en verde.
