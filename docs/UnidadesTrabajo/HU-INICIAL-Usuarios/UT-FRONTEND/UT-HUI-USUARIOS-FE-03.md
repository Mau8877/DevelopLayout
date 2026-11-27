# UT-HUI-USUARIOS-FE-03 — Pantalla de usuarios

| Campo | Valor |
|-------|-------|
| **Código** | `UT-HUI-USUARIOS-FE-03` |
| **Nombre** | Pantalla de usuarios |
| **Historia** | HU-INICIAL-Usuarios-FE |
| **Depende de** | 01 |

## Antes de implementar

RUTAS_NAVEGACION_FRONTEND, ERRORES_LOGGING_FRONTEND y `BitacoraScreen`.

## Solicitud de negocio

El administrador ve las cuentas y encuentra la que busca.

## Objetivo

Ruta `/usuarios` con tabla paginada, búsqueda y filtros.

## Alcance

- `routes/_authenticated/_admin/usuarios.tsx` con `staticData.breadcrumb` (el menú ya apunta a `/usuarios`).
- `UsuariosScreen` con `DataTable`, `DataTablePagination` y `DataTableToolbar`: búsqueda con debounce y filtros de estado y rol. Cambiar un criterio vuelve a la página 1. Nada se escribe en la URL.
- Columnas: correo, roles (distintivos), estado (Activa/Inactiva), «Invitación pendiente» y fecha de alta.
- Estados de carga, vacío y error con «Reintentar».

**Archivos esperados (crear/tocar):**

- `projects/frontend/src/routes/_authenticated/_admin/usuarios.tsx`
- `projects/frontend/src/routeTree.gen.ts`
- `projects/frontend/src/features/usuarios/screens/UsuariosScreen.tsx`
- `projects/frontend/src/features/usuarios/components/columnasUsuario.tsx`
- `projects/frontend/src/features/usuarios/tests/UsuariosScreen.test.tsx`
- `projects/frontend/src/features/usuarios/index.ts`

## Seguridad

La ruta cae bajo el guard de administrador; la autorización real es del backend. El correo se muestra como texto.

## Fuera de alcance (no tocar)

- Crear y acciones por fila (UT 04 y 05).

## Resultado esperado

El listado funciona con búsqueda, filtros y paginación.

## Validación

1. Pruebas de columnas, criterios (y vuelta a la página 1), vacío y error con reintento.
2. Acceso denegado: un usuario sin `SUPER_ADMIN` que abre `/usuarios` termina en `/panel`.
3. `pnpm exec vite build`, `pnpm build` y `pnpm lint` sin errores.
