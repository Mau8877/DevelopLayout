import { Badge } from '@/components/ui/badge'
import type { DataTableColumnDef } from '@/components/ui/tanstack-table'
import { formatDateTime } from '@/utils/formatDateTime'
import type { Usuario } from '../types/usuario'
import { etiquetaDeRol } from './roles'

export const columnasUsuario: DataTableColumnDef<Usuario>[] = [
  { accessorKey: 'email', header: 'Correo', meta: { grow: true, cardTitle: true } },
  {
    accessorKey: 'roles',
    header: 'Roles',
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
    header: 'Estado',
    meta: { width: 'w-24' },
    cell: ({ row }) => (
      <Badge variant={row.original.estado === 'ACTIVO' ? 'secondary' : 'destructive'}>
        {row.original.estado === 'ACTIVO' ? 'Activa' : 'Inactiva'}
      </Badge>
    ),
  },
  {
    accessorKey: 'invitacionPendiente',
    header: 'Invitación',
    meta: { width: 'w-28' },
    cell: ({ row }) => (row.original.invitacionPendiente ? <Badge variant="outline">Pendiente</Badge> : '—'),
  },
  {
    accessorKey: 'creadoEn',
    header: 'Alta',
    meta: { width: 'w-40' },
    cell: ({ row }) => formatDateTime(row.original.creadoEn),
  },
]
