import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { CLAVE_CANDADO, VIGENCIA_CANDADO_MS, conCandadoDeSesion } from './candadoSesion'

/** Tarea que anota cuándo empieza y termina. */
function tarea(nombre: string, pasos: string[]) {
  return async () => {
    pasos.push(`${nombre} empieza`)
    await new Promise((resolve) => setTimeout(resolve, 100))
    pasos.push(`${nombre} termina`)
    return nombre
  }
}

beforeEach(() => localStorage.clear())
afterEach(() => {
  vi.useRealTimers()
  vi.restoreAllMocks()
  vi.unstubAllGlobals()
})

describe('conCandadoDeSesion con Web Locks', () => {
  it('delega en el candado del navegador', async () => {
    const request = vi.fn((_nombre: string, t: () => Promise<unknown>) => t())
    vi.stubGlobal('navigator', { locks: { request } })

    await expect(conCandadoDeSesion(async () => 7)).resolves.toBe(7)

    expect(request).toHaveBeenCalledWith('auth-session', expect.any(Function))
  })
})

describe('conCandadoDeSesion sin Web Locks', () => {
  beforeEach(() => {
    vi.stubGlobal('navigator', {})
    vi.useFakeTimers()
  })

  it('las tareas simultáneas se ejecutan de a una', async () => {
    const pasos: string[] = []

    const todas = Promise.all(['a', 'b', 'c'].map((n) => conCandadoDeSesion(tarea(n, pasos))))
    await vi.advanceTimersByTimeAsync(2_000)
    await todas

    expect(pasos).toHaveLength(6)
    for (let i = 0; i < pasos.length; i += 2) {
      expect(pasos[i + 1]).toBe(pasos[i].replace('empieza', 'termina'))
    }
    expect(localStorage.getItem(CLAVE_CANDADO)).toBeNull()
  })

  it('espera mientras otra pestaña tiene la reserva vigente y la toma al vencer', async () => {
    localStorage.setItem(CLAVE_CANDADO, JSON.stringify({ id: 'otra', hasta: Date.now() + VIGENCIA_CANDADO_MS }))
    const hecha = vi.fn(async () => 'ok')

    const resultado = conCandadoDeSesion(hecha)
    await vi.advanceTimersByTimeAsync(VIGENCIA_CANDADO_MS - 1_000)
    expect(hecha).not.toHaveBeenCalled()

    await vi.advanceTimersByTimeAsync(2_000)
    await expect(resultado).resolves.toBe('ok')
  })

  it('libera la reserva aunque la tarea falle', async () => {
    const resultado = conCandadoDeSesion(async () => {
      throw new Error('fallo')
    })
    const comprobacion = expect(resultado).rejects.toThrow('fallo')
    await vi.advanceTimersByTimeAsync(200)
    await comprobacion

    expect(localStorage.getItem(CLAVE_CANDADO)).toBeNull()
  })

  it('un valor corrupto no bloquea', async () => {
    localStorage.setItem(CLAVE_CANDADO, 'no es json')

    const resultado = conCandadoDeSesion(async () => 'ok')
    await vi.advanceTimersByTimeAsync(200)

    await expect(resultado).resolves.toBe('ok')
  })

  it('si localStorage no está disponible ejecuta la tarea igual', async () => {
    vi.stubGlobal('localStorage', {
      getItem: () => {
        throw new Error('bloqueado')
      },
    })

    await expect(conCandadoDeSesion(async () => 'ok')).resolves.toBe('ok')
  })
})
