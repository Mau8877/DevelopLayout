import { describe, it, expect, vi, beforeEach } from 'vitest'
import { renderHook, waitFor } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import type { ReactNode } from 'react'
import { apiClient, ApiError } from '@/store'
import { authKeys } from '../api'
import { useLogin } from '../api'
import { useSession } from '../api'

const usuario = { codigo: 'u-1', email: 'a@b.co', roles: ['USUARIO'] }

function setup() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  const wrapper = ({ children }: { children: ReactNode }) => (
    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  )
  return { queryClient, wrapper }
}

describe('useSession', () => {
  beforeEach(() => vi.restoreAllMocks())

  it('devuelve el usuario de /auth/me', async () => {
    vi.spyOn(apiClient, 'get').mockResolvedValue({ data: usuario })
    const { wrapper } = setup()

    const { result } = renderHook(() => useSession(), { wrapper })

    await waitFor(() => expect(result.current.isSuccess).toBe(true))
    expect(result.current.data).toEqual(usuario)
  })

  it('con 401 queda en error sin reintentar (sin sesión)', async () => {
    const get = vi.spyOn(apiClient, 'get').mockRejectedValue(new ApiError('ERR_AUTH_07', 'No autenticado'))
    const { wrapper } = setup()

    const { result } = renderHook(() => useSession(), { wrapper })

    await waitFor(() => expect(result.current.isError).toBe(true))
    expect(get).toHaveBeenCalledTimes(1)
  })
})

describe('useLogin', () => {
  beforeEach(() => vi.restoreAllMocks())

  it('al éxito escribe el usuario en la query de sesión', async () => {
    const post = vi.spyOn(apiClient, 'post').mockResolvedValue({ data: { usuario } })
    const { queryClient, wrapper } = setup()

    const { result } = renderHook(() => useLogin(), { wrapper })
    result.current.mutate({ email: 'a@b.co', password: 'ClaveSegura123' })

    await waitFor(() => expect(result.current.isSuccess).toBe(true))
    expect(post).toHaveBeenCalledWith('/auth/login', { email: 'a@b.co', password: 'ClaveSegura123' })
    expect(queryClient.getQueryData(authKeys.session)).toEqual(usuario)
  })

  it('con credenciales inválidas expone el ApiError y no toca la sesión', async () => {
    vi.spyOn(apiClient, 'post').mockRejectedValue(new ApiError('ERR_AUTH_01', 'Correo o contraseña incorrectos'))
    const { queryClient, wrapper } = setup()

    const { result } = renderHook(() => useLogin(), { wrapper })
    result.current.mutate({ email: 'a@b.co', password: 'ClaveSegura123' })

    await waitFor(() => expect(result.current.isError).toBe(true))
    expect((result.current.error as ApiError).code).toBe('ERR_AUTH_01')
    expect(queryClient.getQueryData(authKeys.session)).toBeUndefined()
  })
})
