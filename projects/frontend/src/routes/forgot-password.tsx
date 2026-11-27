import { createFileRoute } from '@tanstack/react-router'
import { ForgotPasswordScreen } from '@/features/auth'

export const Route = createFileRoute('/forgot-password')({
  component: ForgotPasswordScreen,
})
