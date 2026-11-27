import { describe, it, expect } from 'vitest'
import { Route } from '@/routes/_authenticated/_admin'

type Guard = (args: { context: { usuario: { roles: string[] } } }) => unknown

const guard = Route.options.beforeLoad as unknown as Guard
const con = (roles: string[]) => () => guard({ context: { usuario: { roles } } })

describe('guard de rutas de administrador', () => {
  it('deja pasar a SUPER_ADMIN', () => {
    expect(con(['SUPER_ADMIN'])).not.toThrow()
  })

  it('redirige a /panel a quien no lo es', () => {
    let lanzado: unknown
    try {
      con(['USUARIO'])()
    } catch (error) {
      lanzado = error
    }
    expect(lanzado).toMatchObject({ options: { to: '/panel' } })
  })
})
