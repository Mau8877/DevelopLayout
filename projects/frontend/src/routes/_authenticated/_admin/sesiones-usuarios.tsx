import { createFileRoute } from '@tanstack/react-router'
import { SesionesAdminScreen } from '@/features/auth'

export const Route = createFileRoute('/_authenticated/_admin/sesiones-usuarios')({
  staticData: { breadcrumb: 'Sesiones de usuarios' },
  component: SesionesAdminScreen,
})
