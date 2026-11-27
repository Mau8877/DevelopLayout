import { useState } from 'react'
import { LogOutIcon } from 'lucide-react'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { ErrorState } from '@/components/ui/error-state'
import { ModalDestructive } from '@/components/ui/modal-destructive'
import { DataTable, DataTablePagination, RowActionButton, useDataTable } from '@/components/ui/tanstack-table'
import { toastError, toastSuccess } from '@/components/ui/toast'
import { ApiError } from '@/store'
import { useCerrarMiSesion } from '../api/useCerrarMiSesion'
import { useCerrarOtrasSesiones } from '../api/useCerrarOtrasSesiones'
import { useMisSesiones } from '../api/useMisSesiones'
import { columnasSesion } from '../components/columnasSesion'
import type { MiSesion } from '../types/sesion'

const columnas = columnasSesion<MiSesion>((sesion) => (sesion.actual ? <Badge>Esta sesión</Badge> : null))

type Confirmacion = { tipo: 'una'; sesion: MiSesion } | { tipo: 'otras' }

function mensajeDelError(error: unknown): string {
  return error instanceof ApiError ? error.message : 'No pudimos completar la acción. Intentá de nuevo.'
}

export function MisSesionesScreen() {
  const [page, setPage] = useState(1)
  const [pageSize, setPageSize] = useState(20)
  const [confirmacion, setConfirmacion] = useState<Confirmacion | null>(null)

  const consulta = useMisSesiones(page, pageSize)
  const cerrarUna = useCerrarMiSesion()
  const cerrarOtras = useCerrarOtrasSesiones()

  const meta = consulta.data?.meta
  // * Si al cerrar la última de una página esta dejó de existir, se vuelve atrás.
  if (meta && page > Math.max(1, meta.totalPages)) setPage(Math.max(1, meta.totalPages))

  const cambiarTamanio = (nuevo: number) => {
    setPageSize(nuevo)
    setPage(1)
  }
  const { table } = useDataTable({
    data: consulta.data?.data ?? [],
    columns: columnas,
    meta,
    page,
    pageSize,
    onPageChange: setPage,
    onPageSizeChange: cambiarTamanio,
    getRowId: (sesion) => sesion.codigo,
  })

  async function confirmar() {
    if (!confirmacion) return
    try {
      if (confirmacion.tipo === 'una') {
        await cerrarUna.mutateAsync(confirmacion.sesion.codigo)
        toastSuccess('Sesión cerrada')
      } else {
        await cerrarOtras.mutateAsync()
        toastSuccess('Se cerraron las demás sesiones')
        setPage(1)
      }
    } catch (error) {
      toastError(mensajeDelError(error))
    } finally {
      setConfirmacion(null)
    }
  }

  if (consulta.isError) {
    return <ErrorState descripcion={mensajeDelError(consulta.error)} onReintentar={() => void consulta.refetch()} />
  }

  return (
    <div className="flex flex-col gap-4">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <p className="text-sm text-muted-foreground">
          Estos son los lugares donde tu cuenta está abierta. Si no reconocés alguno, cerralo y cambiá tu
          contraseña.
        </p>
        <Button
          variant="destructive"
          size="sm"
          disabled={(meta?.totalItems ?? 0) <= 1}
          onClick={() => setConfirmacion({ tipo: 'otras' })}
        >
          <LogOutIcon data-icon="inline-start" />
          Cerrar todas las demás
        </Button>
      </div>

      <DataTable
        table={table}
        isLoading={consulta.isPending}
        isError={false}
        emptyMessage="No hay sesiones activas"
        renderActions={(row) =>
          row.original.actual ? null : (
            <RowActionButton
              icon={<LogOutIcon />}
              label="Cerrar esta otra sesión"
              variant="destructive"
              onClick={() => setConfirmacion({ tipo: 'una', sesion: row.original })}
            />
          )
        }
      />
      <DataTablePagination
        meta={meta}
        page={page}
        onPageChange={setPage}
        onPageSizeChange={cambiarTamanio}
        isLoading={consulta.isFetching}
      />

      <ModalDestructive
        open={confirmacion !== null}
        onOpenChange={(open) => !open && setConfirmacion(null)}
        title={confirmacion?.tipo === 'otras' ? 'Cerrar todas las demás sesiones' : 'Cerrar sesión'}
        description={
          confirmacion?.tipo === 'otras'
            ? 'Los demás dispositivos perderán el acceso de inmediato. Esta sesión sigue abierta.'
            : 'Ese dispositivo perderá el acceso de inmediato.'
        }
        confirmLabel="Cerrar"
        isSubmitting={cerrarUna.isPending || cerrarOtras.isPending}
        onConfirm={() => void confirmar()}
      />
    </div>
  )
}
