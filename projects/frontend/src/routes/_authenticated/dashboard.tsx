import { createFileRoute } from '@tanstack/react-router'
import { useTranslation } from 'react-i18next'
import { LayoutPorRol } from '@/layouts'

// * Pantalla mínima: su contenido lo reemplaza la primera feature real detrás del login.
export const Route = createFileRoute('/_authenticated/dashboard')({
  component: PanelPage,
})

function PanelPage() {
  const { usuario } = Route.useRouteContext()
  const { t } = useTranslation('shared')

  return (
    <LayoutPorRol title={t('navegacion.panel')} usuario={usuario}>
      <p className="text-muted-foreground">{t('panel.vacio')}</p>
    </LayoutPorRol>
  )
}
