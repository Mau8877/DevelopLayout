import { screen, waitFor } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { apiClient, ApiError } from '@/store'
import { UsuariosScreen } from '../screens'
import { montar, pagina, usuario } from './soporte'

// * La tabla y las tarjetas conviven (se ocultan por CSS): se mira la tabla.
const tabla = () => screen.getByRole('table')

describe('UsuariosScreen: listado', () => {
  beforeEach(() => vi.restoreAllMocks())

  it('muestra correo, roles, estado e invitación pendiente', async () => {
    vi.spyOn(apiClient, 'post').mockResolvedValue(
      pagina([usuario({ roles: ['SUPER_ADMIN'], estado: 'INACTIVO', invitacionPendiente: true })]),
    )

    montar(<UsuariosScreen />)

    await waitFor(() => expect(tabla()).toHaveTextContent('ana@test.local'))
    for (const texto of ['Administrador', 'Inactiva', 'Pendiente']) expect(tabla()).toHaveTextContent(texto)
  })

  it('busca con espera, por el body, sin dejar el correo en la clave de caché', async () => {
    const post = vi.spyOn(apiClient, 'post').mockResolvedValue(pagina([usuario()]))
    const { queryClient, user } = montar(<UsuariosScreen />)

    await user.type(screen.getByPlaceholderText('Buscar por correo'), 'ana@test')

    await waitFor(() =>
      expect(post).toHaveBeenLastCalledWith('/users/search', {
        page: 1,
        pageSize: 20,
        search: 'ana@test',
        estado: undefined,
        rol: undefined,
      }),
    )
    // * Una consulta al abrir y otra al terminar de escribir, no una por tecla.
    expect(post).toHaveBeenCalledTimes(2)
    const claves = JSON.stringify(queryClient.getQueryCache().getAll().map((q) => q.queryKey))
    expect(claves).not.toContain('ana@test')
    expect(window.location.href).not.toContain('ana')
  })

  it('filtrar vuelve a la primera página', async () => {
    const post = vi.spyOn(apiClient, 'post').mockResolvedValue(pagina([usuario()], 3))
    const { user } = montar(<UsuariosScreen />)
    await waitFor(() => expect(tabla()).toHaveTextContent('ana@test.local'))
    await user.click(screen.getByRole('button', { name: /siguiente/i }))
    await waitFor(() => expect(post.mock.lastCall?.[1]).toMatchObject({ page: 2 }))

    await user.click(screen.getByRole('combobox', { name: 'Estado' }))
    await user.click(await screen.findByRole('option', { name: 'Inactiva' }))

    await waitFor(() => expect(post.mock.lastCall?.[1]).toMatchObject({ page: 1, estado: 'INACTIVO' }))
  })

  it('sin resultados muestra el estado vacío', async () => {
    vi.spyOn(apiClient, 'post').mockResolvedValue(pagina([]))

    montar(<UsuariosScreen />)

    expect((await screen.findAllByText('No hay usuarios con esos criterios')).length).toBeGreaterThan(0)
  })

  it('un acceso denegado muestra el mensaje, sin datos, y permite reintentar', async () => {
    const post = vi
      .spyOn(apiClient, 'post')
      .mockRejectedValueOnce(new ApiError('ERR_AUTH_08', 'No tenés permiso'))
      .mockResolvedValue(pagina([usuario()]))
    const { user } = montar(<UsuariosScreen />)

    expect(await screen.findByText('No tenés permiso')).toBeInTheDocument()
    expect(screen.queryByRole('table')).not.toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: 'Reintentar' }))

    await waitFor(() => expect(tabla()).toHaveTextContent('ana@test.local'))
    expect(post).toHaveBeenCalledTimes(2)
  })
})
