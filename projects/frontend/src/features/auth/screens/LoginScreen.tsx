import { useForm } from '@tanstack/react-form'
import { useNavigate } from '@tanstack/react-router'
import { Alert } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { FormField } from '@/components/ui/form-field'
import { Input } from '@/components/ui/input'
import { CenteredCardLayout } from '@/components/layout'
import { ApiError } from '@/store'
import { useLogin } from '../api/useLogin'
import { loginSchema } from '../schemas/loginSchema'

const MENSAJE_GENERICO = 'No pudimos iniciar sesión. Intentá de nuevo.'

function mensajeDelError(error: unknown): string {
  return error instanceof ApiError ? error.message : MENSAJE_GENERICO
}

// * Los errores de validación llegan como strings o como objetos con `message`
// * (Standard Schema); se normalizan al primer mensaje.
function primerError(errors: unknown[]): string | undefined {
  const first = errors[0]
  if (typeof first === 'string') return first
  if (first && typeof first === 'object' && 'message' in first) return String((first as { message: unknown }).message)
  return undefined
}

export function LoginScreen() {
  const navigate = useNavigate()
  const login = useLogin()

  const form = useForm({
    defaultValues: { email: '', password: '' },
    validators: { onChange: loginSchema, onBlur: loginSchema },
    onSubmit: async ({ value }) => {
      try {
        await login.mutateAsync(value)
        await navigate({ to: '/panel' })
      } catch {
        // * El error queda en login.error y se muestra abajo; nada que hacer acá.
      }
    },
  })

  const bloqueada = login.error instanceof ApiError && login.error.code === 'ERR_AUTH_02'

  return (
    <CenteredCardLayout titulo="Iniciar sesión" descripcion="Ingresá con tu correo y contraseña">
      <form
        noValidate
        className="flex flex-col gap-4"
        onSubmit={(event) => {
          event.preventDefault()
          event.stopPropagation()
          void form.handleSubmit()
        }}
      >
        {login.isError && (
          <Alert variant={bloqueada ? 'warning' : 'destructive'}>{mensajeDelError(login.error)}</Alert>
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
                  onBlur={field.handleBlur}
                  onChange={(event) => field.handleChange(event.target.value)}
                />
              )}
            </FormField>
          )}
        </form.Field>

        <form.Field name="password">
          {(field) => (
            <FormField
              id="password"
              label="Contraseña"
              error={field.state.meta.isTouched ? primerError(field.state.meta.errors) : undefined}
            >
              {(controlProps) => (
                <Input
                  {...controlProps}
                  name={field.name}
                  type="password"
                  autoComplete="current-password"
                  value={field.state.value}
                  onBlur={field.handleBlur}
                  onChange={(event) => field.handleChange(event.target.value)}
                />
              )}
            </FormField>
          )}
        </form.Field>

        {/* Deshabilitado mientras envía: evita el doble envío (más fuerte que un debounce). */}
        <Button type="submit" disabled={login.isPending}>
          {login.isPending ? 'Ingresando…' : 'Ingresar'}
        </Button>
      </form>
    </CenteredCardLayout>
  )
}
