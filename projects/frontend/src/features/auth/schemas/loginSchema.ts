import { z } from 'zod'

// * Mismas reglas que LoginRequest del backend (correo válido, contraseña de
// * 8 a 72 bytes: BCrypt ignora lo que pase de 72). El backend valida igual.
const MAX_PASSWORD_BYTES = 72

export const loginSchema = z.object({
  email: z.string().min(1, 'El correo es obligatorio').email('El correo no es válido'),
  password: z
    .string()
    .min(1, 'La contraseña es obligatoria')
    .min(8, 'La contraseña debe tener al menos 8 caracteres')
    .refine((value) => new TextEncoder().encode(value).length <= MAX_PASSWORD_BYTES, {
      message: 'La contraseña es demasiado larga',
    }),
})

export type LoginRequest = z.infer<typeof loginSchema>
