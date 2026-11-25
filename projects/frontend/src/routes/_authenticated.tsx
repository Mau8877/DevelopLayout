import { createFileRoute, Outlet, redirect } from '@tanstack/react-router'
import { ensureSession } from '@/features/auth'
import { queryClient } from '@/store'

// Layout "pathless" (prefijo `_`) que envuelve todas las rutas privadas.
// Ninguna ruta nueva que necesite sesión se valida a mano por su cuenta:
// se agrega como hija de este archivo (carpeta `_authenticated/`) y hereda
// este guard. Ver .claude/rules/frontend/RUTAS_NAVEGACION_FRONTEND.md.
//
// * El guard es solo UX: pregunta al backend (/auth/me) quién es el usuario y
// * redirige al login si no hay sesión. La autorización real la hace el
// * backend en cada request.
export const Route = createFileRoute('/_authenticated')({
  beforeLoad: async () => {
    const usuario = await ensureSession(queryClient)
    if (!usuario) {
      throw redirect({ to: '/login' })
    }
    // Disponible para las rutas hijas (ej. para elegir el layout por rol).
    return { usuario }
  },
  component: AuthenticatedLayout,
})

function AuthenticatedLayout() {
  return <Outlet />
}
