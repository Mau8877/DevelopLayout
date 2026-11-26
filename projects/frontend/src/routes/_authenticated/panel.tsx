import { createFileRoute } from '@tanstack/react-router'
import { AdminLayout, UsuarioLayout } from '@/layouts'

// * Pantalla mínima: monta el layout según el rol (solo UX, la autorización es del
// * backend). Su contenido lo reemplaza la primera feature real detrás del login.
export const Route = createFileRoute('/_authenticated/panel')({
  component: PanelPage,
})

function PanelPage() {
  const { usuario } = Route.useRouteContext()
  const esAdmin = usuario.roles.includes('SUPER_ADMIN')
  const Layout = esAdmin ? AdminLayout : UsuarioLayout

  return (
    <Layout title="Panel" user={{ nombre: usuario.email, rol: esAdmin ? 'Administrador' : 'Usuario' }}>
      <p className="text-muted-foreground">Todavía no hay contenido en el panel.</p>
    </Layout>
  )
}
