import { StrictMode } from 'react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import {
  createMemoryHistory,
  createRootRoute,
  createRoute,
  createRouter,
  Outlet,
  RouterProvider,
} from '@tanstack/react-router'
import { apiClient, ApiError } from '@/store'
import { authKeys } from '../api'
import { ResetPasswordPage } from '../screens'

// * Con un router de verdad: lo que se prueba es qué pasa con la URL y el historial.
function renderEn(url: string) {
  const raiz = createRootRoute({ component: Outlet })
  const rutas = [
    createRoute({ getParentRoute: () => raiz, path: '/reset-password', component: ResetPasswordPage }),
    createRoute({ getParentRoute: () => raiz, path: '/login', component: () => <p>pantalla de login</p> }),
    createRoute({ getParentRoute: () => raiz, path: '/forgot-password', component: () => <p>pedir</p> }),
  ]
  const history = createMemoryHistory({ initialEntries: [url] })
  const router = createRouter({ routeTree: raiz.addChildren(rutas), history })
  const queryClient = new QueryClient()
  queryClient.setQueryData(authKeys.session, { codigo: 'u', email: 'a@b.co', roles: ['USUARIO'] })
  render(
    <StrictMode>
      <QueryClientProvider client={queryClient}>
        <RouterProvider router={router as never} />
      </QueryClientProvider>
    </StrictMode>,
  )
  return { history, router, queryClient }
}

async function completar(password: string, confirmacion = password) {
  const user = userEvent.setup()
  const nueva = await screen.findByLabelText('Contraseña nueva')
  const repetida = screen.getByLabelText('Repite la contraseña')
  await user.clear(nueva)
  await user.type(nueva, password)
  await user.clear(repetida)
  await user.type(repetida, confirmacion)
  await user.click(screen.getByRole('button', { name: 'Cambiar contraseña' }))
}

describe('ResetPasswordScreen', () => {
  beforeEach(() => vi.restoreAllMocks())

  it('quita el token de la URL sin agregar una entrada al historial', async () => {
    const { history, router } = renderEn('/reset-password#token=abc123')

    await screen.findByLabelText('Contraseña nueva')

    await waitFor(() => expect(router.state.location.hash).toBe(''))
    expect(history.location.href).toBe('/reset-password')
    expect(history.length).toBe(1)
  })

  it('manda el token en el body, y tras el éxito va al login con la sesión en null', async () => {
    const post = vi.spyOn(apiClient, 'post').mockResolvedValue({ data: undefined })
    const { queryClient } = renderEn('/reset-password#token=abc123')

    await completar('ClaveNueva123')

    expect(await screen.findByText('pantalla de login')).toBeInTheDocument()
    expect(post).toHaveBeenCalledWith('/auth/password/reset', { token: 'abc123', password: 'ClaveNueva123' })
    expect(queryClient.getQueryData(authKeys.session)).toBeNull()
  })

  it('con ERR_AUTH_06 muestra el error bajo el campo y conserva el token para reintentar', async () => {
    const post = vi
      .spyOn(apiClient, 'post')
      .mockRejectedValueOnce(new ApiError('ERR_AUTH_06', 'Tiene que ser distinta de la actual', 400))
      .mockResolvedValue({ data: undefined })
    renderEn('/reset-password#token=abc123')

    await completar('ClaveActual123')
    expect(await screen.findByText('Tiene que ser distinta de la actual')).toBeInTheDocument()
    await completar('ClaveNueva123')

    expect(await screen.findByText('pantalla de login')).toBeInTheDocument()
    expect(post).toHaveBeenLastCalledWith('/auth/password/reset', { token: 'abc123', password: 'ClaveNueva123' })
  })

  it('con ERR_AUTH_05 ofrece pedir otro enlace', async () => {
    vi.spyOn(apiClient, 'post').mockRejectedValue(new ApiError('ERR_AUTH_05', 'vencido', 400))
    renderEn('/reset-password#token=viejo')

    await completar('ClaveNueva123')

    expect(await screen.findByRole('link', { name: 'Pedir un enlace nuevo' })).toHaveAttribute(
      'href',
      '/forgot-password',
    )
  })

  it('con 429 muestra el aviso y deja el formulario', async () => {
    vi.spyOn(apiClient, 'post').mockRejectedValue(new ApiError('ERR_SYS_02', 'Demasiadas solicitudes', 429))
    renderEn('/reset-password#token=abc123')

    await completar('ClaveNueva123')

    expect(await screen.findByRole('alert')).toHaveTextContent('Demasiadas solicitudes')
    expect(screen.getByLabelText('Contraseña nueva')).toBeInTheDocument()
  })

  it('envía con Enter sin haber salido del último campo', async () => {
    const post = vi.spyOn(apiClient, 'post').mockResolvedValue({ data: undefined })
    renderEn('/reset-password#token=abc123')
    const user = userEvent.setup()

    await user.type(await screen.findByLabelText('Contraseña nueva'), 'ClaveNueva123')
    await user.tab()
    await user.keyboard('ClaveNueva123{Enter}')

    await waitFor(() => expect(post).toHaveBeenCalledOnce())
  })

  it('sin token muestra el enlace inválido', async () => {
    renderEn('/reset-password')

    expect(await screen.findByRole('link', { name: 'Pedir un enlace nuevo' })).toBeInTheDocument()
    expect(screen.queryByLabelText('Contraseña nueva')).not.toBeInTheDocument()
  })

  it('no envía si las contraseñas no coinciden', async () => {
    const post = vi.spyOn(apiClient, 'post')
    renderEn('/reset-password#token=abc123')

    await completar('ClaveNueva123', 'ClaveNueva999')

    expect(await screen.findByText('Las contraseñas no coinciden')).toBeInTheDocument()
    expect(post).not.toHaveBeenCalled()
  })
})
