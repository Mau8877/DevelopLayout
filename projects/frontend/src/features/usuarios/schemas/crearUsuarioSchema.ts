import { z } from 'zod'

export const crearUsuarioSchema = z.object({
  email: z
    .string()
    .trim()
    .min(1, 'usuarios:validacion.correoObligatorio')
    .max(255, 'usuarios:validacion.correoLargo')
    .pipe(z.email('usuarios:validacion.correoInvalido')),
  roles: z.array(z.string()).min(1, 'usuarios:validacion.rolObligatorio'),
})
