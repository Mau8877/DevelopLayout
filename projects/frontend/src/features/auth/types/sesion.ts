import type { components } from '@/shared/api'

type Esquema = components['schemas']['SesionResponse']

/**
 * Sesión vista por el administrador (contrato OpenAPI). `ip`, `userAgent` y
 * `ultimoUsoEn` pueden venir vacíos.
 */
export type Sesion = Required<Omit<Esquema, 'ip' | 'userAgent' | 'ultimoUsoEn'>> &
  Pick<Esquema, 'ip' | 'userAgent' | 'ultimoUsoEn'>

type EsquemaPropio = components['schemas']['MiSesionResponse']

/** Sesión que ve su propio dueño. `actual` es la sesión desde la que se consulta. */
export type MiSesion = Required<Omit<EsquemaPropio, 'ip' | 'userAgent' | 'ultimoUsoEn'>> &
  Pick<EsquemaPropio, 'ip' | 'userAgent' | 'ultimoUsoEn'>

/** Lo que muestran las columnas comunes a las dos pantallas de sesiones. */
export type CamposDeSesion = Pick<MiSesion, 'tipoCliente' | 'ip' | 'userAgent' | 'creadaEn' | 'ultimoUsoEn'>
