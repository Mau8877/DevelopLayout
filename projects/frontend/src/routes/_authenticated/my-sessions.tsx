import { createFileRoute } from '@tanstack/react-router'
import { useTranslation } from 'react-i18next'
import { MisSesionesScreen } from '@/features/auth'
import { LayoutPorRol } from '@/layouts'

export const Route = createFileRoute('/_authenticated/my-sessions')({
  component: MisSesionesPage,
})

function MisSesionesPage() {
  const { usuario } = Route.useRouteContext()
  const { t } = useTranslation('shared')

  return (
    <LayoutPorRol title={t('navegacion.misSesiones')} usuario={usuario}>
      <MisSesionesScreen />
    </LayoutPorRol>
  )
}
