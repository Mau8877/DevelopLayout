import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { apiClient, ApiError } from '@/store'
import { SesionesAdminScreen } from '../screens/SesionesAdminScreen'

const sesion = {
  codigo: 's-1',
  usuarioCodigo: 'u-9',
  tipoCliente: 'WEB',
  ip: '10.0.0.1',
  userAgent: 'Navegador de prueba',
  creadaEn: '2026-03-10T10:00:00Z',
  expiraEn: '2026-03-17T10:00:00Z',
}
const pagina = (data: unknown[]) => ({ data: { data, meta: { page: 1, pageSize: 20, totalItems: data.length, totalPages: 1 } } })

function montar() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  render(
    <QueryClientProvider client={queryClient}>
      <SesionesAdminScreen />
    </QueryClientProvider>,
  )
  return { queryClient, user: userEvent.setup() }
}

async function buscar(user: ReturnType<typeof userEvent.setup>, correo: string) {
  const campo = screen.getByLabelText('Correo del usuario')
  await user.clear(campo)
  await user.type(campo, correo)
  await user.click(screen.getByRole('button', { name: 'Buscar' }))
}

describe('SesionesAdminScreen', () => {
  beforeEach(() => vi.restoreAllMocks())

  it('no consulta con un correo inválido y muestra el error bajo el campo', async () => {
    const post = vi.spyOn(apiClient, 'post')
    const { user } = montar()

    await buscar(user, 'no-es-correo')

    expect(await screen.findByText('El correo no es válido')).toBeInTheDocument()
    expect(post).not.toHaveBeenCalled()
  })

  it('busca por POST, lista las sesiones y no pone el correo en la clave de caché', async () => {
    const post = vi.spyOn(apiClient, 'post').mockResolvedValue(pagina([sesion]))
    const { queryClient, user } = montar()

    await buscar(user, 'ana@test.local')

    expect(await screen.findByText('Navegador de prueba')).toBeInTheDocument()
    expect(post).toHaveBeenCalledWith('/admin/sesiones/buscar', { email: 'ana@test.local', page: 1, pageSize: 20 })
    const claves = JSON.stringify(queryClient.getQueryCache().getAll().map((q) => q.queryKey))
    expect(claves).not.toContain('ana@test.local')
  })

  it('un correo sin sesiones muestra el estado vacío, sin acciones', async () => {
    vi.spyOn(apiClient, 'post').mockResolvedValue(pagina([]))
    const { user } = montar()

    await buscar(user, 'nadie@test.local')

    expect(await screen.findByText('No hay sesiones activas para ese correo')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Cerrar todas' })).not.toBeInTheDocument()
  })

  it('cerrar una sesión pide confirmación y vuelve a consultar', async () => {
    const post = vi.spyOn(apiClient, 'post').mockResolvedValue(pagina([sesion]))
    const borrar = vi.spyOn(apiClient, 'delete').mockResolvedValue({ data: null })
    const { user } = montar()
    await buscar(user, 'ana@test.local')

    await user.click(await screen.findByRole('button', { name: 'Cerrar sesión de este dispositivo' }))
    expect(borrar).not.toHaveBeenCalled()
    await user.click(within(await screen.findByRole('alertdialog')).getByRole('button', { name: 'Cerrar' }))

    await waitFor(() => expect(borrar).toHaveBeenCalledWith('/admin/sesiones/s-1'))
    await waitFor(() => expect(post).toHaveBeenCalledTimes(2))
  })

  it('cerrar todas llama al endpoint del usuario', async () => {
    const post = vi.spyOn(apiClient, 'post').mockResolvedValue(pagina([sesion]))
    const { user } = montar()
    await buscar(user, 'ana@test.local')

    await user.click(await screen.findByRole('button', { name: 'Cerrar todas' }))
    await user.click(within(await screen.findByRole('alertdialog')).getByRole('button', { name: 'Cerrar' }))

    await waitFor(() => expect(post).toHaveBeenCalledWith('/admin/usuarios/u-9/sesiones/cerrar'))
  })

  it('un fallo muestra el mensaje del backend con Reintentar', async () => {
    vi.spyOn(apiClient, 'post').mockRejectedValue(new ApiError('ERR_AUTH_08', 'Acceso denegado'))
    const { user } = montar()

    await buscar(user, 'ana@test.local')

    expect(await screen.findByText('Acceso denegado')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Reintentar' })).toBeInTheDocument()
  })
})
