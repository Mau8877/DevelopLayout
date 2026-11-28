import { formatoActual } from '@/shared/language'

const formateadores = new Map<string, Intl.DateTimeFormat>()

function formateador(idioma: string): Intl.DateTimeFormat {
  const existente = formateadores.get(idioma)
  if (existente) return existente
  const nuevo = new Intl.DateTimeFormat(idioma, { dateStyle: 'short', timeStyle: 'medium' })
  formateadores.set(idioma, nuevo)
  return nuevo
}

/** Fecha y hora locales de un instante ISO-8601, en el idioma vigente; "—" si no hay valor. */
export function formatDateTime(iso: string | null | undefined): string {
  if (!iso) return '—'
  const fecha = new Date(iso)
  return Number.isNaN(fecha.getTime()) ? '—' : formateador(formatoActual()).format(fecha)
}
