import type { components } from '@/shared/api/schema'

type Esquema = components['schemas']['SesionResponse']

/**
 * Sesión vista por el administrador (contrato OpenAPI). `ip`, `userAgent` y
 * `ultimoUsoEn` pueden venir vacíos.
 */
export type Sesion = Required<Omit<Esquema, 'ip' | 'userAgent' | 'ultimoUsoEn'>> &
  Pick<Esquema, 'ip' | 'userAgent' | 'ultimoUsoEn'>
