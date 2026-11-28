import { screen, waitFor } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { toastSuccess, toastWarning } from '@/components/ui/toast'
import { apiClient, ApiError } from '@/store'
import { CrearUsuarioModal } from '../components'
import { montar, usuario } from './soporte'

vi.mock('@/components/ui/toast', () => ({ toastSuccess: vi.fn(), toastWarning: vi.fn() }))

const respuesta = (invitacionEnviada: boolean) => ({ data: { usuario: usuario(), invitacionEnviada } })

function abrir() {
  const onClose = vi.fn()
  return { onClose, ...montar(<CrearUsuarioModal onClose={onClose} />) }
}

async function completar(user: ReturnType<typeof abrir>['user'], correo = 'nueva@test.local') {
  await user.click(screen.getByRole('checkbox', { name: 'Usuario' }))
  await user.type(screen.getByLabelText('Correo'), `${correo}{Enter}`)
}

describe('CrearUsuarioModal', () => {
  beforeEach(() => vi.clearAllMocks())

  it('envía con Enter solo el correo y los roles, avisa y cierra', async () => {
    const post = vi.spyOn(apiClient, 'post').mockResolvedValue(respuesta(true))
    const { user, onClose } = abrir()

    await completar(user)

    await waitFor(() => expect(onClose).toHaveBeenCalled())
    expect(post).toHaveBeenCalledTimes(1)
    expect(post).toHaveBeenCalledWith('/users', { email: 'nueva@test.local', roles: ['USUARIO'] })
    expect(toastSuccess).toHaveBeenCalled()
  })

  it('si el correo no salió, avisa que la cuenta se creó', async () => {
    vi.spyOn(apiClient, 'post').mockResolvedValue(respuesta(false))
    const { user, onClose } = abrir()

    await completar(user)

    await waitFor(() => expect(onClose).toHaveBeenCalled())
    expect(toastWarning).toHaveBeenCalledWith('La cuenta se creó, pero no se pudo enviar el correo', expect.any(String))
    expect(toastSuccess).not.toHaveBeenCalled()
  })

  it('con datos inválidos muestra el error junto al campo y no envía', async () => {
    const post = vi.spyOn(apiClient, 'post')
    const { user } = abrir()

    await user.click(screen.getByRole('button', { name: 'Crear' }))
    expect(await screen.findByText('El correo es obligatorio')).toBeInTheDocument()
    expect(screen.getByText('Elige al menos un rol')).toBeInTheDocument()

    await user.type(screen.getByLabelText('Correo'), 'no-es-correo{Enter}')

    expect(await screen.findByText('El correo no es válido')).toBeInTheDocument()
    expect(post).not.toHaveBeenCalled()
  })

  it('ante un rechazo muestra el mensaje del servidor y no cierra', async () => {
    vi.spyOn(apiClient, 'post').mockRejectedValue(new ApiError('ERR_US_02', 'Ya existe una cuenta con ese correo'))
    const { user, onClose } = abrir()

    await completar(user)

    expect(await screen.findByText('Ya existe una cuenta con ese correo')).toBeInTheDocument()
    expect(onClose).not.toHaveBeenCalled()
  })
})
