import { useTranslation } from 'react-i18next'
import { useMemo, useState } from 'react'
import { MailIcon, ShieldIcon, UserCheckIcon, UserPlusIcon, UserXIcon } from 'lucide-react'
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
import { debounce } from '@/utils'
import { useCambiarEstadoUsuario, type CambioDeEstado } from '../api'
import { useReenviarInvitacion } from '../api'
import { nuevaBusqueda, SIN_BUSQUEDA, useUsuarios } from '../api'
import { columnasUsuario } from '../components'
import { CrearUsuarioModal } from '../components'
import { mensajeDelError } from '../components'
import { RolesModal } from '../components'
import { opcionesDeRol } from '../components'
import type { EstadoUsuario, Usuario } from '../types'

const ESPERA_BUSQUEDA_MS = 300

export function UsuariosScreen() {
  const { t } = useTranslation('usuarios')
  const [texto, setTexto] = useState('')
  const [busqueda, setBusqueda] = useState(SIN_BUSQUEDA)
  const [estado, setEstado] = useState<EstadoUsuario | undefined>(undefined)
  const [rol, setRol] = useState<string | undefined>(undefined)
  const [page, setPage] = useState(1)
  const [pageSize, setPageSize] = useState(20)
  const [creando, setCreando] = useState(false)
  const [aDesactivar, setADesactivar] = useState<Usuario | null>(null)
  const [conRoles, setConRoles] = useState<Usuario | null>(null)

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
    columns: columnasUsuario(t),
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
      toastSuccess(t(cambio.accion === 'deactivate' ? 'lista.usuarioDesactivado' : 'lista.usuarioReactivado'))
    } catch (error) {
      toastError(mensajeDelError(error))
    } finally {
      setADesactivar(null)
    }
  }

  async function reenviarInvitacion(usuario: Usuario) {
    try {
      const { invitacionEnviada } = await reenviar.mutateAsync(usuario.codigo)
      if (invitacionEnviada) toastSuccess(t('lista.invitacionReenviada'))
      else toastWarning(t('lista.sinCorreoTitulo'), t('lista.sinCorreoTexto'))
    } catch (error) {
      toastError(mensajeDelError(error))
    }
  }

  const ocupado = reenviar.isPending || cambiarEstado.isPending

  // * Qué botones se muestran es comodidad: quien decide es el backend.
  const acciones = (usuario: Usuario) => (
    <>
      <RowActionButton
        icon={<ShieldIcon />}
        label={t('lista.cambiarRoles')}
        disabled={ocupado}
        onClick={() => setConRoles(usuario)}
      />
      {usuario.estado === 'ACTIVO' && usuario.invitacionPendiente && (
        <RowActionButton
          icon={<MailIcon />}
          label={t('lista.reenviar')}
          disabled={ocupado}
          onClick={() => void reenviarInvitacion(usuario)}
        />
      )}
      {usuario.estado === 'ACTIVO' && usuario.codigo !== yo && (
        <RowActionButton
          icon={<UserXIcon />}
          label={t('lista.desactivar')}
          variant="destructive"
          disabled={ocupado}
          onClick={() => setADesactivar(usuario)}
        />
      )}
      {usuario.estado === 'INACTIVO' && (
        <RowActionButton
          icon={<UserCheckIcon />}
          label={t('lista.reactivar')}
          disabled={ocupado}
          onClick={() => void cambiar({ codigo: usuario.codigo, accion: 'reactivate' })}
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
        searchPlaceholder={t('lista.buscar')}
        filters={[
          {
            id: 'estado',
            label: t('lista.filtroEstado'),
            options: [
              { value: 'ACTIVO', label: t('estado.activa') },
              { value: 'INACTIVO', label: t('estado.inactiva') },
            ],
            value: estado,
            onValueChange: (valor) => {
              setPage(1)
              setEstado(valor as EstadoUsuario | undefined)
            },
          },
          {
            id: 'rol',
            label: t('lista.filtroRol'),
            options: opcionesDeRol(),
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
            {t('lista.crear')}
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
              emptyMessage={t('lista.vacio')}
              renderActions={(row) => acciones(row.original)}
            />
          </div>
          <div className="md:hidden">
            <DataCards
              table={table}
              isLoading={consulta.isPending}
              isError={false}
              emptyMessage={t('lista.vacio')}
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
      {conRoles && (
        <RolesModal usuario={conRoles} esPropia={conRoles.codigo === yo} onClose={() => setConRoles(null)} />
      )}

      <ModalConfirmacion
        open={aDesactivar !== null}
        onOpenChange={(open) => !open && setADesactivar(null)}
        title={t('lista.desactivarTitulo')}
        description={t('lista.desactivarAviso', { correo: aDesactivar?.email ?? '' })}
        confirmLabel={t('lista.desactivar')}
        isSubmitting={cambiarEstado.isPending}
        onConfirm={() => aDesactivar && void cambiar({ codigo: aDesactivar.codigo, accion: 'deactivate' })}
      />
    </div>
  )
}
