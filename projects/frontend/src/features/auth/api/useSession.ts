import { useQuery } from '@tanstack/react-query'
import { apiClient } from '@/store'
import type { UsuarioSesion } from '../types/auth'
import { authKeys } from './keys'

export async function fetchSession(): Promise<UsuarioSesion> {
  const response = await apiClient.get<UsuarioSesion>('/auth/me')
  return response.data
}

/**
 * Sesión actual, decidida siempre por el backend (GET /auth/me).
 *
 * * Un 401 (ERR_AUTH_07) significa "sin sesión", no un fallo a reintentar.
 * * El usuario vive en TanStack Query: no se copia a Zustand ni a ningún storage.
 */
export function useSession() {
  return useQuery({
    queryKey: authKeys.session,
    queryFn: fetchSession,
    retry: false,
    staleTime: 5 * 60 * 1000,
  })
}
