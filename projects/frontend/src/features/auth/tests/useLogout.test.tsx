import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, renderHook, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import {
  createMemoryHistory,
  createRootRoute,
  createRoute,
  createRouter,
  RouterProvider,
} from '@tanstack/react-router'
import type { ReactNode } from 'react'
import { AdminLayout, UsuarioLayout } from '@/layouts'
import { apiClient, ApiError } from '@/store'
import { authKeys } from '../api/keys'
import { manejarSesionExpirada } from '../api/manejarSesionExpirada'
import { useLogout } from '../api/useLogout'

const navigate = vi.hoisted(() => vi.fn())
vi.mock('@tanstack/react-router', async (original) => ({
  ...(await original<typeof import('@tanstack/react-router')>()),
  useNavigate: () => navigate,
}))

const usuario = { codigo: 'u-1', email: 'a@b.co', roles: ['USUARIO'] }

function clienteConSesion() {
  const queryClient = new QueryClient()
  queryClient.setQueryData(authKeys.session, usuario)
  queryClient.setQueryData(['pedidos', 'list'], [1])
  return queryClient
}

function envolver(queryClient: QueryClient) {
  return ({ children }: { children: ReactNode }) => (
    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  )
}

beforeEach(() => {
  vi.restoreAllMocks()
  navigate.mockReset()
})

describe('useLogout', () => {
  it.each([
    ['responde', () => Promise.resolve({ data: null })],
    ['no responde', () => Promise.reject(new ApiError('CLIENT_NETWORK', 'sin conexión'))],
  ])('si el servidor %s, limpia la caché y va al login', async (_caso, servidor) => {
    const post = vi.spyOn(apiClient, 'post').mockImplementation(servidor)
    const queryClient = clienteConSesion()

    const { result } = renderHook(() => useLogout(), { wrapper: envolver(queryClient) })
    result.current.mutate()

    await waitFor(() => expect(navigate).toHaveBeenCalledWith({ to: '/login' }))
    expect(post).toHaveBeenCalledWith('/auth/logout', undefined, expect.objectContaining({ timeout: 15000 }))
    expect(queryClient.getQueryData(authKeys.session)).toBeNull()
    expect(queryClient.getQueryData(['pedidos', 'list'])).toBeUndefined()
  })
})

describe('manejarSesionExpirada', () => {
  it('con sesión limpia la caché y redirige al login', () => {
    const queryClient = clienteConSesion()
    const irALogin = vi.fn()

    manejarSesionExpirada(queryClient, '/panel', irALogin)

    expect(queryClient.getQueryData(authKeys.session)).toBeNull()
    expect(queryClient.getQueryData(['pedidos', 'list'])).toBeUndefined()
    expect(irALogin).toHaveBeenCalledTimes(1)
  })

  it('sin sesión previa no redirige (evita el bucle en /login)', () => {
    const irALogin = vi.fn()

    manejarSesionExpirada(new QueryClient(), '/login', irALogin)
    manejarSesionExpirada(new QueryClient(), '/panel', irALogin)

    expect(irALogin).not.toHaveBeenCalled()
  })

  it('estando ya en /login no vuelve a navegar', () => {
    const irALogin = vi.fn()

    manejarSesionExpirada(clienteConSesion(), '/login', irALogin)

    expect(irALogin).not.toHaveBeenCalled()
  })
})

describe.each([
  ['AdminLayout', AdminLayout],
  ['UsuarioLayout', UsuarioLayout],
])('%s', (_nombre, Layout) => {
  it('"Cerrar sesión" del menú llama al backend', async () => {
    const post = vi.spyOn(apiClient, 'post').mockResolvedValue({ data: null })
    const raiz = createRootRoute()
    const ruta = createRoute({
      getParentRoute: () => raiz,
      path: '/',
      component: () => (
        <Layout title="Panel" user={{ nombre: 'Ana', rol: 'Rol' }}>
          contenido
        </Layout>
      ),
    })
    const router = createRouter({
      routeTree: raiz.addChildren([ruta]),
      history: createMemoryHistory({ initialEntries: ['/'] }),
    })
    const user = userEvent.setup()
    render(<RouterProvider router={router} />, { wrapper: envolver(new QueryClient()) })

    await user.click(await screen.findByRole('button', { name: /Ana/ }))
    await user.click(await screen.findByRole('menuitem', { name: 'Cerrar sesión' }))

    await waitFor(() => expect(post).toHaveBeenCalledWith('/auth/logout', undefined, expect.anything()))
  })
})
