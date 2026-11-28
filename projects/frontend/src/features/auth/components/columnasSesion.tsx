import type { ReactNode } from 'react'
import type { DataTableColumnDef } from '@/components/ui/tanstack-table'
import { i18n } from '@/shared/language'
import { formatDateTime } from '@/utils'
import { resumirDispositivo } from '@/utils'
import type { CamposDeSesion } from '../types'

/**
 * Columnas de una sesión, comunes a «Mis sesiones» y a la administración.
 * `marca` agrega un distintivo junto al dispositivo.
 *
 * * El user agent es texto del cliente: se muestra siempre como texto.
 * * Se llama al pintar, no al cargar el módulo: los encabezados siguen el idioma.
 */
export function columnasSesion<T extends CamposDeSesion>(marca?: (sesion: T) => ReactNode): DataTableColumnDef<T>[] {
  return [
    {
      accessorKey: 'userAgent',
      header: i18n.t('auth:sesiones.columna.dispositivo'),
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
    { accessorKey: 'tipoCliente', header: i18n.t('auth:sesiones.columna.cliente'), meta: { width: 'w-24' } },
    { accessorKey: 'ip', header: i18n.t('auth:sesiones.columna.ip'), meta: { width: 'w-32' }, cell: ({ row }) => row.original.ip ?? '—' },
    {
      accessorKey: 'creadaEn',
      header: i18n.t('auth:sesiones.columna.iniciada'),
      meta: { width: 'w-40' },
      cell: ({ row }) => formatDateTime(row.original.creadaEn),
    },
    {
      accessorKey: 'ultimoUsoEn',
      header: i18n.t('auth:sesiones.columna.ultimoUso'),
      meta: { width: 'w-40' },
      cell: ({ row }) => formatDateTime(row.original.ultimoUsoEn),
    },
  ] as DataTableColumnDef<T>[]
}
