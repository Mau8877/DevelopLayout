import type { QueryClient } from '@tanstack/react-query'
import { authKeys } from './keys'

/**
 * Cierra la sesión local cuando el backend ya no deja renovarla.
 *
 * * Sin sesión en caché no hace nada: de eso se ocupa el guard de rutas, y
 * * redirigir desde acá haría un bucle en /login.
 * * La sesión queda en `null` (no ausente) para que /login no vuelva a pedirla.
 */
export function manejarSesionExpirada(queryClient: QueryClient, rutaActual: string, irALogin: () => void) {
  if (!queryClient.getQueryData(authKeys.session)) return
  queryClient.clear()
  queryClient.setQueryData(authKeys.session, null)
  if (rutaActual !== '/login') irALogin()
}
