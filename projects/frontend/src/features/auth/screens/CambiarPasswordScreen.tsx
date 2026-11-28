import { useForm } from '@tanstack/react-form'
import { useTranslation } from 'react-i18next'
import { Alert } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { FormField } from '@/components/ui/form-field'
import { Input } from '@/components/ui/input'
import { toastSuccess } from '@/components/ui/toast'
import { ApiError } from '@/store'
import { primerError, validarAlSalir } from '@/utils'
import { useCambiarPassword } from '../api'
import { cambiarPasswordSchema } from '../schemas'

const CAMPOS = [
  { name: 'passwordActual', clave: 'cambiarPassword.actual', autoComplete: 'current-password' },
  { name: 'passwordNueva', clave: 'cambiarPassword.nueva', autoComplete: 'new-password' },
  { name: 'confirmacion', clave: 'cambiarPassword.confirmacion', autoComplete: 'new-password' },
] as const

// * Errores del servidor que hablan de un campo: van debajo de él.
const CAMPO_DEL_ERROR: Record<string, (typeof CAMPOS)[number]['name']> = {
  ERR_AUTH_12: 'passwordActual',
  ERR_AUTH_06: 'passwordNueva',
  ERR_SYS_01: 'passwordNueva',
}

export function CambiarPasswordScreen() {
  const { t } = useTranslation('auth')
  const cambio = useCambiarPassword()

  const rechazo = cambio.error instanceof ApiError ? cambio.error : null
  const campoRechazado = rechazo ? CAMPO_DEL_ERROR[rechazo.code] : undefined

  const form = useForm({
    defaultValues: { passwordActual: '', passwordNueva: '', confirmacion: '' },
    validators: { onChange: cambiarPasswordSchema },
    onSubmit: async ({ value }) => {
      if (cambio.isPending) return
      try {
        await cambio.mutateAsync({ passwordActual: value.passwordActual, passwordNueva: value.passwordNueva })
        toastSuccess(t('cambiarPassword.exitoTitulo'), t('cambiarPassword.exitoTexto'))
      } catch {
        // * El error queda en cambio.error y se muestra en el formulario.
      }
    },
  })

  return (
    <form
      noValidate
      className="flex max-w-sm flex-col gap-4"
      onSubmit={(event) => {
        event.preventDefault()
        event.stopPropagation()
        void form.handleSubmit()
      }}
    >
      <p className="text-sm text-muted-foreground">{t('cambiarPassword.aviso')}</p>

      {cambio.isError && !campoRechazado && (
        <Alert variant={rechazo?.status === 423 || rechazo?.status === 429 ? 'warning' : 'destructive'}>
          {rechazo ? rechazo.message : t('cambiarPassword.error')}
        </Alert>
      )}

      {CAMPOS.map(({ name, clave, autoComplete }) => (
        <form.Field key={name} name={name}>
          {(field) => (
            <FormField
              id={name}
              label={t(clave)}
              error={
                (field.state.meta.isTouched ? primerError(field.state.meta.errors) : undefined) ??
                (campoRechazado === name ? rechazo?.message : undefined)
              }
            >
              {(controlProps) => (
                <Input
                  {...controlProps}
                  name={field.name}
                  type="password"
                  autoComplete={autoComplete}
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
        {cambio.isPending ? t('nuevaPassword.guardando') : t('cambiarPassword.boton')}
      </Button>
    </form>
  )
}
