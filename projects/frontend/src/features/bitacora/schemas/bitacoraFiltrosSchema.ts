import { z } from 'zod'

const fecha = z.union([z.literal(''), z.string().regex(/^\d{4}-\d{2}-\d{2}$/, 'La fecha no es válida')])

export const bitacoraFiltrosSchema = z
  .object({
    accion: z.string().max(100, 'La acción es demasiado larga'),
    modulo: z.string().max(50, 'El módulo es demasiado largo'),
    resultado: z.enum(['', 'EXITO', 'FALLO']),
    usuarioCodigo: z.union([z.literal(''), z.uuid('El código de usuario no es válido')]),
    desde: fecha,
    hasta: fecha,
  })
  .refine((f) => !f.desde || !f.hasta || f.desde <= f.hasta, {
    message: 'La fecha inicial no puede ser posterior a la final',
    path: ['hasta'],
  })

export type BitacoraFiltros = z.infer<typeof bitacoraFiltrosSchema>

export const SIN_FILTROS: BitacoraFiltros = {
  accion: '',
  modulo: '',
  resultado: '',
  usuarioCodigo: '',
  desde: '',
  hasta: '',
}

/**
 * Query params de la API. Las fechas son días locales completos, enviados como
 * instantes ISO-8601 que incluyen ambos extremos.
 */
export function aParametros(f: BitacoraFiltros): Record<string, string> {
  const params: Record<string, string> = {}
  if (f.accion.trim()) params['filter[accion]'] = f.accion.trim()
  if (f.modulo.trim()) params['filter[modulo]'] = f.modulo.trim()
  if (f.resultado) params['filter[resultado]'] = f.resultado
  if (f.usuarioCodigo) params['filter[usuarioCodigo]'] = f.usuarioCodigo
  if (f.desde) params.desde = new Date(`${f.desde}T00:00:00`).toISOString()
  // * La base guarda microsegundos: se completa el último milisegundo del día.
  if (f.hasta) params.hasta = new Date(`${f.hasta}T23:59:59.999`).toISOString().replace('999Z', '999999Z')
  return params
}
