import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { apiClient, ApiError } from '@/store'
import { LoginScreen } from '../screens/LoginScreen'

const navigate = vi.fn()
vi.mock('@tanstack/react-router', () => ({
  useNavigate: () => navigate,
  Link: ({ to, children }: { to: string; children: React.ReactNode }) => <a href={to}>{children}</a>,
}))

function renderScreen() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  render(
    <QueryClientProvider client={queryClient}>
      <LoginScreen />
    </QueryClientProvider>,
  )
}

describe('LoginScreen', () => {
  beforeEach(() => {
    vi.restoreAllMocks()
    navigate.mockReset()
  })

  it('muestra errores inline en español al salir de campos vacíos', async () => {
    const user = userEvent.setup()
    renderScreen()

    await user.click(screen.getByLabelText('Correo'))
    await user.click(screen.getByLabelText('Contraseña'))
    await user.click(screen.getByLabelText('Correo'))

    expect(await screen.findByText('El correo es obligatorio')).toBeInTheDocument()
  })

  it('con datos válidos llama a /auth/login y navega al panel', async () => {
    const post = vi
      .spyOn(apiClient, 'post')
      .mockResolvedValue({ data: { usuario: { codigo: 'u', email: 'a@b.co', roles: ['USUARIO'] } } })
    const user = userEvent.setup()
    renderScreen()

    await user.type(screen.getByLabelText('Correo'), 'a@b.co')
    await user.type(screen.getByLabelText('Contraseña'), 'ClaveSegura123')
    await user.click(screen.getByRole('button', { name: 'Ingresar' }))

    await waitFor(() => expect(navigate).toHaveBeenCalledWith({ to: '/panel' }))
    expect(post).toHaveBeenCalledWith('/auth/login', { email: 'a@b.co', password: 'ClaveSegura123' })
  })

  it('muestra el error genérico del backend si las credenciales son malas', async () => {
    vi.spyOn(apiClient, 'post').mockRejectedValue(new ApiError('ERR_AUTH_01', 'Correo o contraseña incorrectos'))
    const user = userEvent.setup()
    renderScreen()

    await user.type(screen.getByLabelText('Correo'), 'a@b.co')
    await user.type(screen.getByLabelText('Contraseña'), 'ClaveSegura123')
    await user.click(screen.getByRole('button', { name: 'Ingresar' }))

    expect(await screen.findByText('Correo o contraseña incorrectos')).toBeInTheDocument()
    expect(navigate).not.toHaveBeenCalled()
  })

  // * El caso de todos los días: correo, Tab, contraseña, Enter, sin salir del último campo.
  it('envía con Enter sin haber salido del campo de contraseña', async () => {
    const post = vi
      .spyOn(apiClient, 'post')
      .mockResolvedValue({ data: { usuario: { codigo: 'u', email: 'a@b.co', roles: ['USUARIO'] } } })
    const user = userEvent.setup()
    renderScreen()

    await user.type(screen.getByLabelText('Correo'), 'a@b.co')
    await user.tab()
    await user.keyboard('ClaveSegura123{Enter}')

    await waitFor(() => expect(post).toHaveBeenCalledWith('/auth/login', { email: 'a@b.co', password: 'ClaveSegura123' }))
  })

  it('enlaza a la recuperación de contraseña', () => {
    renderScreen()

    expect(screen.getByRole('link', { name: '¿Olvidaste tu contraseña?' })).toHaveAttribute(
      'href',
      '/forgot-password',
    )
  })

  it('usa los autocomplete correctos y oculta la contraseña', () => {
    renderScreen()

    expect(screen.getByLabelText('Correo')).toHaveAttribute('autocomplete', 'username')
    expect(screen.getByLabelText('Contraseña')).toHaveAttribute('autocomplete', 'current-password')
    expect(screen.getByLabelText('Contraseña')).toHaveAttribute('type', 'password')
  })
})
