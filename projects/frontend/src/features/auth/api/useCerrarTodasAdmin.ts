import { useMutation, useQueryClient } from '@tanstack/react-query'
import { apiClient } from '@/store'
import { authKeys } from './keys'

export function useCerrarTodasAdmin() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (codigoUsuario: string) => apiClient.post(`/admin/users/${codigoUsuario}/sessions/revoke`),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: authKeys.sesionesAdmin }),
  })
}
