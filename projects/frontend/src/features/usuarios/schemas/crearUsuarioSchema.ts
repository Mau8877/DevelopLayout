import { z } from 'zod'

export const crearUsuarioSchema = z.object({
  email: z
    .string()
    .trim()
    .min(1, 'El correo es obligatorio')
    .max(255, 'El correo es demasiado largo')
    .pipe(z.email('El correo no es válido')),
  roles: z.array(z.string()).min(1, 'Elegí al menos un rol'),
})
