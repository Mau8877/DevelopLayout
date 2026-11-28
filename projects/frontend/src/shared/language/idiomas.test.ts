import { afterEach, describe, expect, it, vi } from 'vitest'
import { i18n } from './i18n'
import { detectarIdioma, guardarPreferencia, leerPreferencia } from './idiomas'
import { formatoActual, traducirMensaje } from './traducir'

afterEach(() => {
  vi.restoreAllMocks()
})

describe('detectarIdioma', () => {
  it('prefiere la elección guardada sobre el idioma del navegador', () => {
    expect(detectarIdioma('es', ['en-US'])).toBe('es')
  })

  it('sin preferencia usa el primer idioma soportado del navegador', () => {
    expect(detectarIdioma(undefined, ['fr-FR', 'en-GB', 'es'])).toBe('en')
    expect(detectarIdioma(undefined, ['es-BO'])).toBe('es')
  })

  it('si ninguno está soportado, queda en español', () => {
    expect(detectarIdioma(undefined, ['fr-FR', 'pt-BR'])).toBe('es')
    expect(detectarIdioma(undefined, [])).toBe('es')
  })

  it('ignora una preferencia guardada que no es un idioma soportado', () => {
    window.localStorage.setItem('idioma', '<script>')

    expect(leerPreferencia()).toBeUndefined()
  })

  it('sin almacenamiento disponible no falla', () => {
    vi.spyOn(Storage.prototype, 'getItem').mockImplementation(() => {
      throw new Error('bloqueado')
    })
    vi.spyOn(Storage.prototype, 'setItem').mockImplementation(() => {
      throw new Error('bloqueado')
    })

    expect(() => guardarPreferencia('en')).not.toThrow()
    expect(leerPreferencia()).toBeUndefined()
  })
})

describe('textos según el idioma', () => {
  it('traduce una clave y deja igual un texto del servidor', () => {
    expect(traducirMensaje('shared:comun.reintentar')).toBe('Reintentar')
    expect(traducirMensaje('El usuario ya existe')).toBe('El usuario ya existe')
    expect(traducirMensaje(undefined)).toBeUndefined()
  })

  it('al cambiar de idioma cambian los textos, el formato y el lang del documento', async () => {
    await i18n.changeLanguage('en')

    expect(traducirMensaje('shared:comun.reintentar')).toBe('Retry')
    expect(formatoActual()).toBe('en-US')
    expect(document.documentElement.lang).toBe('en')
  })

  it('usa el plural que corresponde', () => {
    expect(i18n.t('shared:tabla.resultados', { count: 1 })).toBe('1 resultado')
    expect(i18n.t('shared:tabla.resultados', { count: 2 })).toBe('2 resultados')
  })
})
