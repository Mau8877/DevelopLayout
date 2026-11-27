import { useForm } from '@tanstack/react-form'
import { Link, useNavigate } from '@tanstack/react-router'
import { CenteredCardLayout } from '@/components/layout'
import { Alert } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { FormField } from '@/components/ui/form-field'
import { Input } from '@/components/ui/input'
import { toastSuccess } from '@/components/ui/toast'
import { ApiError } from '@/store'
import { primerError } from '@/utils/primerError'
import { validarAlSalir } from '@/utils/validarAlSalir'
import { useResetPassword } from '../api/useResetPassword'
import { resetPasswordSchema } from '../schemas/resetPasswordSchema'

const MENSAJE_GENERICO = 'No pudimos cambiar la contraseña. Intentá de nuevo.'
const CAMPOS = [
  { name: 'password', label: 'Contraseña nueva' },
  { name: 'confirmacion', label: 'Repetí la contraseña' },
] as const

// * Errores que hablan de la contraseña elegida: van bajo el campo y el enlace
// * sigue sirviendo.
const DE_LA_CONTRASENA = ['ERR_AUTH_06', 'ERR_SYS_01']

export function ResetPasswordScreen({ token }: { token: string | null }) {
  const navigate = useNavigate()
  const cambio = useResetPassword()
  const error = cambio.error instanceof ApiError ? cambio.error : null

  const form = useForm({
    defaultValues: { password: '', confirmacion: '' },
    validators: { onChange: resetPasswordSchema },
    onSubmit: async ({ value }) => {
      if (!token) return
      try {
        await cambio.mutateAsync({ token, password: value.password })
        toastSuccess('Contraseña actualizada', 'Ya podés iniciar sesión')
        await navigate({ to: '/login' })
      } catch {
        // * El error queda en cambio.error y se muestra abajo.
      }
    },
  })

  if (!token || error?.code === 'ERR_AUTH_05') {
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

  const errorDeContrasena = error && DE_LA_CONTRASENA.includes(error.code) ? error.message : undefined

  return (
    <CenteredCardLayout titulo="Elegí una contraseña nueva" descripcion="Se cerrarán tus sesiones abiertas">
      <form
        noValidate
        className="flex flex-col gap-4"
        onSubmit={(event) => {
          event.preventDefault()
          event.stopPropagation()
          void form.handleSubmit()
        }}
      >
        {cambio.isError && !errorDeContrasena && (
          <Alert variant={error?.status === 429 ? 'warning' : 'destructive'}>
            {error ? error.message : MENSAJE_GENERICO}
          </Alert>
        )}

        {CAMPOS.map(({ name, label }) => (
          <form.Field key={name} name={name}>
            {(field) => (
              <FormField
                id={name}
                label={label}
                error={
                  (field.state.meta.isTouched ? primerError(field.state.meta.errors) : undefined) ??
                  (name === 'password' ? errorDeContrasena : undefined)
                }
              >
                {(controlProps) => (
                  <Input
                    {...controlProps}
                    name={field.name}
                    type="password"
                    autoComplete="new-password"
                    value={field.state.value}
                    onBlur={validarAlSalir(field)}
                    onChange={(event) => {
                      // * Al corregir, el rechazo del servidor deja de aplicar.
                      if (cambio.isError) cambio.reset()
                      field.handleChange(event.target.value)
                    }}
                  />
                )}
              </FormField>
            )}
          </form.Field>
        ))}

        <Button type="submit" disabled={cambio.isPending}>
          {cambio.isPending ? 'Guardando…' : 'Cambiar contraseña'}
        </Button>
      </form>
    </CenteredCardLayout>
  )
}
