import { useForm } from '@tanstack/react-form'
import { ShieldIcon } from 'lucide-react'
import { useState } from 'react'
import { Alert } from '@/components/ui/alert'
import { CheckboxGroupField } from '@/components/ui/checkbox-group-field'
import { ModalConfirmacion } from '@/components/ui/modal-confirmacion'
import { ModalLayout } from '@/components/ui/modal-layout'
import { toastSuccess } from '@/components/ui/toast'
import { primerError } from '@/utils/primerError'
import { useCambiarRoles } from '../api/useCambiarRoles'
import { rolesSchema } from '../schemas/rolesSchema'
import type { Usuario } from '../types/usuario'
import { mensajeDelError } from './mensajeDelError'
import { ROLES } from './roles'

const FORM_ID = 'cambiar-roles'
const ROL_ADMIN = 'SUPER_ADMIN'

function mismoConjunto(a: string[], b: string[]): boolean {
  return a.length === b.length && a.every((valor) => b.includes(valor))
}

export interface RolesModalProps {
  usuario: Usuario
  /** La cuenta con la que se inició sesión. */
  esPropia: boolean
  onClose: () => void
}

/** Se monta al abrirse, con los roles actuales marcados. */
export function RolesModal({ usuario, esPropia, onClose }: RolesModalProps) {
  const cambiar = useCambiarRoles()
  const [porConfirmar, setPorConfirmar] = useState<string[] | null>(null)

  const form = useForm({
    defaultValues: { roles: usuario.roles },
    validators: { onChange: rolesSchema },
    onSubmit: ({ value }) => {
      if (mismoConjunto(value.roles, usuario.roles)) onClose()
      else setPorConfirmar(value.roles)
    },
  })

  async function confirmar(roles: string[]) {
    try {
      await cambiar.mutateAsync({ codigo: usuario.codigo, roles })
      toastSuccess('Roles actualizados')
      onClose()
    } catch {
      // * El error queda en cambiar.error y se muestra en el formulario.
    } finally {
      setPorConfirmar(null)
    }
  }

  // * Bloquear la casilla es comodidad: el backend rechaza quitarse ese rol.
  const opciones = ROLES.map((rol) =>
    esPropia && rol.value === ROL_ADMIN && usuario.roles.includes(ROL_ADMIN)
      ? { ...rol, disabled: true, descripcion: 'No podés quitarte tu propio rol de administrador' }
      : rol,
  )

  return (
    <>
      <ModalLayout
        open
        onOpenChange={(open) => !open && onClose()}
        title="Cambiar roles"
        subtitle={usuario.email}
        icon={ShieldIcon}
        formId={FORM_ID}
        confirmLabel="Guardar"
        isSubmitting={cambiar.isPending}
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
          {cambiar.isError && <Alert variant="destructive">{mensajeDelError(cambiar.error)}</Alert>}
          <form.Field name="roles">
            {(field) => (
              <CheckboxGroupField
                id="cambiar-roles-grupo"
                legend="Roles"
                options={opciones}
                value={field.state.value}
                onChange={(valor) => {
                  if (cambiar.isError) cambiar.reset()
                  field.handleChange(valor)
                }}
                error={primerError(field.state.meta.errors)}
              />
            )}
          </form.Field>
        </form>
      </ModalLayout>

      <ModalConfirmacion
        open={porConfirmar !== null}
        onOpenChange={(open) => !open && setPorConfirmar(null)}
        title="Confirmar el cambio de roles"
        description={
          esPropia
            ? 'Se cerrarán tus sesiones abiertas, también esta: vas a tener que iniciar sesión de nuevo.'
            : `Se cerrarán las sesiones abiertas de ${usuario.email}. Los roles nuevos rigen la próxima vez que entre.`
        }
        confirmLabel="Cambiar roles"
        isSubmitting={cambiar.isPending}
        onConfirm={() => porConfirmar && void confirmar(porConfirmar)}
      />
    </>
  )
}
