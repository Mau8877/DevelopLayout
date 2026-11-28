import { screen, waitFor, within } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { toastSuccess } from '@/components/ui/toast'
import { apiClient, ApiError } from '@/store'
import { usuariosKeys } from '../api'
import { RolesModal } from '../components'
import { montar, usuario } from './soporte'

vi.mock('@/components/ui/toast', () => ({ toastSuccess: vi.fn() }))

function abrir(cambios: Parameters<typeof usuario>[0] = {}, esPropia = false) {
  const onClose = vi.fn()
  const montado = montar(<RolesModal usuario={usuario(cambios)} esPropia={esPropia} onClose={onClose} />)
  const invalidar = vi.spyOn(montado.queryClient, 'invalidateQueries')
  return { onClose, invalidar, ...montado }
}

const casilla = (nombre: string) => screen.getByRole('checkbox', { name: nombre })
const guardar = () => screen.getByRole('button', { name: 'Guardar' })
const confirmacion = () => screen.findByRole('alertdialog')

describe('RolesModal', () => {
  beforeEach(() => vi.restoreAllMocks())

  it('marca los roles actuales', () => {
    abrir({ roles: ['USUARIO'] })

    expect(casilla('Usuario')).toBeChecked()
    expect(casilla('Administrador')).not.toBeChecked()
  })

  it('sin ningún rol muestra el error y no pide confirmación', async () => {
    const put = vi.spyOn(apiClient, 'put')
    const { user } = abrir()

    await user.click(casilla('Usuario'))
    await user.click(guardar())

    expect(await screen.findByText('Elige al menos un rol')).toBeInTheDocument()
    expect(screen.queryByRole('alertdialog')).not.toBeInTheDocument()
    expect(put).not.toHaveBeenCalled()
  })

  it('sin cambios cierra sin llamar', async () => {
    const put = vi.spyOn(apiClient, 'put')
    const { user, onClose } = abrir()

    await user.click(guardar())

    await waitFor(() => expect(onClose).toHaveBeenCalled())
    expect(put).not.toHaveBeenCalled()
  })

  it('avisa que se cierran las sesiones y no llama sin confirmar', async () => {
    const put = vi.spyOn(apiClient, 'put')
    const { user, onClose } = abrir()

    await user.click(casilla('Administrador'))
    await user.click(guardar())
    const dialogo = await confirmacion()
    expect(dialogo).toHaveTextContent('Se cerrarán las sesiones abiertas de ana@test.local')
    await user.click(within(dialogo).getByRole('button', { name: 'Cancelar' }))

    expect(put).not.toHaveBeenCalled()
    expect(onClose).not.toHaveBeenCalled()
  })

  it('al confirmar envía el conjunto completo, refresca la lista y cierra', async () => {
    const put = vi.spyOn(apiClient, 'put').mockResolvedValue({ data: usuario() })
    const { user, onClose, invalidar } = abrir()

    await user.click(casilla('Administrador'))
    await user.click(guardar())
    await user.click(within(await confirmacion()).getByRole('button', { name: 'Cambiar roles' }))

    await waitFor(() => expect(onClose).toHaveBeenCalled())
    expect(put).toHaveBeenCalledTimes(1)
    expect(put).toHaveBeenCalledWith('/users/u-1/roles', { roles: ['SUPER_ADMIN', 'USUARIO'] })
    expect(invalidar).toHaveBeenCalledWith({ queryKey: usuariosKeys.lists() })
    expect(toastSuccess).toHaveBeenCalledWith('Roles actualizados')
  })

  it('con el teclado abre la confirmación al pulsar Enter en Guardar', async () => {
    const { user } = abrir()

    await user.click(casilla('Administrador'))
    guardar().focus()
    await user.keyboard('{Enter}')

    expect(await confirmacion()).toBeInTheDocument()
  })

  it('en la cuenta propia no deja quitar el rol de administrador y avisa del cierre', async () => {
    const { user } = abrir({ roles: ['SUPER_ADMIN'] }, true)

    await user.click(screen.getByText('Administrador'))
    expect(casilla('Administrador')).toBeChecked()
    expect(screen.getByText('No puedes quitarte tu propio rol de administrador')).toBeInTheDocument()

    await user.click(casilla('Usuario'))
    await user.click(guardar())

    expect(await confirmacion()).toHaveTextContent('tendrás que iniciar sesión de nuevo')
  })

  it('un rechazo muestra el mensaje y deja elegir de nuevo', async () => {
    vi.spyOn(apiClient, 'put').mockRejectedValue(new ApiError('ERR_AUTH_08', 'No tenés permiso'))
    const { user, onClose } = abrir()

    await user.click(casilla('Administrador'))
    await user.click(guardar())
    await user.click(within(await confirmacion()).getByRole('button', { name: 'Cambiar roles' }))

    expect(await screen.findByText('No tenés permiso')).toBeInTheDocument()
    expect(onClose).not.toHaveBeenCalled()
    await user.click(casilla('Administrador'))
    expect(screen.queryByText('No tenés permiso')).not.toBeInTheDocument()
  })
})
