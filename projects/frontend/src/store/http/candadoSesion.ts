/**
 * Candado compartido entre pestañas para las operaciones que rotan o cierran la
 * sesión (refresh y logout).
 *
 * * Las pestañas comparten las cookies: dos refresh simultáneos mandarían el mismo
 * * token y el backend lo trataría como robado. De a uno, el segundo ya sale con
 * * la cookie nueva.
 * * Usa Web Locks. Si no hay (navegador viejo, o `http://` que no sea localhost),
 * * usa una reserva con vencimiento en localStorage.
 * ! La reserva no es atómica como Web Locks: escribe, espera y confirma, lo que
 * ! deja una ventana mínima. Guarda solo un id al azar y una hora, nunca tokens.
 */
const NOMBRE = 'auth-session'
export const CLAVE_CANDADO = 'auth.candado-sesion'

/** Mayor que lo que puede tardar la tarea (CSRF + refresh, 15 s cada uno). */
export const VIGENCIA_CANDADO_MS = 35_000
const CONFIRMACION_MS = 50

interface Reserva {
  id: string
  hasta: number
}

const esperar = (ms: number) => new Promise<void>((resolve) => setTimeout(resolve, ms))

function leerReserva(): Reserva | null {
  try {
    return JSON.parse(localStorage.getItem(CLAVE_CANDADO) ?? 'null') as Reserva | null
  } catch (error) {
    // * Un valor corrupto se trata como libre; si el storage no está disponible,
    // * la escritura que sigue falla y se continúa sin candado.
    if (error instanceof SyntaxError) return null
    throw error
  }
}

/** `false` si localStorage no está disponible (ventana privada, bloqueado). */
async function reservar(id: string): Promise<boolean> {
  try {
    for (;;) {
      const actual = leerReserva()
      if (!actual || actual.hasta <= Date.now()) {
        localStorage.setItem(CLAVE_CANDADO, JSON.stringify({ id, hasta: Date.now() + VIGENCIA_CANDADO_MS }))
        await esperar(CONFIRMACION_MS)
        if (leerReserva()?.id === id) return true
      } else {
        await esperar(CONFIRMACION_MS)
      }
    }
  } catch {
    return false
  }
}

async function conReservaLocal<T>(tarea: () => Promise<T>): Promise<T> {
  const id = `${Date.now()}-${Math.random()}`
  const reservada = await reservar(id)
  try {
    return await tarea()
  } finally {
    if (reservada && leerReserva()?.id === id) localStorage.removeItem(CLAVE_CANDADO)
  }
}

/** Ejecuta la tarea de a una en todo el navegador. */
export function conCandadoDeSesion<T>(tarea: () => Promise<T>): Promise<T> {
  const locks = globalThis.navigator?.locks
  if (!locks) return conReservaLocal(tarea)
  return locks.request(NOMBRE, tarea) as Promise<T>
}
