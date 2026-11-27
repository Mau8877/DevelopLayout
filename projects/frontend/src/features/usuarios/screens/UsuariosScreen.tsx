import { useMemo, useState } from 'react'
import { MailIcon, UserCheckIcon, UserPlusIcon, UserXIcon } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { ErrorState } from '@/components/ui/error-state'
import { ModalConfirmacion } from '@/components/ui/modal-confirmacion'
import {
  DataCards,
  DataTable,
  DataTablePagination,
  DataTableToolbar,
  RowActionButton,
  useDataTable,
} from '@/components/ui/tanstack-table'
import { toastError, toastSuccess, toastWarning } from '@/components/ui/toast'
import { useSession } from '@/features/auth'
import { debounce } from '@/utils/debounce'
import { useCambiarEstadoUsuario, type CambioDeEstado } from '../api/useCambiarEstadoUsuario'
import { useReenviarInvitacion } from '../api/useReenviarInvitacion'
import { nuevaBusqueda, SIN_BUSQUEDA, useUsuarios } from '../api/useUsuarios'
import { columnasUsuario } from '../components/columnasUsuario'
import { CrearUsuarioModal } from '../components/CrearUsuarioModal'
import { mensajeDelError } from '../components/mensajeDelError'
import { ROLES } from '../components/roles'
import type { EstadoUsuario, Usuario } from '../types/usuario'

const ESPERA_BUSQUEDA_MS = 300

const OPCIONES_ESTADO = [
  { value: 'ACTIVO', label: 'Activa' },
  { value: 'INACTIVO', label: 'Inactiva' },
]

export function UsuariosScreen() {
  const [texto, setTexto] = useState('')
  const [busqueda, setBusqueda] = useState(SIN_BUSQUEDA)
  const [estado, setEstado] = useState<EstadoUsuario | undefined>(undefined)
  const [rol, setRol] = useState<string | undefined>(undefined)
  const [page, setPage] = useState(1)
  const [pageSize, setPageSize] = useState(20)
  const [creando, setCreando] = useState(false)
  const [aDesactivar, setADesactivar] = useState<Usuario | null>(null)

  const yo = useSession().data?.codigo
  const consulta = useUsuarios({ busqueda, estado, rol, page, pageSize })
  const reenviar = useReenviarInvitacion()
  const cambiarEstado = useCambiarEstadoUsuario()

  const aplicarBusqueda = useMemo(
    () =>
      debounce((nuevo: string) => {
        setPage(1)
        setBusqueda(nuevaBusqueda(nuevo))
      }, ESPERA_BUSQUEDA_MS),
    [],
  )

  const cambiarTamanio = (nuevo: number) => {
    setPageSize(nuevo)
    setPage(1)
  }
  const { table } = useDataTable({
    data: consulta.data?.data ?? [],
    columns: columnasUsuario,
    meta: consulta.data?.meta,
    page,
    pageSize,
    onPageChange: setPage,
    onPageSizeChange: cambiarTamanio,
    getRowId: (usuario) => usuario.codigo,
  })

  async function cambiar(cambio: CambioDeEstado) {
    try {
      await cambiarEstado.mutateAsync(cambio)
      toastSuccess(cambio.accion === 'desactivar' ? 'Usuario desactivado' : 'Usuario reactivado')
    } catch (error) {
      toastError(mensajeDelError(error))
    } finally {
      setADesactivar(null)
    }
  }

  async function reenviarInvitacion(usuario: Usuario) {
    try {
      const { invitacionEnviada } = await reenviar.mutateAsync(usuario.codigo)
      if (invitacionEnviada) toastSuccess('Invitación reenviada')
      else toastWarning('No se pudo enviar el correo', 'Probá de nuevo en un rato')
    } catch (error) {
      toastError(mensajeDelError(error))
    }
  }

  const ocupado = reenviar.isPending || cambiarEstado.isPending

  // * Qué botones se muestran es comodidad: quien decide es el backend.
  const acciones = (usuario: Usuario) => (
    <>
      {usuario.estado === 'ACTIVO' && usuario.invitacionPendiente && (
        <RowActionButton
          icon={<MailIcon />}
          label="Reenviar invitación"
          disabled={ocupado}
          onClick={() => void reenviarInvitacion(usuario)}
        />
      )}
      {usuario.estado === 'ACTIVO' && usuario.codigo !== yo && (
        <RowActionButton
          icon={<UserXIcon />}
          label="Desactivar"
          variant="destructive"
          disabled={ocupado}
          onClick={() => setADesactivar(usuario)}
        />
      )}
      {usuario.estado === 'INACTIVO' && (
        <RowActionButton
          icon={<UserCheckIcon />}
          label="Reactivar"
          disabled={ocupado}
          onClick={() => void cambiar({ codigo: usuario.codigo, accion: 'reactivar' })}
        />
      )}
    </>
  )

  return (
    <div className="flex flex-col gap-4">
      <DataTableToolbar
        searchValue={texto}
        onSearchChange={(nuevo) => {
          setTexto(nuevo.slice(0, 255))
          aplicarBusqueda(nuevo.slice(0, 255))
        }}
        searchPlaceholder="Buscar por correo"
        filters={[
          {
            id: 'estado',
            label: 'Estado',
            options: OPCIONES_ESTADO,
            value: estado,
            onValueChange: (valor) => {
              setPage(1)
              setEstado(valor as EstadoUsuario | undefined)
            },
          },
          {
            id: 'rol',
            label: 'Rol',
            options: ROLES,
            value: rol,
            onValueChange: (valor) => {
              setPage(1)
              setRol(valor)
            },
          },
        ]}
        actions={
          <Button size="sm" onClick={() => setCreando(true)}>
            <UserPlusIcon data-icon="inline-start" />
            Crear usuario
          </Button>
        }
      />

      {consulta.isError ? (
        <ErrorState descripcion={mensajeDelError(consulta.error)} onReintentar={() => void consulta.refetch()} />
      ) : (
        <>
          <div className="hidden md:block">
            <DataTable
              table={table}
              isLoading={consulta.isPending}
              isError={false}
              emptyMessage="No hay usuarios con esos criterios"
              renderActions={(row) => acciones(row.original)}
            />
          </div>
          <div className="md:hidden">
            <DataCards
              table={table}
              isLoading={consulta.isPending}
              isError={false}
              emptyMessage="No hay usuarios con esos criterios"
              renderActions={(row) => acciones(row.original)}
            />
          </div>
          <DataTablePagination
            meta={consulta.data?.meta}
            page={page}
            onPageChange={setPage}
            onPageSizeChange={cambiarTamanio}
            isLoading={consulta.isFetching}
          />
        </>
      )}

      {creando && <CrearUsuarioModal onClose={() => setCreando(false)} />}

      <ModalConfirmacion
        open={aDesactivar !== null}
        onOpenChange={(open) => !open && setADesactivar(null)}
        title="Desactivar usuario"
        description={`${aDesactivar?.email ?? ''} no podrá iniciar sesión y se cerrarán sus sesiones abiertas. Podés reactivar la cuenta después.`}
        confirmLabel="Desactivar"
        isSubmitting={cambiarEstado.isPending}
        onConfirm={() => aDesactivar && void cambiar({ codigo: aDesactivar.codigo, accion: 'desactivar' })}
      />
    </div>
  )
}
