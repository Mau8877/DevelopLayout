import { createFileRoute, redirect } from '@tanstack/react-router'
import { ensureSession, LoginScreen } from '@/features/auth'
import { queryClient } from '@/store'

export const Route = createFileRoute('/login')({
  // Si ya hay sesión, no tiene sentido mostrar el login.
  beforeLoad: async () => {
    if (await ensureSession(queryClient)) {
      throw redirect({ to: '/dashboard' })
    }
  },
  component: LoginScreen,
})
