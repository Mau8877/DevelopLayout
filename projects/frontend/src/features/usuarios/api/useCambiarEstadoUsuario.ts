import { useMutation, useQueryClient } from '@tanstack/react-query'
import { apiClient } from '@/store'
import { usuariosKeys } from './keys'

export interface CambioDeEstado {
  codigo: string
  accion: 'desactivar' | 'reactivar'
}

export function useCambiarEstadoUsuario() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: async ({ codigo, accion }: CambioDeEstado) => {
      await apiClient.post(`/usuarios/${codigo}/${accion}`)
    },
    // * También si falla: un 404 o un 409 indican que la lista quedó vieja.
    onSettled: () => queryClient.invalidateQueries({ queryKey: usuariosKeys.lists() }),
  })
}
