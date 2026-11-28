import { describe, expect, it } from 'vitest'
import { traducirMensaje } from '@/shared/language'
import { cambiarPasswordSchema } from '../schemas'

const errores = (passwordActual: string, passwordNueva: string, confirmacion = passwordNueva) => {
  const r = cambiarPasswordSchema.safeParse({ passwordActual, passwordNueva, confirmacion })
  return r.success ? [] : r.error.issues.map((i) => `${i.path.join('.')}: ${traducirMensaje(i.message)}`)
}

describe('cambiarPasswordSchema', () => {
  it('acepta una actual y una nueva válidas', () => {
    expect(errores('ClaveActual123', 'ClaveNueva456')).toEqual([])
  })

  it('exige la contraseña actual', () => {
    expect(errores('', 'ClaveNueva456')).toContain('passwordActual: La contraseña actual es obligatoria')
  })

  it('aplica a la nueva la política de 8 caracteres y 72 bytes', () => {
    expect(errores('ClaveActual123', 'corta')).toContain(
      'passwordNueva: La contraseña debe tener al menos 8 caracteres',
    )
    expect(errores('ClaveActual123', 'ñ'.repeat(40))).toContain('passwordNueva: La contraseña es demasiado larga')
    expect(errores('ñ'.repeat(40), 'ClaveNueva456')).toContain('passwordActual: La contraseña es demasiado larga')
  })

  it('marca la confirmación cuando no coincide', () => {
    expect(errores('ClaveActual123', 'ClaveNueva456', 'ClaveNueva457')).toEqual([
      'confirmacion: Las contraseñas no coinciden',
    ])
  })
})
