import { useMutation, useQueryClient } from '@tanstack/react-query'
import { apiClient } from '@/store'
import { usuariosKeys } from './keys'

export function useReenviarInvitacion() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: async (codigo: string) => {
      const response = await apiClient.post<{ invitacionEnviada: boolean }>(
        `/users/${codigo}/resend-invitation`,
      )
      return response.data
    },
    // * También si falla: un 404 o un 409 indican que la lista quedó vieja.
    onSettled: () => queryClient.invalidateQueries({ queryKey: usuariosKeys.lists() }),
  })
}
