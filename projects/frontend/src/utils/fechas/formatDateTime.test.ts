import { describe, it, expect } from 'vitest'
import { formatDateTime } from './formatDateTime'

describe('formatDateTime', () => {
  it('formatea un instante ISO', () => {
    expect(formatDateTime('2026-03-10T10:00:00Z')).toContain('26')
  })

  it('sin valor o con un valor inválido devuelve un guion', () => {
    expect(formatDateTime(undefined)).toBe('—')
    expect(formatDateTime('no es una fecha')).toBe('—')
  })
})
