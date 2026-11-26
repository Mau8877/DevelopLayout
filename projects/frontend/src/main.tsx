import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { RouterProvider, createRouter } from '@tanstack/react-router'
import './index.css'
import { routeTree } from './routeTree.gen'
import { ErrorState } from './components/ui/error-state'
import { manejarSesionExpirada } from './features/auth'
import { QueryProvider, ensureCsrfCookie, onSesionExpirada, queryClient } from './store'
import { logger } from './utils/logger'

// * Errores de render/carga y rutas inexistentes: pantalla genérica, sin
// * detalles técnicos (el detalle va al logger, no a la UI).
const router = createRouter({
  routeTree,
  defaultErrorComponent: ({ error, reset }) => {
    logger.error('Error en la ruta', error instanceof Error ? error.name : 'desconocido')
    return <ErrorState onReintentar={reset} />
  },
  defaultNotFoundComponent: () => (
    <ErrorState titulo="Página no encontrada" descripcion="La página que buscás no existe." />
  ),
})

declare module '@tanstack/react-router' {
  interface Register {
    router: typeof router
  }
}

onSesionExpirada(() => {
  manejarSesionExpirada(queryClient, router.state.location.pathname, () => {
    void router.navigate({ to: '/login' })
  })
})

// * Errores que ningún componente atrapó: se registran por el logger (sin
// * mostrar detalles técnicos al usuario).
window.addEventListener('error', (event) => {
  logger.error('Error no controlado', event.message)
})
window.addEventListener('unhandledrejection', (event) => {
  logger.error('Promesa rechazada sin controlar', event.reason instanceof Error ? event.reason.name : 'desconocido')
})

async function bootstrap() {
  // * La cookie CSRF tiene que existir antes de la primera request mutadora.
  await ensureCsrfCookie()

  createRoot(document.getElementById('root')!).render(
    <StrictMode>
      <QueryProvider>
        <RouterProvider router={router} />
      </QueryProvider>
    </StrictMode>,
  )
}

void bootstrap()
