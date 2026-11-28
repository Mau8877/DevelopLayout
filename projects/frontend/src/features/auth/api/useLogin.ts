import { useMutation, useQueryClient } from '@tanstack/react-query'
import { apiClient } from '@/store'
import type { LoginRequest } from '../schemas'
import type { UsuarioSesion } from '../types'
import { authKeys } from './keys'

interface LoginData {
  usuario: UsuarioSesion
}

export function useLogin() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: async (request: LoginRequest) => {
      const response = await apiClient.post<LoginData>('/auth/login', request)
      return response.data.usuario
    },
    // * Los tokens llegan en cookies HttpOnly; solo el usuario se escribe en la query de sesión.
    onSuccess: (usuario) => {
      queryClient.setQueryData(authKeys.session, usuario)
    },
  })
}
