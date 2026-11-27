import { z } from 'zod'
import { loginSchema } from './loginSchema'

// * Misma política que el login: 8 caracteres como mínimo y 72 bytes como máximo.
export const resetPasswordSchema = z
  .object({
    password: loginSchema.shape.password,
    confirmacion: z.string().min(1, 'Repetí la contraseña'),
  })
  .refine((valores) => valores.password === valores.confirmacion, {
    message: 'Las contraseñas no coinciden',
    path: ['confirmacion'],
  })

export type ResetPasswordForm = z.infer<typeof resetPasswordSchema>
