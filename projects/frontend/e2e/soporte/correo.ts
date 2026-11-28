import { expect } from '@playwright/test'
import { entorno } from './entorno'

export interface Correo {
  asunto: string
  texto: string
}

async function mensajesDe(destinatario: string): Promise<{ ID: string; Subject: string }[]> {
  const consulta = encodeURIComponent(`to:"${destinatario}"`)
  const respuesta = await fetch(`${entorno.mailpit}/api/v1/search?query=${consulta}`)
  return ((await respuesta.json()) as { messages: { ID: string; Subject: string }[] }).messages
}

/**
 * Espera (sin pausas fijas) a que el buzón desechable tenga `cantidad` correos
 * para esa dirección y devuelve el más reciente.
 */
export async function esperarCorreo(destinatario: string, { cantidad = 1 } = {}): Promise<Correo> {
  await expect
    .poll(async () => (await mensajesDe(destinatario)).length, { timeout: 30_000 })
    .toBeGreaterThanOrEqual(cantidad)
  // * Mailpit devuelve primero el más nuevo.
  const [{ ID, Subject }] = await mensajesDe(destinatario)
  const respuesta = await fetch(`${entorno.mailpit}/api/v1/message/${ID}`)
  const { Text } = (await respuesta.json()) as { Text: string }
  return { asunto: Subject, texto: Text }
}

export function enlaceDe(texto: string): string {
  const enlace = /https?:\/\/\S+/.exec(texto)?.[0]
  if (!enlace) throw new Error('el correo no trae ningún enlace')
  return enlace
}

export function tokenDelEnlace(enlace: string): string {
  return new URL(enlace).hash.replace('#token=', '')
}
