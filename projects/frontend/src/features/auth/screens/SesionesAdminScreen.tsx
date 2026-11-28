import { useTranslation } from 'react-i18next'
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
import { copiarAlPortapapeles, primerError } from '@/utils'
import { useCerrarSesionAdmin } from '../api'
import { useCerrarTodasAdmin } from '../api'
import { nuevaBusqueda, useSesionesAdmin, type Busqueda } from '../api'
import { columnasSesion } from '../components'
import { buscarSesionesSchema } from '../schemas'
import type { Sesion } from '../types'

type Confirmacion = { tipo: 'una'; sesion: Sesion } | { tipo: 'todas'; usuarioCodigo: string }

export function SesionesAdminScreen() {
  const { t } = useTranslation('auth')
  const [busqueda, setBusqueda] = useState<Busqueda | null>(null)
  const [page, setPage] = useState(1)
  const [pageSize, setPageSize] = useState(20)
  const [confirmacion, setConfirmacion] = useState<Confirmacion | null>(null)

  const columnas = columnasSesion<Sesion>()
  const mensajeDelError = (error: unknown) =>
    error instanceof ApiError ? error.message : t('comun.errorDeAccion')

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
        toastSuccess(t('sesiones.cerrada'))
      } else {
        await cerrarTodas.mutateAsync(confirmacion.usuarioCodigo)
        toastSuccess(t('sesionesAdmin.cerradas'))
      }
      setPage(1)
    } catch (error) {
      toastError(mensajeDelError(error))
    } finally {
      setConfirmacion(null)
    }
  }

  async function copiar(codigo: string) {
    if (await copiarAlPortapapeles(codigo)) toastSuccess(t('sesionesAdmin.codigoCopiado'))
    else toastError(t('sesionesAdmin.noSeCopio'), codigo)
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
          {(field) => (
            <FormField
              id="buscar-email"
              label={t('sesionesAdmin.correoDelUsuario')}
              description={t('sesionesAdmin.correoExacto')}
              error={primerError(field.state.meta.errors)}
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
          )}
        </form.Field>
        <Button type="submit" className="mt-5">
          <SearchIcon data-icon="inline-start" />
          {t('sesionesAdmin.buscar')}
        </Button>
      </form>

      {busqueda === null ? (
        <p className="text-sm text-muted-foreground">{t('sesionesAdmin.instruccion')}</p>
      ) : consulta.isError ? (
        <ErrorState descripcion={mensajeDelError(consulta.error)} onReintentar={() => void consulta.refetch()} />
      ) : (
        <>
          {usuarioCodigo && (
            <div className="flex flex-wrap justify-end gap-2">
              <Button variant="outline" size="sm" onClick={() => void copiar(usuarioCodigo)}>
                <CopyIcon data-icon="inline-start" />
                {t('sesionesAdmin.copiarCodigo')}
              </Button>
              <Button
                variant="destructive"
                size="sm"
                onClick={() => setConfirmacion({ tipo: 'todas', usuarioCodigo })}
              >
                <LogOutIcon data-icon="inline-start" />
                {t('sesionesAdmin.cerrarTodas')}
              </Button>
            </div>
          )}
          <DataTable
            table={table}
            isLoading={consulta.isPending}
            isError={false}
            emptyMessage={t('sesionesAdmin.vacio')}
            renderActions={(row) => (
              <RowActionButton
                icon={<LogOutIcon />}
                label={t('sesionesAdmin.cerrarDispositivo')}
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
        title={confirmacion?.tipo === 'todas' ? t('sesionesAdmin.tituloTodas') : t('sesiones.tituloCerrar')}
        description={
          confirmacion?.tipo === 'todas'
            ? t('sesionesAdmin.avisoTodas')
            : t('sesionesAdmin.avisoUna')
        }
        confirmLabel={t('comun.cerrar')}
        isSubmitting={cerrarUna.isPending || cerrarTodas.isPending}
        onConfirm={() => void confirmar()}
      />
    </div>
  )
}
