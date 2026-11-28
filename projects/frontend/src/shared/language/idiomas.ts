/** Idiomas soportados. Cada nombre va escrito en su propio idioma. */
export const IDIOMAS = [
  { codigo: 'es', nombre: 'Español', formato: 'es' },
  { codigo: 'en', nombre: 'English', formato: 'en-US' },
] as const

export type CodigoIdioma = (typeof IDIOMAS)[number]['codigo']

export const IDIOMA_POR_DEFECTO: CodigoIdioma = 'es'

// * No es un dato sensible: puede vivir en localStorage.
const CLAVE_PREFERENCIA = 'idioma'

export function esIdioma(valor: unknown): valor is CodigoIdioma {
  return IDIOMAS.some((idioma) => idioma.codigo === valor)
}

export function leerPreferencia(): CodigoIdioma | undefined {
  try {
    const valor = window.localStorage.getItem(CLAVE_PREFERENCIA)
    return esIdioma(valor) ? valor : undefined
  } catch {
    return undefined
  }
}

export function guardarPreferencia(codigo: CodigoIdioma): void {
  try {
    window.localStorage.setItem(CLAVE_PREFERENCIA, codigo)
  } catch {
    // * Sin almacenamiento (ventana privada): el idioma dura lo que dure la pestaña.
  }
}

/** La preferencia guardada; si no, el primer idioma soportado del navegador; si no, español. */
export function detectarIdioma(
  guardado: CodigoIdioma | undefined = leerPreferencia(),
  delNavegador: readonly string[] = window.navigator.languages ?? [],
): CodigoIdioma {
  if (guardado) return guardado
  for (const etiqueta of delNavegador) {
    const base = etiqueta.toLowerCase().split('-')[0]
    if (esIdioma(base)) return base
  }
  return IDIOMA_POR_DEFECTO
}
