import { createFileRoute } from '@tanstack/react-router'
import { MisSesionesScreen } from '@/features/auth'
import { LayoutPorRol } from '@/layouts'

export const Route = createFileRoute('/_authenticated/mis-sesiones')({
  component: MisSesionesPage,
})

function MisSesionesPage() {
  const { usuario } = Route.useRouteContext()

  return (
    <LayoutPorRol title="Mis sesiones" usuario={usuario}>
      <MisSesionesScreen />
    </LayoutPorRol>
  )
}
