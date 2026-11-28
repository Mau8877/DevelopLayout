import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useNavigate } from '@tanstack/react-router'
import type { components } from '@/shared/api'
import { apiClient } from '@/store'
import { authKeys } from './keys'

/**
 * Cambia la propia contraseña. El servidor cierra todas las sesiones, también
 * esta: al lograrlo se limpia la sesión local y se va al login.
 *
 * * `gcTime: 0`: las contraseñas no quedan en la caché de mutaciones.
 * ! No usar `conCandadoDeSesion`: si el access token venció, el interceptor
 * ! renueva pidiendo ese mismo candado y la petición se bloquearía a sí misma.
 * ! El servidor ya serializa el cambio con la renovación.
 */
export function useCambiarPassword() {
  const queryClient = useQueryClient()
  const navigate = useNavigate()

  return useMutation({
    gcTime: 0,
    mutationFn: async (request: components['schemas']['CambiarPasswordRequest']) => {
      await apiClient.post('/auth/password/change', request)
    },
    onSuccess: async () => {
      queryClient.clear()
      queryClient.setQueryData(authKeys.session, null)
      await navigate({ to: '/login' })
    },
  })
}
