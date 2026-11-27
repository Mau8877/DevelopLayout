import { useForm } from '@tanstack/react-form'
import { Link } from '@tanstack/react-router'
import { CenteredCardLayout } from '@/components/layout'
import { Alert } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { FormField } from '@/components/ui/form-field'
import { Input } from '@/components/ui/input'
import { ApiError } from '@/store'
import { primerError } from '@/utils/primerError'
import { validarAlSalir } from '@/utils/validarAlSalir'
import { useForgotPassword } from '../api/useForgotPassword'
import { forgotPasswordSchema } from '../schemas/forgotPasswordSchema'

const MENSAJE_GENERICO = 'No pudimos enviar el pedido. Intentá de nuevo.'

export function ForgotPasswordScreen() {
  const pedido = useForgotPassword()

  const form = useForm({
    defaultValues: { email: '' },
    validators: { onChange: forgotPasswordSchema },
    onSubmit: async ({ value }) => {
      try {
        await pedido.mutateAsync(value)
      } catch {
        // * El error queda en pedido.error y se muestra abajo.
      }
    },
  })

  const limitado = pedido.error instanceof ApiError && pedido.error.status === 429

  return (
    <CenteredCardLayout titulo="Recuperar contraseña" descripcion="Ingresá el correo de tu cuenta">
      {pedido.isSuccess ? (
        // * No afirma que se envió: la web no sabe (ni debe saber) si la cuenta existe.
        <Alert role="status">
          Si hay una cuenta con ese correo, te enviamos un enlace para restablecer la contraseña. Revisá también
          la carpeta de spam.
        </Alert>
      ) : (
        <form
          noValidate
          className="flex flex-col gap-4"
          onSubmit={(event) => {
            event.preventDefault()
            event.stopPropagation()
            void form.handleSubmit()
          }}
        >
          {pedido.isError && (
            <Alert variant={limitado ? 'warning' : 'destructive'}>
              {pedido.error instanceof ApiError ? pedido.error.message : MENSAJE_GENERICO}
            </Alert>
          )}

          <form.Field name="email">
            {(field) => (
              <FormField
                id="email"
                label="Correo"
                error={field.state.meta.isTouched ? primerError(field.state.meta.errors) : undefined}
              >
                {(controlProps) => (
                  <Input
                    {...controlProps}
                    name={field.name}
                    type="email"
                    autoComplete="username"
                    value={field.state.value}
                    onBlur={validarAlSalir(field)}
                    onChange={(event) => field.handleChange(event.target.value)}
                  />
                )}
              </FormField>
            )}
          </form.Field>

          <Button type="submit" disabled={pedido.isPending}>
            {pedido.isPending ? 'Enviando…' : 'Enviar enlace'}
          </Button>
        </form>
      )}

      <p className="mt-4 text-center text-sm">
        <Link to="/login" className="text-primary underline underline-offset-4">
          Volver a iniciar sesión
        </Link>
      </p>
    </CenteredCardLayout>
  )
}
