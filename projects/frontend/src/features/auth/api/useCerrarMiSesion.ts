import { useMutation, useQueryClient } from '@tanstack/react-query'
import { apiClient } from '@/store'
import { authKeys } from './keys'

export function useCerrarMiSesion() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (codigo: string) => apiClient.delete(`/auth/sessions/${codigo}`),
    // * También si falla: un 404 significa que el listado quedó viejo.
    onSettled: () => queryClient.invalidateQueries({ queryKey: authKeys.misSesiones }),
  })
}
