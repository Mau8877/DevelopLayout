import { createFileRoute } from '@tanstack/react-router'
import { BitacoraScreen } from '@/features/bitacora'

export const Route = createFileRoute('/_authenticated/_admin/bitacora')({
  staticData: { breadcrumb: 'Bitácora' },
  component: BitacoraScreen,
})
