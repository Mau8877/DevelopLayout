import { useMutation, useQueryClient } from '@tanstack/react-query'
import { apiClient } from '@/store'
import { authKeys } from './keys'

export function useCerrarSesionAdmin() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (codigo: string) => apiClient.delete(`/admin/sessions/${codigo}`),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: authKeys.sesionesAdmin }),
  })
}
