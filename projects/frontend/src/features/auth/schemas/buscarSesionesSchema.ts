import { z } from 'zod'

export const buscarSesionesSchema = z.object({
  email: z.string().min(1, 'auth:validacion.correoObligatorio').email('auth:validacion.correoInvalido'),
})
