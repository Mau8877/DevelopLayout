import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { apiClient, ApiError } from '@/store'
import { BitacoraScreen } from '../screens/BitacoraScreen'

const evento = {
  codigo: 'b-1',
  fecha: '2026-03-10T10:00:00Z',
  accion: 'AUTH_LOGIN_FALLO',
  modulo: 'auth',
  resultado: 'FALLO',
  detalle: { nota: '<img src=x onerror=alert(1)>' },
}
const pagina = (data: unknown[], totalPages = 1) => ({
  data: { data, meta: { page: 1, pageSize: 20, totalItems: data.length * totalPages, totalPages } },
})

function montar() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  render(
    <QueryClientProvider client={queryClient}>
      <BitacoraScreen />
    </QueryClientProvider>,
  )
  return userEvent.setup()
}

const ultimosParams = (get: { mock: { calls: unknown[][] } }) =>
  (get.mock.calls.at(-1)?.[1] as { params: Record<string, unknown> }).params

describe('BitacoraScreen', () => {
  beforeEach(() => vi.restoreAllMocks())

  it('consulta la primera página y muestra los eventos', async () => {
    const get = vi.spyOn(apiClient, 'get').mockResolvedValue(pagina([evento]))
    montar()

    expect((await screen.findAllByText('AUTH_LOGIN_FALLO')).length).toBeGreaterThan(0)
    expect(get.mock.calls[0][0]).toBe('/bitacora')
    expect(ultimosParams(get)).toEqual({ page: 1, pageSize: 20 })
  })

  it('al filtrar manda filter[...] y vuelve a la página 1', async () => {
    const get = vi.spyOn(apiClient, 'get').mockResolvedValue(pagina([evento], 3))
    const user = montar()
    await user.click(await screen.findByRole('button', { name: 'Página 2' }))
    await waitFor(() => expect(ultimosParams(get).page).toBe(2))

    await user.type(screen.getByLabelText('Acción'), 'AUTH_LOGOUT')
    await user.click(screen.getByRole('button', { name: 'Filtrar' }))

    await waitFor(() => expect(ultimosParams(get)['filter[accion]']).toBe('AUTH_LOGOUT'))
    expect(ultimosParams(get).page).toBe(1)
  })

  it('un rango invertido muestra el error y no consulta', async () => {
    const get = vi.spyOn(apiClient, 'get').mockResolvedValue(pagina([]))
    const user = montar()
    await screen.findAllByText('No hay eventos con esos filtros')

    await user.type(screen.getByLabelText('Desde'), '2026-03-02')
    await user.type(screen.getByLabelText('Hasta'), '2026-03-01')
    await user.click(screen.getByRole('button', { name: 'Filtrar' }))

    expect(await screen.findByText('La fecha inicial no puede ser posterior a la final')).toBeInTheDocument()
    expect(get).toHaveBeenCalledTimes(1)
  })

  it('el detalle se muestra como texto, nunca como HTML', async () => {
    vi.spyOn(apiClient, 'get').mockResolvedValue(pagina([evento]))
    const user = montar()

    await user.click((await screen.findAllByRole('button', { name: 'Ver detalle' }))[0])

    const dialogo = await screen.findByRole('dialog')
    expect(within(dialogo).getByText(/<img src=x onerror=alert\(1\)>/)).toBeInTheDocument()
    expect(dialogo.querySelector('img')).toBeNull()
  })

  it('un fallo muestra el mensaje con Reintentar', async () => {
    vi.spyOn(apiClient, 'get').mockRejectedValue(new ApiError('ERR_AUTH_08', 'Acceso denegado'))
    montar()

    expect(await screen.findByText('Acceso denegado')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Reintentar' })).toBeInTheDocument()
  })
})
