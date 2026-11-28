import { describe, expect, it } from 'vitest'

/**
 * Barriles (ARQUITECTURA_FRONTEND.md): cada carpeta obligatoria tiene un
 * `index.ts` que exporta todo, y nadie entra a una carpeta saltándoselo.
 */
const archivos = import.meta.glob('/src/**/*.{ts,tsx}', { query: '?raw', import: 'default', eager: true }) as Record<
  string,
  string
>

const carpetaDe = (ruta: string) => ruta.slice(0, ruta.lastIndexOf('/'))
const moduloDe = (ruta: string) => ruta.slice(ruta.lastIndexOf('/') + 1).replace(/(\.d)?\.tsx?$/, '')
const esPrueba = (ruta: string) => ruta.includes('.test.') || ruta.includes('/tests/')

const TRANSVERSALES = [
  'utils',
  'utils/fechas',
  'utils/formularios',
  'shared/api',
  'shared/language',
  'store',
  'store/http',
  'store/query',
  'store/ui',
  'components/layout',
  'layouts',
].map((carpeta) => `/src/${carpeta}`)
const DE_FEATURE = /^\/src\/features\/[^/]+\/(?!tests$|language$)[^/]+$/
const obligatoria = (carpeta: string) => TRANSVERSALES.includes(carpeta) || DE_FEATURE.test(carpeta)

const modulos = Object.keys(archivos).filter(
  (ruta) => obligatoria(carpetaDe(ruta)) && !esPrueba(ruta) && moduloDe(ruta) !== 'index',
)
const carpetas = [...new Set(modulos.map(carpetaDe))]

function resolver(origen: string, especificador: string): string | null {
  if (especificador.startsWith('@/')) return `/src/${especificador.slice(2)}`
  if (!especificador.startsWith('.')) return null
  const partes = carpetaDe(origen).split('/')
  for (const parte of especificador.split('/')) {
    if (parte === '..') partes.pop()
    else if (parte !== '.') partes.push(parte)
  }
  return partes.join('/')
}

const importaciones = Object.entries(archivos)
  // * `i18next.d.ts` solo importa los JSON como tipos: no entra en el paquete.
  .filter(([ruta]) => !['/src/routeTree.gen.ts', '/src/i18next.d.ts', '/src/estructura.test.ts'].includes(ruta))
  .flatMap(([origen, texto]) =>
    [...texto.matchAll(/\bfrom '([^']+)'|\bimport '([^']+)'/g)].flatMap((m) => {
      const destino = resolver(origen, m[1] ?? m[2])
      return destino ? [{ origen, destino }] : []
    }),
  )

describe('estructura del código', () => {
  it('cada carpeta obligatoria tiene su barril', () => {
    expect(carpetas.length).toBeGreaterThan(5)
    expect(carpetas.filter((carpeta) => !archivos[`${carpeta}/index.ts`])).toEqual([])
  })

  it('las subcarpetas de store y utils están vigiladas y las exporta el barril de su carpeta', () => {
    const subcarpetas = Object.keys(archivos).flatMap((ruta) => {
      const hallada = /^(\/src\/(?:store|utils))\/([^/]+)\/index\.ts$/.exec(ruta)
      return hallada ? [{ padre: hallada[1], nombre: hallada[2] }] : []
    })

    expect(subcarpetas.length).toBeGreaterThan(0)
    expect(
      subcarpetas.filter(
        ({ padre, nombre }) =>
          !obligatoria(`${padre}/${nombre}`) || !(archivos[`${padre}/index.ts`] ?? '').includes(`from './${nombre}'`),
      ),
    ).toEqual([])
  })

  it('cada barril exporta todos los archivos de su carpeta', () => {
    const sinExportar = modulos.filter(
      (ruta) => !(archivos[`${carpetaDe(ruta)}/index.ts`] ?? '').includes(`from './${moduloDe(ruta)}'`),
    )

    expect(sinExportar).toEqual([])
  })

  it('desde otra carpeta se importa del barril, no del archivo', () => {
    const saltos = importaciones.filter(
      ({ origen, destino }) =>
        carpetas.includes(carpetaDe(destino)) &&
        carpetaDe(destino) !== carpetaDe(origen) &&
        // * Una subcarpeta se importa por su propio barril.
        !archivos[`${destino}/index.ts`],
    )

    expect(saltos).toEqual([])
  })

  it('desde fuera de una feature solo se usa su barril raíz', () => {
    const feature = (ruta: string) => /^\/src\/features\/([^/]+)/.exec(ruta)?.[1]
    const internos = importaciones.filter(({ origen, destino }) => {
      const dueña = feature(destino)
      return dueña !== undefined && dueña !== feature(origen) && destino !== `/src/features/${dueña}`
    })

    expect(internos).toEqual([])
  })

  it('un archivo no importa el barril de su propia carpeta', () => {
    const circulares = importaciones.filter(
      ({ origen, destino }) =>
        moduloDe(origen) !== 'index' &&
        carpetas.includes(carpetaDe(origen)) &&
        (destino === carpetaDe(origen) || destino === `${carpetaDe(origen)}/index`),
    )

    expect(circulares).toEqual([])
  })

  it('no hay dependencias circulares entre archivos', () => {
    const aArchivo = (destino: string) =>
      [`${destino}.ts`, `${destino}.tsx`, `${destino}/index.ts`, destino].find((ruta) => ruta in archivos)
    const grafo = new Map<string, string[]>()
    for (const [origen, texto] of Object.entries(archivos)) {
      if (esPrueba(origen) || origen.endsWith('routeTree.gen.ts')) continue
      // * Los imports solo de tipos se borran al compilar: no crean ciclos.
      const sinTipos = texto.replace(/(import|export) type [^;]*?from '[^']+'/g, '')
      grafo.set(
        origen,
        [...sinTipos.matchAll(/\bfrom '([^']+)'/g)].flatMap((m) => {
          const destino = resolver(origen, m[1])
          const archivo = destino ? aArchivo(destino) : undefined
          return archivo && !archivo.endsWith('routeTree.gen.ts') ? [archivo] : []
        }),
      )
    }
    const ciclos = new Set<string>()
    const terminados = new Set<string>()
    const visitar = (nodo: string, camino: string[]) => {
      if (camino.includes(nodo)) {
        ciclos.add(camino.slice(camino.indexOf(nodo)).sort().join(' <-> '))
        return
      }
      if (terminados.has(nodo)) return
      for (const siguiente of grafo.get(nodo) ?? []) visitar(siguiente, [...camino, nodo])
      terminados.add(nodo)
    }
    for (const nodo of grafo.keys()) visitar(nodo, [])

    expect([...ciclos]).toEqual([])
  })
})
