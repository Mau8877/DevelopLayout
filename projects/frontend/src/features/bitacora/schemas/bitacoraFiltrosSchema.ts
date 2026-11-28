import { z } from 'zod'

const fecha = z.union([z.literal(''), z.string().regex(/^\d{4}-\d{2}-\d{2}$/, 'bitacora:validacion.fechaInvalida')])

export const bitacoraFiltrosSchema = z
  .object({
    accion: z.string().max(100, 'bitacora:validacion.accionLarga'),
    modulo: z.string().max(50, 'bitacora:validacion.moduloLargo'),
    resultado: z.enum(['', 'EXITO', 'FALLO']),
    usuarioCodigo: z.union([z.literal(''), z.uuid('bitacora:validacion.codigoInvalido')]),
    desde: fecha,
    hasta: fecha,
  })
  .refine((f) => !f.desde || !f.hasta || f.desde <= f.hasta, {
    message: 'bitacora:validacion.rangoInvalido',
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
  if (f.accion.trim()) params['filter[action]'] = f.accion.trim()
  if (f.modulo.trim()) params['filter[module]'] = f.modulo.trim()
  if (f.resultado) params['filter[result]'] = f.resultado
  if (f.usuarioCodigo) params['filter[userCode]'] = f.usuarioCodigo
  if (f.desde) params.from = new Date(`${f.desde}T00:00:00`).toISOString()
  // * La base guarda microsegundos: se completa el último milisegundo del día.
  if (f.hasta) params.to = new Date(`${f.hasta}T23:59:59.999`).toISOString().replace('999Z', '999999Z')
  return params
}
