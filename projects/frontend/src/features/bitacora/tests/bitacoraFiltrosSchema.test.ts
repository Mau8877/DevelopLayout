import { describe, it, expect } from 'vitest'
import { aParametros, bitacoraFiltrosSchema, SIN_FILTROS } from '../schemas/bitacoraFiltrosSchema'

const con = (cambios: Partial<typeof SIN_FILTROS>) => ({ ...SIN_FILTROS, ...cambios })

describe('bitacoraFiltrosSchema', () => {
  it('acepta sin filtros', () => {
    expect(bitacoraFiltrosSchema.safeParse(SIN_FILTROS).success).toBe(true)
    expect(aParametros(SIN_FILTROS)).toEqual({})
  })

  it('rechaza un rango invertido y un código que no es UUID', () => {
    expect(bitacoraFiltrosSchema.safeParse(con({ desde: '2026-03-02', hasta: '2026-03-01' })).success).toBe(false)
    expect(bitacoraFiltrosSchema.safeParse(con({ usuarioCodigo: 'abc' })).success).toBe(false)
  })

  it('arma los query params con el día local completo', () => {
    const params = aParametros(con({ accion: ' AUTH_LOGOUT ', resultado: 'FALLO', desde: '2026-03-01', hasta: '2026-03-01' }))

    expect(params['filter[accion]']).toBe('AUTH_LOGOUT')
    expect(params['filter[resultado]']).toBe('FALLO')
    expect(params.desde).toBe(new Date(2026, 2, 1, 0, 0, 0, 0).toISOString())
    expect(params.hasta).toBe(new Date(2026, 2, 1, 23, 59, 59, 999).toISOString().replace('999Z', '999999Z'))
  })
})
