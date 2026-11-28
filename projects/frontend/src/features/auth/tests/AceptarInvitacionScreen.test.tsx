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
import { AceptarInvitacionPage } from '../screens'

// * Con un router de verdad: lo que se prueba es qué pasa con la URL y el historial.
function renderEn(url: string) {
  const raiz = createRootRoute({ component: Outlet })
  const rutas = [
    createRoute({ getParentRoute: () => raiz, path: '/accept-invitation', component: AceptarInvitacionPage }),
    createRoute({ getParentRoute: () => raiz, path: '/login', component: () => <p>pantalla de login</p> }),
  ]
  const history = createMemoryHistory({ initialEntries: [url] })
  const router = createRouter({ routeTree: raiz.addChildren(rutas), history })
  render(
    <StrictMode>
      <QueryClientProvider client={new QueryClient()}>
        <RouterProvider router={router as never} />
      </QueryClientProvider>
    </StrictMode>,
  )
  return { history, router, user: userEvent.setup() }
}

async function completarConEnter(user: ReturnType<typeof userEvent.setup>, password: string, repetida = password) {
  const nueva = await screen.findByLabelText('Contraseña nueva')
  await user.clear(nueva)
  await user.type(nueva, password)
  const confirmacion = screen.getByLabelText('Repite la contraseña')
  await user.clear(confirmacion)
  await user.type(confirmacion, `${repetida}{Enter}`)
}

describe('AceptarInvitacionScreen', () => {
  beforeEach(() => vi.restoreAllMocks())

  it('quita el token de la URL sin agregar una entrada al historial', async () => {
    const { history, router } = renderEn('/accept-invitation#token=abc123')

    await screen.findByLabelText('Contraseña nueva')

    await waitFor(() => expect(router.state.location.hash).toBe(''))
    expect(history.location.href).toBe('/accept-invitation')
    expect(history.length).toBe(1)
    expect(JSON.stringify([localStorage, sessionStorage])).not.toContain('abc123')
  })

  it('envía con Enter el token en el body y va al login', async () => {
    const post = vi.spyOn(apiClient, 'post').mockResolvedValue({ data: undefined })
    const { user } = renderEn('/accept-invitation#token=abc123')

    await completarConEnter(user, 'ClaveElegida789') // gitleaks:allow

    expect(await screen.findByText('pantalla de login')).toBeInTheDocument()
    expect(post).toHaveBeenCalledTimes(1)
    expect(post).toHaveBeenCalledWith('/auth/invitation/accept', { token: 'abc123', password: 'ClaveElegida789' }) // gitleaks:allow
  })

  it('no envía si las contraseñas no coinciden', async () => {
    const post = vi.spyOn(apiClient, 'post')
    const { user } = renderEn('/accept-invitation#token=abc123')

    await completarConEnter(user, 'ClaveElegida789', 'ClaveDistinta789') // gitleaks:allow

    expect(await screen.findByText('Las contraseñas no coinciden')).toBeInTheDocument()
    expect(post).not.toHaveBeenCalled()
  })

  it('una contraseña rechazada se muestra bajo el campo y permite reintentar', async () => {
    const post = vi
      .spyOn(apiClient, 'post')
      .mockRejectedValueOnce(new ApiError('ERR_SYS_01', 'La contraseña es demasiado larga', 400))
      .mockResolvedValue({ data: undefined })
    const { user } = renderEn('/accept-invitation#token=abc123')

    await completarConEnter(user, 'ClaveElegida789') // gitleaks:allow
    expect(await screen.findByText('La contraseña es demasiado larga')).toBeInTheDocument()
    await completarConEnter(user, 'OtraClave789') // gitleaks:allow

    expect(await screen.findByText('pantalla de login')).toBeInTheDocument()
    expect(post).toHaveBeenLastCalledWith('/auth/invitation/accept', { token: 'abc123', password: 'OtraClave789' }) // gitleaks:allow
  })

  it('una invitación que no sirve explica que hay que pedir otra', async () => {
    vi.spyOn(apiClient, 'post').mockRejectedValue(new ApiError('ERR_AUTH_11', 'vencida', 400))
    const { user } = renderEn('/accept-invitation#token=viejo')

    await completarConEnter(user, 'ClaveElegida789') // gitleaks:allow

    expect(await screen.findByText(/pide a un administrador/i)).toBeInTheDocument()
    expect(screen.queryByLabelText('Contraseña nueva')).not.toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Ir al inicio de sesión' })).toHaveAttribute('href', '/login')
  })

  it('sin token no muestra el formulario ni envía nada', async () => {
    const post = vi.spyOn(apiClient, 'post')
    renderEn('/accept-invitation')

    expect(await screen.findByRole('link', { name: 'Ir al inicio de sesión' })).toBeInTheDocument()
    expect(screen.queryByLabelText('Contraseña nueva')).not.toBeInTheDocument()
    expect(post).not.toHaveBeenCalled()
  })
})
