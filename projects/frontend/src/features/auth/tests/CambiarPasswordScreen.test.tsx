import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import {
  createMemoryHistory,
  createRootRoute,
  createRoute,
  createRouter,
  Outlet,
  RouterProvider,
} from '@tanstack/react-router'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { AxiosError, type AxiosAdapter, type InternalAxiosRequestConfig } from 'axios'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { toastSuccess } from '@/components/ui/toast'
import { i18n } from '@/shared/language'
import { apiClient, ApiError } from '@/store'
import { authKeys } from '../api'
import { CambiarPasswordScreen } from '../screens'

vi.mock('@/components/ui/toast', () => ({ toastSuccess: vi.fn() }))

function montar() {
  const raiz = createRootRoute({ component: Outlet })
  const rutas = [
    createRoute({ getParentRoute: () => raiz, path: '/change-password', component: CambiarPasswordScreen }),
    createRoute({ getParentRoute: () => raiz, path: '/login', component: () => <p>pantalla de login</p> }),
  ]
  const router = createRouter({
    routeTree: raiz.addChildren(rutas),
    history: createMemoryHistory({ initialEntries: ['/change-password'] }),
  })
  const queryClient = new QueryClient()
  queryClient.setQueryData(authKeys.session, { codigo: 'u', email: 'a@b.co', roles: ['USUARIO'] })
  queryClient.setQueryData(['pedidos', 'list'], [1])
  render(
    <QueryClientProvider client={queryClient}>
      <RouterProvider router={router as never} />
    </QueryClientProvider>,
  )
  return { queryClient, user: userEvent.setup() }
}

async function completar(user: ReturnType<typeof userEvent.setup>, { enviar = true } = {}) {
  await user.type(await screen.findByLabelText('Contraseña actual'), 'ClaveActual123')
  await user.type(screen.getByLabelText('Contraseña nueva'), 'ClaveNueva456')
  await user.type(screen.getByLabelText('Repite la contraseña nueva'), `ClaveNueva456${enviar ? '{Enter}' : ''}`)
}

const sesionDe = (queryClient: QueryClient) => queryClient.getQueryData(authKeys.session)

describe('CambiarPasswordScreen', () => {
  const adaptadorOriginal = apiClient.defaults.adapter

  beforeEach(() => {
    vi.restoreAllMocks()
    vi.mocked(toastSuccess).mockClear()
  })

  afterEach(() => {
    apiClient.defaults.adapter = adaptadorOriginal
  })

  it('con Enter envía solo las dos contraseñas, limpia la sesión, avisa y va al login', async () => {
    const post = vi.spyOn(apiClient, 'post').mockResolvedValue({ data: undefined })
    const { queryClient, user } = montar()

    await completar(user)

    expect(await screen.findByText('pantalla de login')).toBeInTheDocument()
    expect(post).toHaveBeenCalledTimes(1)
    expect(post).toHaveBeenCalledWith('/auth/password/change', {
      passwordActual: 'ClaveActual123',
      passwordNueva: 'ClaveNueva456',
    })
    expect(sesionDe(queryClient)).toBeNull()
    expect(queryClient.getQueryData(['pedidos', 'list'])).toBeUndefined()
    expect(toastSuccess).toHaveBeenCalledWith('Contraseña cambiada', 'Inicia sesión con la contraseña nueva')
    // * Las contraseñas no quedan guardadas en la caché de mutaciones.
    await waitFor(() => expect(queryClient.getMutationCache().getAll()).toHaveLength(0))
  })

  it('mientras envía, el botón queda deshabilitado y no se envía dos veces', async () => {
    const post = vi.spyOn(apiClient, 'post').mockReturnValue(new Promise(() => {}))
    const { user } = montar()

    await completar(user)
    await user.type(screen.getByLabelText('Repite la contraseña nueva'), '{Enter}')

    expect(screen.getByRole('button', { name: 'Guardando…' })).toBeDisabled()
    expect(post).toHaveBeenCalledTimes(1)
  })

  it('no envía si las contraseñas nuevas no coinciden', async () => {
    const post = vi.spyOn(apiClient, 'post')
    const { user } = montar()

    await completar(user, { enviar: false })
    await user.type(screen.getByLabelText('Repite la contraseña nueva'), 'x{Enter}')

    expect(await screen.findByText('Las contraseñas no coinciden')).toBeInTheDocument()
    expect(post).not.toHaveBeenCalled()
  })

  it.each([
    ['ERR_AUTH_12', 400, 'La contraseña actual no es correcta', 'Contraseña actual'],
    ['ERR_AUTH_06', 400, 'La contraseña nueva debe ser distinta de la actual', 'Contraseña nueva'],
    ['ERR_SYS_01', 400, 'La contraseña es demasiado larga', 'Contraseña nueva'],
  ])('%s se muestra bajo su campo y la sesión sigue', async (code, status, mensaje, campo) => {
    vi.spyOn(apiClient, 'post').mockRejectedValue(new ApiError(code, mensaje, status))
    const { queryClient, user } = montar()

    await completar(user)

    expect(await screen.findByText(mensaje)).toBeInTheDocument()
    expect(screen.getByLabelText(campo, { exact: true })).toHaveAccessibleDescription(mensaje)
    expect(screen.queryByText('pantalla de login')).not.toBeInTheDocument()
    expect(sesionDe(queryClient)).not.toBeNull()
  })

  it.each([
    ['ERR_AUTH_13', 423, 'Demasiados intentos con la contraseña actual'],
    ['ERR_SYS_02', 429, 'Demasiadas solicitudes'],
  ])('%s se muestra como aviso y la sesión sigue', async (code, status, mensaje) => {
    vi.spyOn(apiClient, 'post').mockRejectedValue(new ApiError(code, mensaje, status))
    const { queryClient, user } = montar()

    await completar(user)

    expect(await screen.findByRole('alert')).toHaveTextContent(mensaje)
    expect(sesionDe(queryClient)).not.toBeNull()
  })

  it('con el access token vencido renueva y reintenta sin quedarse colgado', async () => {
    const llamadas: string[] = []
    let renovada = false
    const responder = (config: InternalAxiosRequestConfig, status: number, data: unknown) => {
      const response = { config, status, data, statusText: '', headers: {} }
      return status < 400
        ? Promise.resolve(response)
        : Promise.reject(new AxiosError('fallo', 'ERR_BAD_REQUEST', config, undefined, response))
    }
    const sobre = { status: 'success', data: null, message: 'ok', timestamp: '' }
    const adaptador: AxiosAdapter = async (config) => {
      llamadas.push(config.url ?? '')
      if (config.url === '/auth/csrf') return responder(config, 204, '')
      if (config.url === '/auth/refresh') {
        renovada = true
        return responder(config, 200, sobre)
      }
      return renovada
        ? responder(config, 200, sobre)
        : responder(config, 401, { ...sobre, status: 'failed', error: 'ERR_AUTH_07' })
    }
    apiClient.defaults.adapter = adaptador
    const { user } = montar()

    await completar(user)

    expect(await screen.findByText('pantalla de login')).toBeInTheDocument()
    expect(llamadas.filter((url) => url === '/auth/password/change')).toHaveLength(2)
    expect(llamadas.filter((url) => url === '/auth/refresh')).toHaveLength(1)
  })

  it('se muestra en inglés', async () => {
    await i18n.changeLanguage('en')
    montar()

    expect(await screen.findByLabelText('Current password')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Change password' })).toBeInTheDocument()
    expect(screen.getByText(/All your sessions will be closed/)).toBeInTheDocument()
  })
})
