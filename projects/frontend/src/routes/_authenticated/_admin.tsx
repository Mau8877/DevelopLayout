import { createFileRoute, Outlet, redirect, useMatches } from '@tanstack/react-router'
import { AdminLayout } from '@/layouts'

// * Layout pathless de las rutas de administrador. El guard es solo UX: cada
// * endpoint lo autoriza el backend.
export const Route = createFileRoute('/_authenticated/_admin')({
  beforeLoad: ({ context }) => {
    if (!context.usuario.roles.includes('SUPER_ADMIN')) {
      throw redirect({ to: '/panel' })
    }
  },
  component: AdminRoutes,
})

function AdminRoutes() {
  const { usuario } = Route.useRouteContext()
  // El título de cada pantalla es el `staticData.breadcrumb` de su ruta.
  const titulo = useMatches({ select: (matches) => matches.at(-1)?.staticData.breadcrumb })

  return (
    <AdminLayout title={titulo ?? 'Administración'} user={{ nombre: usuario.email, rol: 'Administrador' }}>
      <Outlet />
    </AdminLayout>
  )
}
