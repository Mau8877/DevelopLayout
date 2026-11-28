import { createFileRoute } from '@tanstack/react-router'
import { useTranslation } from 'react-i18next'
import { CambiarPasswordScreen } from '@/features/auth'
import { LayoutPorRol } from '@/layouts'

export const Route = createFileRoute('/_authenticated/change-password')({
  component: CambiarPasswordPage,
})

function CambiarPasswordPage() {
  const { usuario } = Route.useRouteContext()
  const { t } = useTranslation('auth')

  return (
    <LayoutPorRol title={t('cambiarPassword.titulo')} usuario={usuario}>
      <CambiarPasswordScreen />
    </LayoutPorRol>
  )
}
