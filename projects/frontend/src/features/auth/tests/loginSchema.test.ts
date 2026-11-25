import { describe, it, expect } from 'vitest'
import { loginSchema } from '../schemas/loginSchema'

describe('loginSchema', () => {
  it('acepta credenciales válidas', () => {
    expect(loginSchema.safeParse({ email: 'a@b.co', password: 'ClaveSegura123' }).success).toBe(true)
  })

  it('rechaza campos vacíos con mensajes en español', () => {
    const result = loginSchema.safeParse({ email: '', password: '' })
    expect(result.success).toBe(false)
    const mensajes = result.error?.issues.map((i) => i.message) ?? []
    expect(mensajes).toContain('El correo es obligatorio')
    expect(mensajes).toContain('La contraseña es obligatoria')
  })

  it('rechaza un correo inválido', () => {
    const result = loginSchema.safeParse({ email: 'no-es-correo', password: 'ClaveSegura123' })
    expect(result.error?.issues[0].message).toBe('El correo no es válido')
  })

  it('rechaza una contraseña de menos de 8 caracteres', () => {
    const result = loginSchema.safeParse({ email: 'a@b.co', password: 'corta' })
    expect(result.error?.issues[0].message).toBe('La contraseña debe tener al menos 8 caracteres')
  })

  it('rechaza una contraseña de más de 72 bytes (los acentos cuentan doble)', () => {
    expect(loginSchema.safeParse({ email: 'a@b.co', password: 'é'.repeat(37) }).success).toBe(false)
    expect(loginSchema.safeParse({ email: 'a@b.co', password: 'a'.repeat(72) }).success).toBe(true)
  })
})
