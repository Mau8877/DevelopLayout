import { describe, expect, it } from 'vitest'
import { i18n } from './i18n'

/**
 * Lo que ESLint (`no-literal-string`, solo JSX) no ve: textos en `.ts`, avisos y
 * mensajes de Zod.
 */
const archivos = import.meta.glob<string>('/src/**/*.{ts,tsx}', { query: '?raw', import: 'default', eager: true })

const codigo = Object.entries(archivos)
  .filter(([ruta]) => !ruta.includes('.test.') && !ruta.includes('/tests/'))
  .filter(([ruta]) => !['/src/routeTree.gen.ts', '/src/shared/api/schema.d.ts'].includes(ruta))
  .map(([ruta, texto]) => [ruta, sinLogs(sinComentarios(texto))] as const)

function sinComentarios(texto: string): string {
  return texto.replace(/\/\*[\s\S]*?\*\//g, '').replace(/(^|\s)\/\/.*$/gm, '$1')
}

// * Los logs no se traducen (IDIOMAS.md).
function sinLogs(texto: string): string {
  return texto.replace(/logger\.\w+\(\s*(`[^`]*`|'[^'\n]*'|"[^"\n]*")/g, 'logger(')
}

const literales = (texto: string) =>
  [...texto.matchAll(/'([^'\n]*)'|"([^"\n]*)"|`([^`]*)`/g)].map((m) => m[1] ?? m[2] ?? m[3])

// * El nombre de un idioma va escrito en su propio idioma.
const PERMITIDOS = new Set(['Español'])

describe('sin texto fijo en el código', () => {
  it('revisa de verdad el código', () => {
    expect(codigo.length).toBeGreaterThan(60)
  })

  it('ningún literal está escrito en español', () => {
    const fijos = codigo.flatMap(([ruta, texto]) =>
      literales(texto)
        .filter((valor) => /[áéíóúñ¿¡]/i.test(valor) && !PERMITIDOS.has(valor))
        .map((valor) => `${ruta}: ${valor}`),
    )

    expect(fijos).toEqual([])
  })

  it('los avisos reciben textos traducidos, no literales', () => {
    const fijos = codigo.filter(([, texto]) => /\btoast(Success|Error|Warning|Info)\(\s*['"`]/.test(texto))

    expect(fijos.map(([ruta]) => ruta)).toEqual([])
  })

  it('los mensajes de los schemas son claves, no frases', () => {
    const frases = codigo
      .filter(([ruta]) => ruta.includes('/schemas/'))
      .flatMap(([ruta, texto]) =>
        literales(texto)
          .filter((valor) => /\p{L} \p{L}/u.test(valor))
          .map((valor) => `${ruta}: ${valor}`),
      )

    expect(frases).toEqual([])
  })

  it.each(['es', 'en'])('toda clave con espacio de nombres existe en «%s»', (idioma) => {
    const claves = codigo.flatMap(([ruta, texto]) =>
      literales(texto)
        .filter((valor) => /^(shared|auth|usuarios|bitacora):\w+(\.\w+)+$/.test(valor))
        .map((clave) => ({ ruta, clave })),
    )

    expect(claves.length).toBeGreaterThan(10)
    expect(claves.filter(({ clave }) => !i18n.exists(clave, { lng: idioma, fallbackLng: [] }))).toEqual([])
  })
})
