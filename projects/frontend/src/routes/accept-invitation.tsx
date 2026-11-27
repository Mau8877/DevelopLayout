import { createFileRoute } from '@tanstack/react-router'
import { AceptarInvitacionPage } from '@/features/auth'

// * Pública: la credencial es el token del enlace.
export const Route = createFileRoute('/accept-invitation')({
  component: AceptarInvitacionPage,
})
