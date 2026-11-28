import { describe, expect, it } from 'vitest'
import { traducirMensaje } from '@/shared/language'
import { forgotPasswordSchema } from '../schemas'
import { resetPasswordSchema } from '../schemas'

const errores = (password: string, confirmacion = password) => {
  const r = resetPasswordSchema.safeParse({ password, confirmacion })
  return r.success ? [] : r.error.issues.map((i) => `${i.path.join('.')}: ${traducirMensaje(i.message)}`)
}

describe('resetPasswordSchema', () => {
  it('acepta 8 caracteres sin otras reglas', () => {
    expect(errores('abcdefgh')).toEqual([])
  })

  it('rechaza menos de 8 caracteres', () => {
    expect(errores('abcdefg')).toContain('password: La contraseña debe tener al menos 8 caracteres')
  })

  it('mide el máximo en bytes: 40 letras de dos bytes no entran en 72', () => {
    expect(errores('ñ'.repeat(40))).toContain('password: La contraseña es demasiado larga')
    expect(errores('ñ'.repeat(36))).toEqual([])
  })

  it('marca la confirmación cuando no coincide', () => {
    expect(errores('abcdefgh', 'abcdefgX')).toEqual(['confirmacion: Las contraseñas no coinciden'])
  })

  it('el pedido del enlace exige un correo válido', () => {
    expect(forgotPasswordSchema.safeParse({ email: 'no-es' }).success).toBe(false)
    expect(forgotPasswordSchema.safeParse({ email: 'a@b.co' }).success).toBe(true)
  })
})
