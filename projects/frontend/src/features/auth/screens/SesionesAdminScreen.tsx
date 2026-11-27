import { useState } from 'react'
import { useForm } from '@tanstack/react-form'
import { CopyIcon, LogOutIcon, SearchIcon } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { ErrorState } from '@/components/ui/error-state'
import { FormField } from '@/components/ui/form-field'
import { Input } from '@/components/ui/input'
import { ModalDestructive } from '@/components/ui/modal-destructive'
import {
  DataTable,
  DataTablePagination,
  RowActionButton,
  useDataTable,
} from '@/components/ui/tanstack-table'
import { toastError, toastSuccess } from '@/components/ui/toast'
import { ApiError } from '@/store'
import { copiarAlPortapapeles } from '@/utils/copiarAlPortapapeles'
import { useCerrarSesionAdmin } from '../api/useCerrarSesionAdmin'
import { useCerrarTodasAdmin } from '../api/useCerrarTodasAdmin'
import { nuevaBusqueda, useSesionesAdmin, type Busqueda } from '../api/useSesionesAdmin'
import { columnasSesion } from '../components/columnasSesion'
import { buscarSesionesSchema } from '../schemas/buscarSesionesSchema'
import type { Sesion } from '../types/sesion'

const columnas = columnasSesion<Sesion>()

type Confirmacion = { tipo: 'una'; sesion: Sesion } | { tipo: 'todas'; usuarioCodigo: string }

function mensajeDelError(error: unknown): string {
  return error instanceof ApiError ? error.message : 'No pudimos completar la acción. Intentá de nuevo.'
}

export function SesionesAdminScreen() {
  const [busqueda, setBusqueda] = useState<Busqueda | null>(null)
  const [page, setPage] = useState(1)
  const [pageSize, setPageSize] = useState(20)
  const [confirmacion, setConfirmacion] = useState<Confirmacion | null>(null)

  const consulta = useSesionesAdmin(busqueda, page, pageSize)
  const cerrarUna = useCerrarSesionAdmin()
  const cerrarTodas = useCerrarTodasAdmin()

  const sesiones = consulta.data?.data ?? []
  const usuarioCodigo = sesiones[0]?.usuarioCodigo
  const { table } = useDataTable({
    data: sesiones,
    columns: columnas,
    meta: consulta.data?.meta,
    page,
    pageSize,
    onPageChange: setPage,
    onPageSizeChange: (nuevo) => {
      setPageSize(nuevo)
      setPage(1)
    },
    getRowId: (sesion) => sesion.codigo,
  })

  const form = useForm({
    defaultValues: { email: '' },
    validators: { onSubmit: buscarSesionesSchema },
    onSubmit: ({ value }) => {
      setPage(1)
      setBusqueda(nuevaBusqueda(value.email.trim()))
    },
  })

  async function confirmar() {
    if (!confirmacion) return
    try {
      if (confirmacion.tipo === 'una') {
        await cerrarUna.mutateAsync(confirmacion.sesion.codigo)
        toastSuccess('Sesión cerrada')
      } else {
        await cerrarTodas.mutateAsync(confirmacion.usuarioCodigo)
        toastSuccess('Sesiones cerradas')
      }
      setPage(1)
    } catch (error) {
      toastError(mensajeDelError(error))
    } finally {
      setConfirmacion(null)
    }
  }

  async function copiar(codigo: string) {
    if (await copiarAlPortapapeles(codigo)) toastSuccess('Código de usuario copiado')
    else toastError('No se pudo copiar', codigo)
  }

  return (
    <div className="flex flex-col gap-4">
      <form
        noValidate
        className="flex flex-wrap items-start gap-3"
        onSubmit={(event) => {
          event.preventDefault()
          void form.handleSubmit()
        }}
      >
        <form.Field name="email">
          {(field) => {
            const primero: unknown = field.state.meta.errors[0]
            return (
              <FormField
                id="buscar-email"
                label="Correo del usuario"
                description="Correo exacto"
                error={
                  primero && typeof primero === 'object' && 'message' in primero
                    ? String(primero.message)
                    : undefined
                }
                className="w-full max-w-sm"
              >
                {(controlProps) => (
                  <Input
                    {...controlProps}
                    type="email"
                    autoComplete="off"
                    value={field.state.value}
                    onBlur={field.handleBlur}
                    onChange={(event) => field.handleChange(event.target.value)}
                  />
                )}
              </FormField>
            )
          }}
        </form.Field>
        <Button type="submit" className="mt-5">
          <SearchIcon data-icon="inline-start" />
          Buscar
        </Button>
      </form>

      {busqueda === null ? (
        <p className="text-sm text-muted-foreground">Buscá un usuario por su correo para ver sus sesiones activas.</p>
      ) : consulta.isError ? (
        <ErrorState descripcion={mensajeDelError(consulta.error)} onReintentar={() => void consulta.refetch()} />
      ) : (
        <>
          {usuarioCodigo && (
            <div className="flex flex-wrap justify-end gap-2">
              <Button variant="outline" size="sm" onClick={() => void copiar(usuarioCodigo)}>
                <CopyIcon data-icon="inline-start" />
                Copiar código de usuario
              </Button>
              <Button
                variant="destructive"
                size="sm"
                onClick={() => setConfirmacion({ tipo: 'todas', usuarioCodigo })}
              >
                <LogOutIcon data-icon="inline-start" />
                Cerrar todas
              </Button>
            </div>
          )}
          <DataTable
            table={table}
            isLoading={consulta.isPending}
            isError={false}
            emptyMessage="No hay sesiones activas para ese correo"
            renderActions={(row) => (
              <RowActionButton
                icon={<LogOutIcon />}
                label="Cerrar sesión de este dispositivo"
                variant="destructive"
                onClick={() => setConfirmacion({ tipo: 'una', sesion: row.original })}
              />
            )}
          />
          <DataTablePagination
            meta={consulta.data?.meta}
            page={page}
            onPageChange={setPage}
            onPageSizeChange={(nuevo) => {
              setPageSize(nuevo)
              setPage(1)
            }}
            isLoading={consulta.isFetching}
          />
        </>
      )}

      <ModalDestructive
        open={confirmacion !== null}
        onOpenChange={(open) => !open && setConfirmacion(null)}
        title={confirmacion?.tipo === 'todas' ? 'Cerrar todas las sesiones' : 'Cerrar sesión'}
        description={
          confirmacion?.tipo === 'todas'
            ? 'El usuario perderá el acceso en todos sus dispositivos. Si es tu cuenta, tendrás que iniciar sesión de nuevo.'
            : 'El dispositivo perderá el acceso de inmediato. Si es tu sesión actual, tendrás que iniciar sesión de nuevo.'
        }
        confirmLabel="Cerrar"
        isSubmitting={cerrarUna.isPending || cerrarTodas.isPending}
        onConfirm={() => void confirmar()}
      />
    </div>
  )
}
