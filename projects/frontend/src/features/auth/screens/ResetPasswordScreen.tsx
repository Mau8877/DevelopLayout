import { useTranslation } from 'react-i18next'
import { Link, useNavigate } from '@tanstack/react-router'
import { CenteredCardLayout } from '@/components/layout'
import { Alert } from '@/components/ui/alert'
import { toastSuccess } from '@/components/ui/toast'
import { ApiError } from '@/store'
import { useResetPassword } from '../api'
import { FormularioNuevaPassword } from '../components'

// * Errores que hablan de la contraseña elegida: el enlace sigue sirviendo.
const DE_LA_CONTRASENA = ['ERR_AUTH_06', 'ERR_SYS_01']

export function ResetPasswordScreen({ token }: { token: string | null }) {
  const { t } = useTranslation('auth')
  const navigate = useNavigate()
  const cambio = useResetPassword()

  if (!token || (cambio.error instanceof ApiError && cambio.error.code === 'ERR_AUTH_05')) {
    return (
      <CenteredCardLayout titulo={t('restablecer.enlaceTitulo')}>
        <Alert variant="warning">{t('restablecer.enlaceTexto')}</Alert>
        <p className="mt-4 text-center text-sm">
          <Link to="/forgot-password" className="text-primary underline underline-offset-4">
            {t('restablecer.pedirOtro')}
          </Link>
        </p>
      </CenteredCardLayout>
    )
  }

  return (
    <CenteredCardLayout titulo={t('restablecer.titulo')} descripcion={t('restablecer.descripcion')}>
      <FormularioNuevaPassword
        onEnviar={async (password) => {
          await cambio.mutateAsync({ token, password })
          toastSuccess(t('restablecer.exitoTitulo'), t('restablecer.exitoTexto'))
          await navigate({ to: '/login' })
        }}
        isPending={cambio.isPending}
        error={cambio.error}
        onCorregir={() => cambio.reset()}
        codigosDeLaPassword={DE_LA_CONTRASENA}
        mensajeGenerico={t('restablecer.error')}
        textoBoton={t('restablecer.boton')}
      />
    </CenteredCardLayout>
  )
}
