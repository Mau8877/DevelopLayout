import { useForm } from '@tanstack/react-form'
import { UserPlusIcon } from 'lucide-react'
import { Alert } from '@/components/ui/alert'
import { CheckboxGroupField } from '@/components/ui/checkbox-group-field'
import { FormField } from '@/components/ui/form-field'
import { Input } from '@/components/ui/input'
import { ModalLayout } from '@/components/ui/modal-layout'
import { toastSuccess, toastWarning } from '@/components/ui/toast'
import { primerError } from '@/utils/primerError'
import { validarAlSalir } from '@/utils/validarAlSalir'
import { useCrearUsuario } from '../api/useCrearUsuario'
import { crearUsuarioSchema } from '../schemas/crearUsuarioSchema'
import { mensajeDelError } from './mensajeDelError'
import { ROLES } from './roles'

const FORM_ID = 'crear-usuario'

/** Se monta al abrirse: cada alta empieza con el formulario vacío. */
export function CrearUsuarioModal({ onClose }: { onClose: () => void }) {
  const crear = useCrearUsuario()

  const form = useForm({
    defaultValues: { email: '', roles: [] as string[] },
    validators: { onChange: crearUsuarioSchema },
    onSubmit: async ({ value }) => {
      if (crear.isPending) return
      try {
        const creado = await crear.mutateAsync({ email: value.email.trim(), roles: value.roles })
        if (creado.invitacionEnviada) {
          toastSuccess('Usuario creado', 'Le enviamos la invitación por correo')
        } else {
          toastWarning(
            'La cuenta se creó, pero el correo no salió',
            'Reenviá la invitación desde la lista',
          )
        }
        onClose()
      } catch {
        // * El error queda en crear.error y se muestra arriba del formulario.
      }
    },
  })

  return (
    <ModalLayout
      open
      onOpenChange={(open) => !open && onClose()}
      title="Crear usuario"
      subtitle="Recibirá un correo para elegir su contraseña"
      icon={UserPlusIcon}
      formId={FORM_ID}
      confirmLabel="Crear"
      isSubmitting={crear.isPending}
    >
      <form
        id={FORM_ID}
        noValidate
        className="flex flex-col gap-4"
        onSubmit={(event) => {
          event.preventDefault()
          event.stopPropagation()
          void form.handleSubmit()
        }}
      >
        {crear.isError && <Alert variant="destructive">{mensajeDelError(crear.error)}</Alert>}

        <form.Field name="email">
          {(field) => (
            <FormField
              id="crear-email"
              label="Correo"
              error={field.state.meta.isTouched ? primerError(field.state.meta.errors) : undefined}
            >
              {(controlProps) => (
                <Input
                  {...controlProps}
                  name={field.name}
                  type="email"
                  autoComplete="off"
                  value={field.state.value}
                  onBlur={validarAlSalir(field)}
                  onChange={(event) => {
                    // * Al corregir, el rechazo del servidor deja de aplicar.
                    if (crear.isError) crear.reset()
                    field.handleChange(event.target.value)
                  }}
                />
              )}
            </FormField>
          )}
        </form.Field>

        <form.Field name="roles">
          {(field) => (
            <CheckboxGroupField
              id="crear-roles"
              legend="Roles"
              options={ROLES}
              value={field.state.value}
              onChange={(valor) => field.handleChange(valor)}
              error={field.state.meta.isTouched ? primerError(field.state.meta.errors) : undefined}
            />
          )}
        </form.Field>
      </form>
    </ModalLayout>
  )
}
