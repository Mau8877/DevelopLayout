import { describe, it, expect, vi } from 'vitest'

const tieneDark = () => document.documentElement.classList.contains('dark')

describe('useUiStore: tema', () => {
  it('cambiar el tema pone y quita la clase .dark en el documento', async () => {
    const { useUiStore } = await import('./useUiStore')
    useUiStore.setState({ theme: 'light' })

    useUiStore.getState().toggleTheme()
    expect(tieneDark()).toBe(true)

    useUiStore.getState().toggleTheme()
    expect(tieneDark()).toBe(false)
  })

  it('al cargar aplica el tema guardado', async () => {
    localStorage.setItem('ui-storage', JSON.stringify({ state: { theme: 'dark' }, version: 0 }))
    document.documentElement.classList.remove('dark')
    vi.resetModules()

    await import('./useUiStore')

    expect(tieneDark()).toBe(true)
    localStorage.clear()
    document.documentElement.classList.remove('dark')
  })
})
