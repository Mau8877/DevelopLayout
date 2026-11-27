import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import type { ReactElement } from 'react'
import { authKeys } from '@/features/auth'
import type { Usuario } from '../types/usuario'

export function usuario(cambios: Partial<Usuario> = {}): Usuario {
  return {
    codigo: 'u-1',
    email: 'ana@test.local',
    roles: ['USUARIO'],
    estado: 'ACTIVO',
    invitacionPendiente: false,
    creadoEn: '2026-03-10T10:00:00Z',
    ...cambios,
  }
}

export const pagina = (data: Usuario[], totalPages = 1) => ({
  data: { data, meta: { page: 1, pageSize: 20, totalItems: data.length, totalPages } },
})

/** `yo` es el código del administrador con la sesión abierta. */
export function montar(elemento: ReactElement) {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  queryClient.setQueryData(authKeys.session, { codigo: 'yo', email: 'yo@test.local', roles: ['SUPER_ADMIN'] })
  render(<QueryClientProvider client={queryClient}>{elemento}</QueryClientProvider>)
  return { queryClient, user: userEvent.setup() }
}
