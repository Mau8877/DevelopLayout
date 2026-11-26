import { z } from 'zod'

export const buscarSesionesSchema = z.object({
  email: z.string().min(1, 'El correo es obligatorio').email('El correo no es válido'),
})
