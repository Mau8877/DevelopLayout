import type { ReactNode } from 'react'
import type { DataTableColumnDef } from '@/components/ui/tanstack-table'
import { formatDateTime } from '@/utils/formatDateTime'
import { resumirDispositivo } from '@/utils/resumirDispositivo'
import type { CamposDeSesion } from '../types/sesion'

/**
 * Columnas de una sesión, comunes a «Mis sesiones» y a la administración.
 * `marca` agrega un distintivo junto al dispositivo.
 *
 * * El user agent es texto del cliente: se muestra siempre como texto.
 */
export function columnasSesion<T extends CamposDeSesion>(marca?: (sesion: T) => ReactNode): DataTableColumnDef<T>[] {
  return [
    {
      accessorKey: 'userAgent',
      header: 'Dispositivo',
      // * Con distintivo necesita un ancho propio: la tabla recorta cada celda.
      meta: marca ? { width: 'w-72' } : { grow: true },
      cell: ({ row }) => (
        <span className="flex items-center gap-2">
          <span title={row.original.userAgent}>
            {resumirDispositivo(row.original.userAgent, row.original.tipoCliente)}
          </span>
          {marca?.(row.original)}
        </span>
      ),
    },
    { accessorKey: 'tipoCliente', header: 'Cliente', meta: { width: 'w-24' } },
    { accessorKey: 'ip', header: 'IP', meta: { width: 'w-32' }, cell: ({ row }) => row.original.ip ?? '—' },
    {
      accessorKey: 'creadaEn',
      header: 'Iniciada',
      meta: { width: 'w-40' },
      cell: ({ row }) => formatDateTime(row.original.creadaEn),
    },
    {
      accessorKey: 'ultimoUsoEn',
      header: 'Último uso',
      meta: { width: 'w-40' },
      cell: ({ row }) => formatDateTime(row.original.ultimoUsoEn),
    },
  ] as DataTableColumnDef<T>[]
}
