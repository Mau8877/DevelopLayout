import { useMutation, useQueryClient } from '@tanstack/react-query'
import type { components } from '@/shared/api/schema'
import { apiClient } from '@/store'
import type { Usuario } from '../types/usuario'
import { usuariosKeys } from './keys'

export type CambioDeRoles = { codigo: string } & components['schemas']['CambiarRolesRequest']

export function useCambiarRoles() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: async ({ codigo, roles }: CambioDeRoles) => {
      const response = await apiClient.put<Usuario>(`/usuarios/${codigo}/roles`, { roles })
      return response.data
    },
    // * También si falla: un 404 o un 409 indican que la lista quedó vieja.
    onSettled: () => queryClient.invalidateQueries({ queryKey: usuariosKeys.lists() }),
  })
}
