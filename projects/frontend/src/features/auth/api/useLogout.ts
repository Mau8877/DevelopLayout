import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useNavigate } from '@tanstack/react-router'
import { apiClient, conCandadoDeSesion, TIMEOUT_SESION_MS } from '@/store'
import { authKeys } from './keys'

/**
 * Cierra la sesión.
 *
 * * La limpieza local va en `onSettled`: se hace aunque el servidor no responda.
 * * Usa el candado de sesión para no cruzarse con un refresh en curso, que
 * * dejaría viva la sesión recién rotada.
 */
export function useLogout() {
  const queryClient = useQueryClient()
  const navigate = useNavigate()

  return useMutation({
    mutationFn: () =>
      conCandadoDeSesion(() => apiClient.post('/auth/logout', undefined, { timeout: TIMEOUT_SESION_MS })),
    onSettled: () => {
      queryClient.clear()
      queryClient.setQueryData(authKeys.session, null)
      void navigate({ to: '/login' })
    },
  })
}
