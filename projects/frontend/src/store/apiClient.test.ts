import { describe, it, expect, vi, beforeEach } from 'vitest'
import { AxiosError, AxiosHeaders, type AxiosResponse } from 'axios'
import { ApiError, apiClient, ensureCsrfCookie, normalizeError } from './apiClient'
import { redactBody } from './createBaseApi'

function axiosError(code: string | undefined, response?: Partial<AxiosResponse>) {
  return new AxiosError('x', code, undefined, undefined, response as AxiosResponse | undefined)
}

describe('apiClient', () => {
  it('envía credenciales y configura el header CSRF', () => {
    expect(apiClient.defaults.withCredentials).toBe(true)
    expect(apiClient.defaults.xsrfCookieName).toBe('XSRF-TOKEN')
    expect(apiClient.defaults.xsrfHeaderName).toBe('X-XSRF-TOKEN')
    expect(apiClient.defaults.withXSRFToken).toBe(true)
  })
})

describe('normalizeError', () => {
  it('convierte un fallo con sobre en ApiError con el código del backend', () => {
    const result = normalizeError(
      axiosError('ERR_BAD_REQUEST', {
        status: 401,
        data: { status: 'failed', error: 'ERR_AUTH_01', message: 'Correo o contraseña incorrectos' },
        headers: new AxiosHeaders(),
      }),
    ) as ApiError

    expect(result).toBeInstanceOf(ApiError)
    expect(result.code).toBe('ERR_AUTH_01')
    expect(result.message).toBe('Correo o contraseña incorrectos')
  })

  it('sin respuesta y sin timeout es CLIENT_NETWORK', () => {
    expect((normalizeError(axiosError('ERR_NETWORK')) as ApiError).code).toBe('CLIENT_NETWORK')
  })

  it('un timeout es CLIENT_TIMEOUT', () => {
    expect((normalizeError(axiosError('ECONNABORTED')) as ApiError).code).toBe('CLIENT_TIMEOUT')
  })

  it('una respuesta sin sobre (502, HTML) es CLIENT_UNKNOWN', () => {
    const result = normalizeError(
      axiosError('ERR_BAD_RESPONSE', { status: 502, data: '<html>Bad Gateway</html>', headers: new AxiosHeaders() }),
    ) as ApiError
    expect(result.code).toBe('CLIENT_UNKNOWN')
  })

  it('deja pasar lo que no es un error de axios', () => {
    const err = new Error('otra cosa')
    expect(normalizeError(err)).toBe(err)
  })
})

describe('redactBody', () => {
  it('oculta el body de /auth/**', () => {
    expect(redactBody('http://api/api/v1/auth/login', { password: 'x' })).toBe('[REDACTED]')
  })

  it('deja pasar el resto', () => {
    expect(redactBody('http://api/api/v1/usuarios', { a: 1 })).toEqual({ a: 1 })
  })
})

describe('ensureCsrfCookie', () => {
  beforeEach(() => vi.restoreAllMocks())

  it('hace GET /auth/csrf una sola vez', async () => {
    const get = vi.spyOn(apiClient, 'get').mockResolvedValue({ data: undefined } as AxiosResponse)

    await ensureCsrfCookie()
    await ensureCsrfCookie()

    expect(get).toHaveBeenCalledTimes(1)
    expect(get).toHaveBeenCalledWith('/auth/csrf')
  })
})
