/**
 * Primer mensaje de los errores de un campo de formulario.
 *
 * * Los errores de validación llegan como strings o como objetos con `message`
 * * (Standard Schema).
 */
export function primerError(errores: unknown[]): string | undefined {
  const primero = errores[0]
  if (typeof primero === 'string') return primero
  if (primero && typeof primero === 'object' && 'message' in primero) {
    return String((primero as { message: unknown }).message)
  }
  return undefined
}
