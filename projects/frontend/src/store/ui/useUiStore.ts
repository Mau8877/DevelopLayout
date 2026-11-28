import { create } from 'zustand'
import { persist } from 'zustand/middleware'

export type Theme = 'light' | 'dark'

interface UiState {
  sidebarCollapsed: boolean
  toggleSidebarCollapsed: () => void
  mobileSidebarOpen: boolean
  setMobileSidebarOpen: (open: boolean) => void
  theme: Theme
  toggleTheme: () => void
}

export const useUiStore = create<UiState>()(
  persist(
    (set) => ({
      sidebarCollapsed: false,
      toggleSidebarCollapsed: () =>
        set((s) => ({ sidebarCollapsed: !s.sidebarCollapsed })),
      mobileSidebarOpen: false,
      setMobileSidebarOpen: (open) => set({ mobileSidebarOpen: open }),
      theme: 'light',
      toggleTheme: () =>
        set((s) => ({ theme: s.theme === 'light' ? 'dark' : 'light' })),
    }),
    {
      name: 'ui-storage',
      // * Solo el tema se persiste entre sesiones -- sidebarCollapsed y
      // * mobileSidebarOpen son estado transitorio de la sesión actual.
      partialize: (s) => ({ theme: s.theme }),
    }
  )
)

// * El tema se aplica con la clase `.dark` de <html> (ver index.css). Es síncrono a
// * propósito: la animación de runThemeTransition necesita el cambio en el mismo tick.
function aplicarTema(theme: Theme) {
  document.documentElement.classList.toggle('dark', theme === 'dark')
}

aplicarTema(useUiStore.getState().theme)
useUiStore.subscribe((state) => aplicarTema(state.theme))
