import { useForm } from '@tanstack/react-form'
import { Alert } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { FormField } from '@/components/ui/form-field'
import { Input } from '@/components/ui/input'
import { ApiError } from '@/store'
import { primerError } from '@/utils/primerError'
import { validarAlSalir } from '@/utils/validarAlSalir'
import { resetPasswordSchema } from '../schemas/resetPasswordSchema'

const CAMPOS = [
  { name: 'password', label: 'Contraseña nueva' },
  { name: 'confirmacion', label: 'Repetí la contraseña' },
] as const

export interface FormularioNuevaPasswordProps {
  /** Si falla, el error se pasa de vuelta por `error`. */
  onEnviar: (password: string) => Promise<void>
  isPending: boolean
  error: unknown
  /** Se llama al corregir: el rechazo del servidor deja de aplicar. */
  onCorregir: () => void
  /** Códigos que hablan de la contraseña elegida: van bajo el campo. */
  codigosDeLaPassword: string[]
  mensajeGenerico: string
  textoBoton: string
}

/** Contraseña y confirmación, para restablecerla o para aceptar una invitación. */
export function FormularioNuevaPassword({
  onEnviar,
  isPending,
  error,
  onCorregir,
  codigosDeLaPassword,
  mensajeGenerico,
  textoBoton,
}: FormularioNuevaPasswordProps) {
  const rechazo = error instanceof ApiError ? error : null
  const errorDeContrasena = rechazo && codigosDeLaPassword.includes(rechazo.code) ? rechazo.message : undefined

  const form = useForm({
    defaultValues: { password: '', confirmacion: '' },
    validators: { onChange: resetPasswordSchema },
    onSubmit: async ({ value }) => {
      try {
        await onEnviar(value.password)
      } catch {
        // * El error llega por la prop `error` y se muestra abajo.
      }
    },
  })

  return (
    <form
      noValidate
      className="flex flex-col gap-4"
      onSubmit={(event) => {
        event.preventDefault()
        event.stopPropagation()
        void form.handleSubmit()
      }}
    >
      {Boolean(error) && !errorDeContrasena && (
        <Alert variant={rechazo?.status === 429 ? 'warning' : 'destructive'}>
          {rechazo ? rechazo.message : mensajeGenerico}
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
                    if (error) onCorregir()
                    field.handleChange(event.target.value)
                  }}
                />
              )}
            </FormField>
          )}
        </form.Field>
      ))}

      <Button type="submit" disabled={isPending}>
        {isPending ? 'Guardando…' : textoBoton}
      </Button>
    </form>
  )
}
