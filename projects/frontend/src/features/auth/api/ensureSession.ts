import type { QueryClient } from '@tanstack/react-query'
import { ApiError } from '@/store'
import type { UsuarioSesion } from '../types'
import { authKeys } from './keys'
import { fetchSession } from './useSession'

/**
 * Devuelve el usuario de la sesión o `null` si no hay sesión (401).
 *
 * * Es solo para UX (guard de rutas): la autorización real la hace el backend
 * * en cada request. Otros fallos (red, 500) se propagan al error boundary.
 */
export async function ensureSession(queryClient: QueryClient): Promise<UsuarioSesion | null> {
  try {
    return await queryClient.ensureQueryData({
      queryKey: authKeys.session,
      queryFn: fetchSession,
      revalidateIfStale: false,
      // * Un 401 no cambia al repetirlo, y cada intento gasta un refresh de la cuota por IP.
      retry: false,
    })
  } catch (error) {
    if (error instanceof ApiError && error.code === 'ERR_AUTH_07') return null
    throw error
  }
}
