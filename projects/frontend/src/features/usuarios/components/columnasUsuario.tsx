import type { TFunction } from 'i18next'
import { Badge } from '@/components/ui/badge'
import type { DataTableColumnDef } from '@/components/ui/tanstack-table'
import { formatDateTime } from '@/utils'
import type { Usuario } from '../types'
import { etiquetaDeRol } from './roles'

/** Se llama al pintar, no al cargar el módulo: los textos siguen el idioma. */
export function columnasUsuario(t: TFunction<'usuarios'>): DataTableColumnDef<Usuario>[] {
  return [
  { accessorKey: 'email', header: t('columna.correo'), meta: { grow: true, cardTitle: true } },
  {
    accessorKey: 'roles',
    header: t('columna.roles'),
    meta: { width: 'w-52' },
    cell: ({ row }) => (
      <span className="flex flex-wrap gap-1">
        {row.original.roles.map((rol) => (
          <Badge key={rol} variant="outline">
            {etiquetaDeRol(rol)}
          </Badge>
        ))}
      </span>
    ),
  },
  {
    accessorKey: 'estado',
    header: t('columna.estado'),
    meta: { width: 'w-24' },
    cell: ({ row }) => (
      <Badge variant={row.original.estado === 'ACTIVO' ? 'secondary' : 'destructive'}>
        {row.original.estado === 'ACTIVO' ? t('estado.activa') : t('estado.inactiva')}
      </Badge>
    ),
  },
  {
    accessorKey: 'invitacionPendiente',
    header: t('columna.invitacion'),
    meta: { width: 'w-28' },
    cell: ({ row }) => (row.original.invitacionPendiente ? <Badge variant="outline">{t('invitacion.pendiente')}</Badge> : '—'),
  },
  {
    accessorKey: 'creadoEn',
    header: t('columna.alta'),
    meta: { width: 'w-40' },
    cell: ({ row }) => formatDateTime(row.original.creadoEn),
  },
  ]
}
