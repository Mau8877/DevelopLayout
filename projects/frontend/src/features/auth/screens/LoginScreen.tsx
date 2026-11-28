import { useTranslation } from 'react-i18next'
import { useForm } from '@tanstack/react-form'
import { Link, useNavigate } from '@tanstack/react-router'
import { Alert } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { FormField } from '@/components/ui/form-field'
import { Input } from '@/components/ui/input'
import { CenteredCardLayout } from '@/components/layout'
import { ApiError } from '@/store'
import { primerError } from '@/utils'
import { validarAlSalir } from '@/utils'
import { useLogin } from '../api'
import { loginSchema } from '../schemas'

export function LoginScreen() {
  const { t } = useTranslation('auth')
  const navigate = useNavigate()
  const login = useLogin()

  const form = useForm({
    defaultValues: { email: '', password: '' },
    validators: { onChange: loginSchema },
    onSubmit: async ({ value }) => {
      try {
        await login.mutateAsync(value)
        await navigate({ to: '/dashboard' })
      } catch {
        // * El error queda en login.error y se muestra abajo; nada que hacer acá.
      }
    },
  })

  const bloqueada = login.error instanceof ApiError && login.error.code === 'ERR_AUTH_02'

  return (
    <CenteredCardLayout titulo={t('login.titulo')} descripcion={t('login.descripcion')}>
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
          <Alert variant={bloqueada ? 'warning' : 'destructive'}>{login.error instanceof ApiError ? login.error.message : t('login.error')}</Alert>
        )}

        <form.Field name="email">
          {(field) => (
            <FormField
              id="email"
              label={t('comun.correo')}
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

        <form.Field name="password">
          {(field) => (
            <FormField
              id="password"
              label={t('comun.password')}
              error={field.state.meta.isTouched ? primerError(field.state.meta.errors) : undefined}
            >
              {(controlProps) => (
                <Input
                  {...controlProps}
                  name={field.name}
                  type="password"
                  autoComplete="current-password"
                  value={field.state.value}
                  onBlur={validarAlSalir(field)}
                  onChange={(event) => field.handleChange(event.target.value)}
                />
              )}
            </FormField>
          )}
        </form.Field>

        {/* Deshabilitado mientras envía: evita el doble envío (más fuerte que un debounce). */}
        <Button type="submit" disabled={login.isPending}>
          {login.isPending ? t('login.entrando') : t('login.entrar')}
        </Button>

        <p className="text-center text-sm">
          <Link to="/forgot-password" className="text-primary underline underline-offset-4">
            {t('login.olvidaste')}
          </Link>
        </p>
      </form>
    </CenteredCardLayout>
  )
}
