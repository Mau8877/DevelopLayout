import { useMutation } from '@tanstack/react-query'
import type { components } from '@/shared/api/schema'
import { apiClient } from '@/store'

/**
 * Guarda la contraseña elegida con el token de la invitación (va en el body,
 * nunca en la URL). No inicia sesión.
 *
 * * `gcTime: 0`: ni el token ni la contraseña quedan en la caché de mutaciones.
 */
export function useAceptarInvitacion() {
  return useMutation({
    gcTime: 0,
    mutationFn: async (request: components['schemas']['AceptarInvitacionRequest']) => {
      await apiClient.post('/auth/invitation/accept', request)
    },
  })
}
