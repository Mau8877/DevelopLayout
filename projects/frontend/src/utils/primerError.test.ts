import { describe, expect, it } from 'vitest'
import { primerError } from './primerError'

describe('primerError', () => {
  it('acepta un string o un objeto con message', () => {
    expect(primerError(['uno', 'dos'])).toBe('uno')
    expect(primerError([{ message: 'tres' }])).toBe('tres')
  })

  it('sin errores o con algo desconocido no devuelve nada', () => {
    expect(primerError([])).toBeUndefined()
    expect(primerError([undefined])).toBeUndefined()
    expect(primerError([{}])).toBeUndefined()
  })
})
