import { createFileRoute } from '@tanstack/react-router'
import { LayoutPorRol } from '@/layouts'

// * Pantalla mínima: su contenido lo reemplaza la primera feature real detrás del login.
export const Route = createFileRoute('/_authenticated/panel')({
  component: PanelPage,
})

function PanelPage() {
  const { usuario } = Route.useRouteContext()

  return (
    <LayoutPorRol title="Panel" usuario={usuario}>
      <p className="text-muted-foreground">Todavía no hay contenido en el panel.</p>
    </LayoutPorRol>
  )
}
