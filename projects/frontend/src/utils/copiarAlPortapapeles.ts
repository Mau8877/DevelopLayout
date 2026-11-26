/**
 * Copia texto al portapapeles. `false` si el navegador no lo permite (por
 * ejemplo, `http://` fuera de localhost).
 */
export async function copiarAlPortapapeles(texto: string): Promise<boolean> {
  try {
    if (!navigator.clipboard) return false
    await navigator.clipboard.writeText(texto)
    return true
  } catch {
    return false
  }
}
