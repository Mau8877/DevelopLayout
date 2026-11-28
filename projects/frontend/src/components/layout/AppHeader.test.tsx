import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { createRootRoute, createRouter, createMemoryHistory, RouterProvider } from '@tanstack/react-router'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import { AppHeader } from './AppHeader'
import type { NotificationsController } from './types'

function montar(notifications?: NotificationsController) {
  const raiz = createRootRoute({
    component: () => (
      <AppHeader appName="App" user={{ nombre: 'Ana', rol: 'Rol' }} notifications={notifications} />
    ),
  })
  const router = createRouter({ routeTree: raiz, history: createMemoryHistory({ initialEntries: ['/'] }) })
  render(
    <QueryClientProvider client={new QueryClient()}>
      <RouterProvider router={router as never} />
    </QueryClientProvider>,
  )
  return userEvent.setup()
}

const fuente: NotificationsController = {
  items: [],
  meta: undefined,
  isLoading: false,
  isLoadingMore: false,
  unreadCount: 0,
  onOpenChange: vi.fn(),
  onLoadMore: vi.fn(),
  onMarkAsRead: vi.fn(),
  onMarkAllAsRead: vi.fn(),
  onDeleteAll: vi.fn(),
}

describe('AppHeader', () => {
  it('el menú de usuario solo ofrece opciones que hacen algo', async () => {
    const user = montar()

    await user.click(await screen.findByRole('button', { name: /Ana/ }))

    const opciones = (await screen.findAllByRole('menuitem')).map((opcion) => opcion.textContent)
    expect(opciones).toEqual(['Cambiar contraseña', 'Cambiar tema', 'Cerrar sesión'])
  })

  it('sin una fuente de notificaciones no muestra la campana', async () => {
    montar()

    await screen.findByRole('button', { name: /Ana/ })

    expect(screen.queryByRole('button', { name: /Notificaciones/ })).not.toBeInTheDocument()
  })

  it('con una fuente de notificaciones muestra la campana', async () => {
    montar(fuente)

    expect(await screen.findByRole('button', { name: /Notificaciones/ })).toBeInTheDocument()
  })
})
