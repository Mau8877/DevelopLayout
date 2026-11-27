const formato = new Intl.DateTimeFormat('es', { dateStyle: 'short', timeStyle: 'medium' })

/** Fecha y hora locales de un instante ISO-8601; "—" si no hay valor. */
export function formatDateTime(iso: string | null | undefined): string {
  if (!iso) return '—'
  const fecha = new Date(iso)
  return Number.isNaN(fecha.getTime()) ? '—' : formato.format(fecha)
}
