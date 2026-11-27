import { Link, useNavigate } from '@tanstack/react-router'
import { CenteredCardLayout } from '@/components/layout'
import { Alert } from '@/components/ui/alert'
import { toastSuccess } from '@/components/ui/toast'
import { ApiError } from '@/store'
import { useResetPassword } from '../api/useResetPassword'
import { FormularioNuevaPassword } from '../components/FormularioNuevaPassword'

// * Errores que hablan de la contraseña elegida: el enlace sigue sirviendo.
const DE_LA_CONTRASENA = ['ERR_AUTH_06', 'ERR_SYS_01']

export function ResetPasswordScreen({ token }: { token: string | null }) {
  const navigate = useNavigate()
  const cambio = useResetPassword()

  if (!token || (cambio.error instanceof ApiError && cambio.error.code === 'ERR_AUTH_05')) {
    return (
      <CenteredCardLayout titulo="El enlace no sirve">
        <Alert variant="warning">
          El enlace no es válido, ya se usó o venció. Si recargaste la página, abrilo de nuevo desde el correo o
          pedí otro.
        </Alert>
        <p className="mt-4 text-center text-sm">
          <Link to="/forgot-password" className="text-primary underline underline-offset-4">
            Pedir un enlace nuevo
          </Link>
        </p>
      </CenteredCardLayout>
    )
  }

  return (
    <CenteredCardLayout titulo="Elegí una contraseña nueva" descripcion="Se cerrarán tus sesiones abiertas">
      <FormularioNuevaPassword
        onEnviar={async (password) => {
          await cambio.mutateAsync({ token, password })
          toastSuccess('Contraseña actualizada', 'Ya podés iniciar sesión')
          await navigate({ to: '/login' })
        }}
        isPending={cambio.isPending}
        error={cambio.error}
        onCorregir={() => cambio.reset()}
        codigosDeLaPassword={DE_LA_CONTRASENA}
        mensajeGenerico="No pudimos cambiar la contraseña. Intentá de nuevo."
        textoBoton="Cambiar contraseña"
      />
    </CenteredCardLayout>
  )
}
