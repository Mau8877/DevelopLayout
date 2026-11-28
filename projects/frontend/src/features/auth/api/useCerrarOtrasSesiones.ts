import { useMutation, useQueryClient } from '@tanstack/react-query'
import { apiClient } from '@/store'
import { authKeys } from './keys'

export function useCerrarOtrasSesiones() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: () => apiClient.post('/auth/sessions/revoke-others'),
    onSettled: () => queryClient.invalidateQueries({ queryKey: authKeys.misSesiones }),
  })
}
