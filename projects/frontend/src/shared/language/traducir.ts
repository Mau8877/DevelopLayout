import { i18n } from './i18n'
import { IDIOMA_POR_DEFECTO, IDIOMAS } from './idiomas'

/** Etiqueta para formatear fechas y números en el idioma vigente. */
export function formatoActual(): string {
  const idioma = IDIOMAS.find((i) => i.codigo === i18n.resolvedLanguage)
  return idioma?.formato ?? IDIOMA_POR_DEFECTO
}

/**
 * Traduce un mensaje que puede ser una clave (los de Zod) o un texto ya listo
 * (los que llegan del servidor), que se deja igual.
 */
export function traducirMensaje(mensaje: string | undefined): string | undefined {
  if (mensaje === undefined) return undefined
  // * Las claves dinámicas no se pueden comprobar con los tipos.
  const t = i18n.t.bind(i18n) as (clave: string) => string
  return /^[a-z]+:[\w.]+$/i.test(mensaje) && i18n.exists(mensaje) ? t(mensaje) : mensaje
}
