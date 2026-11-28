import { useTranslation } from 'react-i18next'
import { Link, useNavigate } from '@tanstack/react-router'
import { CenteredCardLayout } from '@/components/layout'
import { Alert } from '@/components/ui/alert'
import { toastSuccess } from '@/components/ui/toast'
import { ApiError } from '@/store'
import { useAceptarInvitacion } from '../api'
import { FormularioNuevaPassword } from '../components'

export function AceptarInvitacionScreen({ token }: { token: string | null }) {
  const { t } = useTranslation('auth')
  const navigate = useNavigate()
  const aceptar = useAceptarInvitacion()

  if (!token || (aceptar.error instanceof ApiError && aceptar.error.code === 'ERR_AUTH_11')) {
    return (
      <CenteredCardLayout titulo={t('invitacion.invalidaTitulo')}>
        <Alert variant="warning">{t('invitacion.invalidaTexto')}</Alert>
        <p className="mt-4 text-center text-sm">
          <Link to="/login" className="text-primary underline underline-offset-4">
            {t('invitacion.irALogin')}
          </Link>
        </p>
      </CenteredCardLayout>
    )
  }

  return (
    <CenteredCardLayout titulo={t('invitacion.titulo')} descripcion={t('invitacion.descripcion')}>
      <FormularioNuevaPassword
        onEnviar={async (password) => {
          await aceptar.mutateAsync({ token, password })
          toastSuccess(t('invitacion.exitoTitulo'), t('invitacion.exitoTexto'))
          await navigate({ to: '/login' })
        }}
        isPending={aceptar.isPending}
        error={aceptar.error}
        onCorregir={() => aceptar.reset()}
        codigosDeLaPassword={['ERR_SYS_01']}
        mensajeGenerico={t('invitacion.error')}
        textoBoton={t('invitacion.boton')}
      />
    </CenteredCardLayout>
  )
}
