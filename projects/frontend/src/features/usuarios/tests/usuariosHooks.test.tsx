import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { renderHook, waitFor } from '@testing-library/react'
import type { ReactNode } from 'react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { apiClient, ApiError } from '@/store'
import { usuariosKeys } from '../api/keys'
import { useCambiarEstadoUsuario } from '../api/useCambiarEstadoUsuario'
import { useCrearUsuario } from '../api/useCrearUsuario'
import { useReenviarInvitacion } from '../api/useReenviarInvitacion'
import { nuevaBusqueda, useUsuarios } from '../api/useUsuarios'

function entorno() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  const invalidar = vi.spyOn(queryClient, 'invalidateQueries')
  const wrapper = ({ children }: { children: ReactNode }) => (
    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  )
  return { queryClient, invalidar, wrapper }
}

describe('hooks de usuarios', () => {
  beforeEach(() => vi.restoreAllMocks())

  it('cada cambio llama a su endpoint e invalida los listados', async () => {
    const post = vi.spyOn(apiClient, 'post').mockResolvedValue({ data: {} })
    const { invalidar, wrapper } = entorno()
    const { result } = renderHook(
      () => ({ crear: useCrearUsuario(), reenviar: useReenviarInvitacion(), estado: useCambiarEstadoUsuario() }),
      { wrapper },
    )

    await result.current.crear.mutateAsync({ email: 'a@test.local', roles: ['USUARIO'] })
    await result.current.reenviar.mutateAsync('u-1')
    await result.current.estado.mutateAsync({ codigo: 'u-1', accion: 'desactivar' })

    expect(post.mock.calls.map(([url]) => url)).toEqual([
      '/usuarios',
      '/usuarios/u-1/reenviar-invitacion',
      '/usuarios/u-1/desactivar',
    ])
    expect(invalidar).toHaveBeenCalledTimes(3)
    expect(invalidar).toHaveBeenCalledWith({ queryKey: usuariosKeys.lists() })
  })

  it('un acceso denegado llega como ApiError, sin datos en caché y sin el texto en la clave', async () => {
    vi.spyOn(apiClient, 'post').mockRejectedValue(new ApiError('ERR_AUTH_08', 'No tenés permiso'))
    const { queryClient, wrapper } = entorno()
    const busqueda = nuevaBusqueda('  secreto@test.local ')
    const { result } = renderHook(() => useUsuarios({ busqueda, page: 1, pageSize: 20 }), { wrapper })

    await waitFor(() => expect(result.current.isError).toBe(true))

    expect(result.current.error).toBeInstanceOf(ApiError)
    expect(result.current.data).toBeUndefined()
    const claves = JSON.stringify(queryClient.getQueryCache().getAll().map((q) => q.queryKey))
    expect(claves).not.toContain('secreto')
  })
})
