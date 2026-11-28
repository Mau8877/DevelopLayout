import { z } from 'zod'
import { loginSchema } from './loginSchema'

const MAX_PASSWORD_BYTES = 72

// * La nueva sigue la misma política que el login. Que sea distinta de la actual lo
// * comprueba el servidor, que es quien sabe cuál es la actual.
export const cambiarPasswordSchema = z
  .object({
    passwordActual: z
      .string()
      .min(1, 'auth:validacion.passwordActualObligatoria')
      .refine((valor) => new TextEncoder().encode(valor).length <= MAX_PASSWORD_BYTES, {
        message: 'auth:validacion.passwordLarga',
      }),
    passwordNueva: loginSchema.shape.password,
    confirmacion: z.string().min(1, 'auth:validacion.repetirPassword'),
  })
  .refine((valores) => valores.passwordNueva === valores.confirmacion, {
    message: 'auth:validacion.passwordsDistintas',
    path: ['confirmacion'],
  })

export type CambiarPasswordForm = z.infer<typeof cambiarPasswordSchema>
