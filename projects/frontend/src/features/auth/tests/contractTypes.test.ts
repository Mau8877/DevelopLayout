import { describe, expect, it } from 'vitest'
import type { components } from '@/shared/api/schema'
import type { ForgotPasswordRequest } from '../schemas/forgotPasswordSchema'
import type { LoginRequest } from '../schemas/loginSchema'

// * La verificación real es de tipos (la corre `tsc -b` en `pnpm build`): si el backend
// * cambia LoginRequest y el schema de Zod del formulario no lo sigue, el build falla.
type LoginRequestContrato = components['schemas']['LoginRequest']
const _formularioCumpleElContrato = (valor: LoginRequest): LoginRequestContrato => valor

type ForgotContrato = components['schemas']['ForgotPasswordRequest']
const _olvideCumpleElContrato = (valor: ForgotPasswordRequest): ForgotContrato => valor

describe('contrato OpenAPI', () => {
  it('el formulario de recuperación produce el ForgotPasswordRequest del backend', () => {
    expect(_olvideCumpleElContrato({ email: 'a@b.c' })).toEqual({ email: 'a@b.c' })
  })

  it('el formulario de login produce el LoginRequest del backend', () => {
    expect(_formularioCumpleElContrato({ email: 'a@b.c', password: '12345678' })).toEqual({
      email: 'a@b.c',
      password: '12345678',
    })
  })
})
