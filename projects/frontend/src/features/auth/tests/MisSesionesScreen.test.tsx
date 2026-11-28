import { beforeEach, describe, expect, it, vi } from 'vitest'
import { render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { apiClient, ApiError } from '@/store'
import * as toast from '@/components/ui/toast'
import { MisSesionesScreen } from '../screens'

const base = { creadaEn: '2026-03-10T10:00:00Z', expiraEn: '2026-03-17T10:00:00Z', ip: '10.0.0.1' }
const actual = {
  ...base,
  codigo: 's-actual',
  tipoCliente: 'WEB',
  userAgent: 'Mozilla/5.0 (Windows NT 10.0) Chrome/140.0 Safari/537.36',
  actual: true,
}
const otra = { ...base, codigo: 's-otra', tipoCliente: 'MOBILE', userAgent: 'Dart/3.12', actual: false }

const pagina = (data: unknown[]) => ({
  data: { data, meta: { page: 1, pageSize: 20, totalItems: data.length, totalPages: 1 } },
})

function montar() {
  render(
    <QueryClientProvider client={new QueryClient({ defaultOptions: { queries: { retry: false } } })}>
      <MisSesionesScreen />
    </QueryClientProvider>,
  )
  return userEvent.setup()
}

const botonesDeCierre = () => screen.queryAllByRole('button', { name: 'Cerrar esta otra sesión' })
const confirmar = async (user: ReturnType<typeof userEvent.setup>) =>
  user.click(within(await screen.findByRole('alertdialog')).getByRole('button', { name: 'Cerrar' }))

describe('MisSesionesScreen', () => {
  beforeEach(() => vi.restoreAllMocks())

  it('lista con el resumen del dispositivo, marca la actual y no le ofrece cierre', async () => {
    const get = vi.spyOn(apiClient, 'get').mockResolvedValue(pagina([actual, otra]))
    montar()

    expect(await screen.findByText('Chrome en Windows')).toBeInTheDocument()
    expect(screen.getByText('App móvil')).toBeInTheDocument()
    expect(screen.getByText('Esta sesión')).toBeInTheDocument()
    expect(botonesDeCierre()).toHaveLength(1)
    expect(get).toHaveBeenCalledWith('/auth/sessions', { params: { page: 1, pageSize: 20 } })
  })

  it('no cierra nada sin confirmar; al confirmar cierra esa y vuelve a pedir el listado', async () => {
    const get = vi.spyOn(apiClient, 'get').mockResolvedValue(pagina([actual, otra]))
    const borrar = vi.spyOn(apiClient, 'delete').mockResolvedValue({ data: null })
    const user = montar()

    await user.click((await screen.findAllByRole('button', { name: 'Cerrar esta otra sesión' }))[0])
    expect(borrar).not.toHaveBeenCalled()
    get.mockResolvedValue(pagina([actual]))
    await confirmar(user)

    await waitFor(() => expect(borrar).toHaveBeenCalledWith('/auth/sessions/s-otra'))
    await waitFor(() => expect(screen.queryByText('App móvil')).not.toBeInTheDocument())
  })

  it('cancelar la confirmación no cierra nada', async () => {
    vi.spyOn(apiClient, 'get').mockResolvedValue(pagina([actual, otra]))
    const post = vi.spyOn(apiClient, 'post')
    const user = montar()

    await user.click(await screen.findByRole('button', { name: 'Cerrar todas las demás' }))
    await user.click(within(await screen.findByRole('alertdialog')).getByRole('button', { name: 'Cancelar' }))

    expect(post).not.toHaveBeenCalled()
  })

  it('cerrar todas las demás llama al endpoint después de confirmar', async () => {
    vi.spyOn(apiClient, 'get').mockResolvedValue(pagina([actual, otra]))
    const post = vi.spyOn(apiClient, 'post').mockResolvedValue({ data: null })
    const user = montar()

    await user.click(await screen.findByRole('button', { name: 'Cerrar todas las demás' }))
    await confirmar(user)

    await waitFor(() => expect(post).toHaveBeenCalledWith('/auth/sessions/revoke-others'))
  })

  it('si la sesión ya no existe avisa y refresca el listado', async () => {
    const get = vi.spyOn(apiClient, 'get').mockResolvedValue(pagina([actual, otra]))
    vi.spyOn(apiClient, 'delete').mockRejectedValue(new ApiError('ERR_AUTH_09', 'La sesión no existe', 404))
    const aviso = vi.spyOn(toast, 'toastError')
    const user = montar()

    await user.click((await screen.findAllByRole('button', { name: 'Cerrar esta otra sesión' }))[0])
    const antes = get.mock.calls.length
    await confirmar(user)

    await waitFor(() => expect(aviso).toHaveBeenCalledWith('La sesión no existe'))
    expect(get.mock.calls.length).toBeGreaterThan(antes)
  })

  it('con una sola sesión no se puede "cerrar las demás"', async () => {
    vi.spyOn(apiClient, 'get').mockResolvedValue(pagina([actual]))
    montar()

    await screen.findByText('Esta sesión')

    expect(screen.getByRole('button', { name: 'Cerrar todas las demás' })).toBeDisabled()
    expect(botonesDeCierre()).toHaveLength(0)
  })

  it('un error de carga ofrece reintentar', async () => {
    const get = vi
      .spyOn(apiClient, 'get')
      .mockRejectedValueOnce(new ApiError('CLIENT_NETWORK', 'No pudimos conectarnos'))
      .mockResolvedValue(pagina([actual]))
    const user = montar()

    expect(await screen.findByText('No pudimos conectarnos')).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: 'Reintentar' }))

    expect(await screen.findByText('Esta sesión')).toBeInTheDocument()
    expect(get).toHaveBeenCalledTimes(2)
  })
})
