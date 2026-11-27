import { z } from 'zod'

export const rolesSchema = z.object({
  roles: z.array(z.string()).min(1, 'Elegí al menos un rol'),
})
