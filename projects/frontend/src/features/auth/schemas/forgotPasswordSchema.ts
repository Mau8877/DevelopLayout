import { z } from 'zod'
import { loginSchema } from './loginSchema'

// * Misma regla de correo que el login (y que ForgotPasswordRequest en el backend).
export const forgotPasswordSchema = z.object({
  email: loginSchema.shape.email,
})

export type ForgotPasswordRequest = z.infer<typeof forgotPasswordSchema>
