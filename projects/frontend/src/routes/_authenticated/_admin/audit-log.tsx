import { createFileRoute } from '@tanstack/react-router'
import { BitacoraScreen } from '@/features/bitacora'

export const Route = createFileRoute('/_authenticated/_admin/audit-log')({
  staticData: { breadcrumb: 'shared:navegacion.bitacora' },
  component: BitacoraScreen,
})
