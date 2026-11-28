import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { useTranslation } from 'react-i18next'
import { describe, expect, it, vi } from 'vitest'
import { SelectorIdioma } from './SelectorIdioma'

function Ejemplo() {
  const { t } = useTranslation('shared')
  return <p>{t('comun.reintentar')}</p>
}

function montar() {
  const queryClient = new QueryClient()
  const invalidar = vi.spyOn(queryClient, 'invalidateQueries')
  render(
    <QueryClientProvider client={queryClient}>
      <SelectorIdioma />
      <Ejemplo />
    </QueryClientProvider>,
  )
  return { invalidar, user: userEvent.setup() }
}

describe('SelectorIdioma', () => {
  it('cambia los textos sin recargar, recuerda la elección y vuelve a pedir los datos', async () => {
    const { invalidar, user } = montar()

    await user.click(screen.getByRole('button', { name: 'Cambiar idioma: Español' }))
    await user.click(await screen.findByRole('menuitem', { name: 'English' }))

    expect(await screen.findByText('Retry')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Change language: English' })).toBeInTheDocument()
    expect(window.localStorage.getItem('idioma')).toBe('en')
    expect(document.documentElement.lang).toBe('en')
    await waitFor(() => expect(invalidar).toHaveBeenCalled())
  })

  it('elegir el idioma actual no vuelve a pedir nada', async () => {
    const { invalidar, user } = montar()

    await user.click(screen.getByRole('button', { name: 'Cambiar idioma: Español' }))
    await user.click(await screen.findByRole('menuitem', { name: 'Español' }))

    expect(invalidar).not.toHaveBeenCalled()
  })
})
