import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { AxiosError, type AxiosAdapter, type InternalAxiosRequestConfig } from 'axios'
import { ApiError, apiClient, onSesionExpirada } from './apiClient'

function respuesta(config: InternalAxiosRequestConfig, status: number, data: unknown) {
  const response = { config, status, data, statusText: '', headers: {} }
  if (status < 400) return Promise.resolve(response)
  return Promise.reject(new AxiosError('fallo', 'ERR_BAD_REQUEST', config, undefined, response))
}

const ok = (data: unknown) => ({ status: 'success', data, message: 'ok', timestamp: '' })
const fallo = (error: string) => ({ status: 'failed', data: null, message: 'x', timestamp: '', error })

/** Backend falso: las rutas de negocio dan 401 hasta que haya un refresh exitoso. */
function backend(opciones: { refresh?: number | 'sin-red'; siempre401?: boolean } = {}) {
  const llamadas: string[] = []
  let renovada = false
  const adapter: AxiosAdapter = async (config) => {
    const url = config.url ?? ''
    llamadas.push(url)
    if (url === '/auth/csrf') return respuesta(config, 204, '')
    if (url === '/auth/refresh') {
      await new Promise((resolve) => setTimeout(resolve, 5))
      if (opciones.refresh === 'sin-red') throw new AxiosError('sin red', 'ERR_NETWORK', config)
      if (opciones.refresh) return respuesta(config, opciones.refresh, fallo('ERR_X'))
      renovada = true
      return respuesta(config, 200, ok({}))
    }
    if (url === '/auth/login') return respuesta(config, 401, fallo('ERR_AUTH_01'))
    if (!renovada || opciones.siempre401) return respuesta(config, 401, fallo('ERR_AUTH_07'))
    return respuesta(config, 200, ok({ url }))
  }
  apiClient.defaults.adapter = adapter
  return { veces: (url: string) => llamadas.filter((u) => u === url).length }
}

describe('apiClient: renovación de sesión', () => {
  const original = apiClient.defaults.adapter
  const expirada = vi.fn()

  beforeEach(() => {
    expirada.mockReset()
    onSesionExpirada(expirada)
  })

  afterEach(() => {
    apiClient.defaults.adapter = original
    vi.unstubAllGlobals()
  })

  it('con tres 401 en paralelo hace un solo refresh y reintenta las tres', async () => {
    const api = backend()

    const respuestas = await Promise.all(['/a', '/b', '/c'].map((url) => apiClient.get(url)))

    expect(respuestas.map((r) => r.data)).toEqual([{ url: '/a' }, { url: '/b' }, { url: '/c' }])
    expect(api.veces('/auth/refresh')).toBe(1)
    expect(api.veces('/a')).toBe(2)
    expect(expirada).not.toHaveBeenCalled()
  })

  it('reintenta una sola vez: si vuelve a dar 401 no entra en bucle', async () => {
    const api = backend({ siempre401: true })

    await expect(apiClient.get('/a')).rejects.toMatchObject({ code: 'ERR_AUTH_07' })

    expect(api.veces('/a')).toBe(2)
    expect(api.veces('/auth/refresh')).toBe(1)
  })

  it.each([401, 403])('con %i en el refresh avisa que la sesión expiró', async (status) => {
    const api = backend({ refresh: status })

    await expect(apiClient.get('/a')).rejects.toMatchObject({ code: 'ERR_AUTH_07' })

    expect(expirada).toHaveBeenCalledTimes(1)
    expect(api.veces('/a')).toBe(1)
  })

  it.each([429, 500, 'sin-red' as const])('con %s en el refresh conserva la sesión', async (refresh) => {
    backend({ refresh })

    const error = await apiClient.get('/a').catch((e: unknown) => e)

    expect(error).toBeInstanceOf(ApiError)
    expect((error as ApiError).code).not.toBe('ERR_AUTH_07')
    expect(expirada).not.toHaveBeenCalled()
  })

  it('un 401 del login no intenta renovar', async () => {
    const api = backend()

    await expect(apiClient.post('/auth/login')).rejects.toMatchObject({ code: 'ERR_AUTH_01' })

    expect(api.veces('/auth/refresh')).toBe(0)
  })

  it('el refresh se ejecuta bajo el candado compartido entre pestañas', async () => {
    const request = vi.fn((_nombre: string, tarea: () => Promise<unknown>) => tarea())
    vi.stubGlobal('navigator', { locks: { request } })
    backend()

    await apiClient.get('/a')

    expect(request).toHaveBeenCalledWith('auth-session', expect.any(Function))
  })
})
