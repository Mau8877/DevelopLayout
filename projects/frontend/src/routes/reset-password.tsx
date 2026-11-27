import { createFileRoute } from '@tanstack/react-router'
import { ResetPasswordPage } from '@/features/auth'

// * Pública también con sesión iniciada: la credencial es el token del enlace.
export const Route = createFileRoute('/reset-password')({
  component: ResetPasswordPage,
})
