import { traducirMensaje } from '@/shared/language'

/**
 * Primer mensaje de los errores de un campo de formulario.
 *
 * * Los errores de validación llegan como strings o como objetos con `message`
 * * (Standard Schema). Los de Zod son claves de idioma: se traducen acá.
 */
export function primerError(errores: unknown[]): string | undefined {
  return traducirMensaje(mensajeCrudo(errores[0]))
}

function mensajeCrudo(error: unknown): string | undefined {
  if (typeof error === 'string') return error
  if (error && typeof error === 'object' && 'message' in error) {
    return String((error as { message: unknown }).message)
  }
  return undefined
}
