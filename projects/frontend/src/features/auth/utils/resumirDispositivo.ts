import { i18n } from '@/shared/language'

// * El orden importa: Edge y Opera también dicen "Chrome", y Chrome dice "Safari".
const NAVEGADORES: [RegExp, string][] = [
  [/Edg(e|A|iOS)?\//, 'Edge'],
  [/OPR\/|Opera/, 'Opera'],
  [/Firefox\/|FxiOS\//, 'Firefox'],
  [/Chrome\/|CriOS\//, 'Chrome'],
  [/Version\/[\d.]+.*Safari\//, 'Safari'],
]

// * El de iPhone dice "like Mac OS X" y el de Android dice "Linux".
const SISTEMAS: [RegExp, string][] = [
  [/iPhone/, 'iPhone'],
  [/iPad/, 'iPad'],
  [/Android/, 'Android'],
  [/Windows/, 'Windows'],
  [/CrOS/, 'ChromeOS'],
  [/Mac OS X|Macintosh/, 'macOS'],
  [/Linux|X11/, 'Linux'],
]

const buscar = (reglas: [RegExp, string][], texto: string) => reglas.find(([patron]) => patron.test(texto))?.[1]

/**
 * Nombre corto y orientativo del dispositivo de una sesión. El user agent lo
 * controla el cliente: puede ser falso, y lo que no se reconoce no se adivina.
 */
export function resumirDispositivo(userAgent: string | null | undefined, tipoCliente: 'WEB' | 'MOBILE'): string {
  if (tipoCliente === 'MOBILE') return i18n.t('auth:dispositivo.appMovil')
  const texto = userAgent ?? ''
  const navegador = buscar(NAVEGADORES, texto)
  const sistema = buscar(SISTEMAS, texto)
  if (navegador && sistema) return i18n.t('auth:dispositivo.navegadorEn', { navegador, sistema })
  return navegador ?? sistema ?? i18n.t('auth:dispositivo.desconocido')
}
