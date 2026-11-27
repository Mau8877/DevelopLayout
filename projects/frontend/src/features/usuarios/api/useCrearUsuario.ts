import { useMutation, useQueryClient } from '@tanstack/react-query'
import type { components } from '@/shared/api/schema'
import { apiClient } from '@/store'
import type { UsuarioCreado } from '../types/usuario'
import { usuariosKeys } from './keys'

export function useCrearUsuario() {
  const queryClient = useQueryClient()

  return useMutation({
    // * El correo no queda en la caché de mutaciones.
    gcTime: 0,
    mutationFn: async (request: components['schemas']['CrearUsuarioRequest']) => {
      const response = await apiClient.post<UsuarioCreado>('/usuarios', request)
      return response.data
    },
    onSuccess: () => queryClient.invalidateQueries({ queryKey: usuariosKeys.lists() }),
  })
}
