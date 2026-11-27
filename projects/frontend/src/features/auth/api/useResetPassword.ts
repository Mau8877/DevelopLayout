import { useMutation, useQueryClient } from '@tanstack/react-query'
import type { components } from '@/shared/api/schema'
import { apiClient } from '@/store'
import { authKeys } from './keys'

/**
 * Cambia la contraseña con el token del enlace (va en el body, nunca en la URL).
 *
 * * El servidor revoca todas las sesiones: si este navegador tenía una, la caché
 * * local se limpia igual que al cerrar sesión.
 * * `gcTime: 0`: ni el token ni la contraseña quedan en la caché de mutaciones.
 */
export function useResetPassword() {
  const queryClient = useQueryClient()

  return useMutation({
    gcTime: 0,
    mutationFn: async (request: components['schemas']['ResetPasswordRequest']) => {
      await apiClient.post('/auth/password/reset', request)
    },
    onSuccess: () => {
      queryClient.clear()
      queryClient.setQueryData(authKeys.session, null)
    },
  })
}
