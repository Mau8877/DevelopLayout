import { beforeEach, describe, expect, it, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { apiClient, ApiError } from '@/store'
import { ForgotPasswordScreen } from '../screens/ForgotPasswordScreen'

vi.mock('@tanstack/react-router', () => ({
  Link: ({ to, children }: { to: string; children: React.ReactNode }) => <a href={to}>{children}</a>,
}))

function renderScreen() {
  render(
    <QueryClientProvider client={new QueryClient()}>
      <ForgotPasswordScreen />
    </QueryClientProvider>,
  )
}

async function enviar(correo: string) {
  const user = userEvent.setup()
  await user.type(screen.getByLabelText('Correo'), correo)
  await user.click(screen.getByRole('button', { name: 'Enviar enlace' }))
}

describe('ForgotPasswordScreen', () => {
  beforeEach(() => vi.restoreAllMocks())

  it('envía el correo y muestra un mensaje que no afirma que la cuenta exista', async () => {
    const post = vi.spyOn(apiClient, 'post').mockResolvedValue({ data: undefined })
    renderScreen()

    await enviar('a@b.co')

    expect(await screen.findByRole('status')).toHaveTextContent('Si hay una cuenta con ese correo')
    expect(post).toHaveBeenCalledWith('/auth/password/forgot', { email: 'a@b.co' })
    expect(screen.queryByRole('button', { name: 'Enviar enlace' })).not.toBeInTheDocument()
  })

  it('con 429 muestra el aviso del backend y deja reintentar', async () => {
    vi.spyOn(apiClient, 'post').mockRejectedValue(new ApiError('ERR_SYS_02', 'Demasiadas solicitudes', 429))
    renderScreen()

    await enviar('a@b.co')

    expect(await screen.findByRole('alert')).toHaveTextContent('Demasiadas solicitudes')
    expect(screen.getByRole('button', { name: 'Enviar enlace' })).toBeEnabled()
  })

  it('no llama a la API con un correo inválido', async () => {
    const post = vi.spyOn(apiClient, 'post')
    renderScreen()

    await enviar('no-es-un-correo')

    expect(await screen.findByText('El correo no es válido')).toBeInTheDocument()
    expect(post).not.toHaveBeenCalled()
  })
})
