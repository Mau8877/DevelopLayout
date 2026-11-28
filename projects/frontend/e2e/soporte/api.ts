import { randomUUID } from 'node:crypto'
import { enlaceDe, esperarCorreo, tokenDelEnlace } from './correo'
import { entorno } from './entorno'

/** Preparación de datos por la API, como un cliente sin cookies (exento de CSRF). */
export async function llamar<T = unknown>(
  ruta: string,
  opciones: { metodo?: string; body?: unknown; token?: string } = {},
) {
  const respuesta = await fetch(`${entorno.api}${ruta}`, {
    method: opciones.metodo ?? 'GET',
    headers: {
      'X-Client-Type': 'mobile',
      'Accept-Language': 'es',
      ...(opciones.body === undefined ? {} : { 'Content-Type': 'application/json' }),
      ...(opciones.token ? { Authorization: `Bearer ${opciones.token}` } : {}),
    },
    body: opciones.body === undefined ? undefined : JSON.stringify(opciones.body),
  })
  const sobre = (await respuesta.json()) as { data: T; error?: string; message: string }
  // * `status` al final: el sobre trae su propio campo `status` con otro significado.
  return { ...sobre, status: respuesta.status }
}

/** Falla con el motivo si la preparación de datos no salió bien. */
async function exigir<T>(ruta: string, opciones: Parameters<typeof llamar>[1]): Promise<T> {
  const r = await llamar<T>(ruta, opciones)
  if (r.status >= 300) throw new Error(`${ruta} respondió ${r.status} ${r.error}: ${r.message}`)
  return r.data
}

export async function tokenDe({ correo, password }: Cuenta): Promise<string> {
  const datos = await exigir<{ accessToken: string }>('/auth/login', {
    metodo: 'POST',
    body: { email: correo, password },
  })
  return datos.accessToken
}

export const nuevoCorreo = () => `e2e-${randomUUID().slice(0, 12)}@e2e.test`

export const nuevaPassword = () => `Prueba-${randomUUID().slice(0, 20)}`

export interface Cuenta {
  correo: string
  password: string
}

/** Cada prueba crea sus cuentas: no depende de datos de otra prueba. */
export async function crearCuenta(roles: string[] = ['USUARIO']): Promise<Cuenta> {
  const token = await tokenDe(entorno.admin)
  const cuenta = { correo: nuevoCorreo(), password: nuevaPassword() }
  await exigir('/users', { metodo: 'POST', token, body: { email: cuenta.correo, roles } })
  const correo = await esperarCorreo(cuenta.correo)
  await exigir('/auth/invitation/accept', {
    metodo: 'POST',
    body: { token: tokenDelEnlace(enlaceDe(correo.texto)), password: cuenta.password },
  })
  return cuenta
}
