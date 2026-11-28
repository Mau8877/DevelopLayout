import { createFileRoute, Outlet, redirect, useMatches } from '@tanstack/react-router'
import { useTranslation } from 'react-i18next'
import { AdminLayout } from '@/layouts'
import { traducirMensaje } from '@/shared/language'

// * Layout pathless de las rutas de administrador. El guard es solo UX: cada
// * endpoint lo autoriza el backend.
export const Route = createFileRoute('/_authenticated/_admin')({
  beforeLoad: ({ context }) => {
    if (!context.usuario.roles.includes('SUPER_ADMIN')) {
      throw redirect({ to: '/dashboard' })
    }
  },
  component: AdminRoutes,
})

function AdminRoutes() {
  const { usuario } = Route.useRouteContext()
  const { t } = useTranslation('shared')
  // El título de cada pantalla es el `staticData.breadcrumb` de su ruta.
  const titulo = useMatches({ select: (matches) => matches.at(-1)?.staticData.breadcrumb })

  return (
    <AdminLayout
      title={traducirMensaje(titulo) ?? t('navegacion.administracion')}
      user={{ nombre: usuario.email, rol: t('rol.administrador') }}
    >
      <Outlet />
    </AdminLayout>
  )
}
