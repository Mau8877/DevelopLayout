import { describe, expect, it } from 'vitest'
import { resumirDispositivo } from './resumirDispositivo'

const WIN = 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko)'

describe('resumirDispositivo', () => {
  it.each([
    [`${WIN} Chrome/140.0 Safari/537.36`, 'Chrome en Windows'],
    [`${WIN} Chrome/140.0 Safari/537.36 Edg/140.0`, 'Edge en Windows'],
    [`${WIN} Chrome/140.0 Safari/537.36 OPR/120.0`, 'Opera en Windows'],
    ['Mozilla/5.0 (X11; Linux x86_64; rv:140.0) Gecko/20100101 Firefox/140.0', 'Firefox en Linux'],
    ['Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 Version/18.0 Safari/605.1.15', 'Safari en macOS'],
    ['Mozilla/5.0 (iPhone; CPU iPhone OS 18_0 like Mac OS X) AppleWebKit/605.1.15 Version/18.0 Mobile/15E148 Safari/604.1', 'Safari en iPhone'],
    ['Mozilla/5.0 (Linux; Android 15) AppleWebKit/537.36 Chrome/140.0 Mobile Safari/537.36', 'Chrome en Android'],
  ])('reconoce %s', (userAgent, esperado) => {
    expect(resumirDispositivo(userAgent, 'WEB')).toBe(esperado)
  })

  it('una sesión mobile es la app, diga lo que diga su user agent', () => {
    expect(resumirDispositivo('Dart/3.12 (dart:io)', 'MOBILE')).toBe('App móvil')
  })

  it('no adivina lo que no reconoce', () => {
    expect(resumirDispositivo('curl/8.0', 'WEB')).toBe('Dispositivo desconocido')
    expect(resumirDispositivo(undefined, 'WEB')).toBe('Dispositivo desconocido')
    expect(resumirDispositivo('algo (Windows NT 10.0)', 'WEB')).toBe('Windows')
  })
})
