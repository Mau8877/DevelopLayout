import { Link, useNavigate } from '@tanstack/react-router'
import { CenteredCardLayout } from '@/components/layout'
import { Alert } from '@/components/ui/alert'
import { toastSuccess } from '@/components/ui/toast'
import { ApiError } from '@/store'
import { useAceptarInvitacion } from '../api/useAceptarInvitacion'
import { FormularioNuevaPassword } from '../components/FormularioNuevaPassword'

export function AceptarInvitacionScreen({ token }: { token: string | null }) {
  const navigate = useNavigate()
  const aceptar = useAceptarInvitacion()

  if (!token || (aceptar.error instanceof ApiError && aceptar.error.code === 'ERR_AUTH_11')) {
    return (
      <CenteredCardLayout titulo="La invitación no sirve">
        <Alert variant="warning">
          La invitación no es válida, ya se usó o venció. Si recargaste la página, abrila de nuevo desde el
          correo. Si no funciona, pedile al administrador que te envíe otra.
        </Alert>
        <p className="mt-4 text-center text-sm">
          <Link to="/login" className="text-primary underline underline-offset-4">
            Ir al inicio de sesión
          </Link>
        </p>
      </CenteredCardLayout>
    )
  }

  return (
    <CenteredCardLayout titulo="Activá tu cuenta" descripcion="Elegí la contraseña con la que vas a entrar">
      <FormularioNuevaPassword
        onEnviar={async (password) => {
          await aceptar.mutateAsync({ token, password })
          toastSuccess('Cuenta activada', 'Ya podés iniciar sesión')
          await navigate({ to: '/login' })
        }}
        isPending={aceptar.isPending}
        error={aceptar.error}
        onCorregir={() => aceptar.reset()}
        codigosDeLaPassword={['ERR_SYS_01']}
        mensajeGenerico="No pudimos activar la cuenta. Intentá de nuevo."
        textoBoton="Activar cuenta"
      />
    </CenteredCardLayout>
  )
}
