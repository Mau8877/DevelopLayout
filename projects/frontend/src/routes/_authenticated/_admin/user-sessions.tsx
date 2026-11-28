import { createFileRoute } from '@tanstack/react-router'
import { SesionesAdminScreen } from '@/features/auth'

export const Route = createFileRoute('/_authenticated/_admin/user-sessions')({
  staticData: { breadcrumb: 'shared:navegacion.sesionesUsuarios' },
  component: SesionesAdminScreen,
})
