import { describe, it, expect, vi } from 'vitest'
import { QueryClient } from '@tanstack/react-query'
import { apiClient, ApiError, queryClient } from '@/store'
import { ensureSession } from '../api/ensureSession'

const usuario = { codigo: 'u-1', email: 'a@b.co', roles: ['USUARIO'] }

describe('ensureSession (guard de rutas privadas)', () => {
  it('con sesión devuelve el usuario', async () => {
    vi.spyOn(apiClient, 'get').mockResolvedValue({ data: usuario })

    expect(await ensureSession(new QueryClient())).toEqual(usuario)
  })

  it('sin sesión (401) devuelve null para que el guard redirija al login', async () => {
    vi.spyOn(apiClient, 'get').mockRejectedValue(new ApiError('ERR_AUTH_07', 'No autenticado'))

    expect(await ensureSession(new QueryClient())).toBeNull()
  })

  it('un fallo de red se propaga (no se confunde con "sin sesión")', async () => {
    vi.spyOn(apiClient, 'get').mockRejectedValue(new ApiError('CLIENT_NETWORK', 'sin conexión'))

    await expect(ensureSession(new QueryClient())).rejects.toMatchObject({ code: 'CLIENT_NETWORK' })
  })

  it('con los defaults de la app no reintenta un 401 (cada intento gastaría un refresh)', async () => {
    const get = vi.spyOn(apiClient, 'get').mockClear().mockRejectedValue(new ApiError('ERR_AUTH_07', 'No autenticado'))
    queryClient.clear()

    expect(await ensureSession(queryClient)).toBeNull()

    expect(get).toHaveBeenCalledTimes(1)
  })
})
