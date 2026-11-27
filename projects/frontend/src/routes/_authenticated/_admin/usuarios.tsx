import { createFileRoute } from '@tanstack/react-router'
import { UsuariosScreen } from '@/features/usuarios'

export const Route = createFileRoute('/_authenticated/_admin/usuarios')({
  staticData: { breadcrumb: 'Usuarios' },
  component: UsuariosScreen,
})
