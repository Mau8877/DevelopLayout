import { useMutation, useQueryClient } from '@tanstack/react-query'
import type { components } from '@/shared/api'
import { apiClient } from '@/store'
import type { UsuarioCreado } from '../types'
import { usuariosKeys } from './keys'

export function useCrearUsuario() {
  const queryClient = useQueryClient()

  return useMutation({
    // * El correo no queda en la caché de mutaciones.
    gcTime: 0,
    mutationFn: async (request: components['schemas']['CrearUsuarioRequest']) => {
      const response = await apiClient.post<UsuarioCreado>('/users', request)
      return response.data
    },
    onSuccess: () => queryClient.invalidateQueries({ queryKey: usuariosKeys.lists() }),
  })
}
