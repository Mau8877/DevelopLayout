import { describe, expect, it } from 'vitest'
import declaraciones from '../../i18next.d.ts?raw'

/** Controles de los archivos de idioma (IDIOMAS.md). */
const archivos = import.meta.glob<Record<string, unknown>>('/src/**/language/{es,en}.json', {
  eager: true,
  import: 'default',
})

const carpetas = [...new Set(Object.keys(archivos).map((ruta) => ruta.slice(0, ruta.lastIndexOf('/'))))]

function planos(textos: Record<string, unknown>, prefijo = ''): [string, string][] {
  return Object.entries(textos).flatMap(([clave, valor]) =>
    typeof valor === 'string'
      ? [[prefijo + clave, valor] as [string, string]]
      : planos(valor as Record<string, unknown>, `${prefijo}${clave}.`),
  )
}

const marcadores = (texto: string) => [...texto.matchAll(/\{\{\s*(\w+)\s*\}\}/g)].map((m) => m[1]).sort()

// * Palabras correctas en español neutro que terminan como las del voseo.
const PERMITIDAS = new Set([
  'más', 'además', 'atrás', 'demás', 'estás', 'después', 'través', 'inglés', 'interés', 'país', 'francés',
  'está', 'así', 'ahí', 'aquí', 'allí', 'sí', 'qué', 'porqué', 'allá', 'quizá', 'mí', 'sé',
])
// * Formas que la terminación no delata, y regionalismos.
const SIEMPRE_MAL = new Set([
  'vos', 'sos', 'acá', 'mirá', 'esperá', 'cerrá', 'entrá', 'borrá', 'registrá', 'abrila', 'abrilo', 'cerralo',
  'cerrala', 'pedile', 'usted', 'ordenador', 'vale', 'ahorita',
])

function palabrasConVoseo(texto: string): string[] {
  const palabras = texto.toLowerCase().match(/\p{L}+/gu) ?? []
  return palabras.filter((palabra) => {
    if (SIEMPRE_MAL.has(palabra)) return true
    if (PERMITIDAS.has(palabra)) return false
    // * «tendrás» y «perderá» son futuro, no voseo.
    if (/r(á|ás|é)$/.test(palabra)) return false
    return /(ás|és|ís|á|é|í)$/.test(palabra)
  })
}

describe('archivos de idioma', () => {
  it('cada carpeta language tiene es.json y en.json', () => {
    expect(carpetas.length).toBeGreaterThanOrEqual(4)
    expect(carpetas.filter((c) => !archivos[`${c}/es.json`] || !archivos[`${c}/en.json`])).toEqual([])
  })

  it.each(carpetas)('%s: mismas claves y mismos marcadores en todos los idiomas', (carpeta) => {
    const resumen = (idioma: string) =>
      planos(archivos[`${carpeta}/${idioma}.json`])
        .map(([clave, valor]) => `${clave} ${marcadores(valor).join(',')}`)
        .sort()

    expect(resumen('en')).toEqual(resumen('es'))
  })

  it('ningún texto está vacío', () => {
    const vacios = Object.entries(archivos).flatMap(([ruta, textos]) =>
      planos(textos)
        .filter(([, valor]) => !valor.trim())
        .map(([clave]) => `${ruta} ${clave}`),
    )

    expect(vacios).toEqual([])
  })

  it('el español es neutro: sin voseo ni regionalismos', () => {
    const faltas = Object.entries(archivos)
      .filter(([ruta]) => ruta.endsWith('/es.json'))
      .flatMap(([ruta, textos]) =>
        planos(textos).flatMap(([clave, valor]) =>
          palabrasConVoseo(valor).map((palabra) => `${ruta} ${clave}: ${palabra}`),
        ),
      )

    expect(faltas).toEqual([])
  })

  it('el control de voseo detecta lo que debe', () => {
    expect(palabrasConVoseo('Iniciá sesión y elegí: podés pedile')).toEqual(['iniciá', 'elegí', 'podés', 'pedile'])
    expect(palabrasConVoseo('Inicia sesión, tendrás más y se perderá después')).toEqual([])
  })

  it('cada carpeta language está declarada en i18next.d.ts', () => {
    const sinDeclarar = carpetas.filter((c) => !declaraciones.includes(`from './${c.replace('/src/', '')}/es.json'`))

    expect(sinDeclarar).toEqual([])
  })
})
