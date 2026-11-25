import { isAxiosError } from 'axios'
import { logger } from '@/utils/logger'
import { createBaseApi } from './createBaseApi'

// Sobre estándar de todas las respuestas del backend -- ver
// .claude/rules/backend/RESPONSES_BACKEND.md. `error` solo viene presente
// cuando status es "failed"; `meta` solo en listados paginados.
export interface ApiEnvelope<T> {
  status: 'success' | 'failed'
  data: T
  message: string
  timestamp: string
  error?: string
  meta?: {
    page: number
    pageSize: number
    totalItems: number
    totalPages: number
  }
}

export class ApiError extends Error {
  readonly code: string

  constructor(code: string, message: string) {
    super(message)
    this.name = 'ApiError'
    this.code = code
  }
}

/**
 * Cliente HTTP único de la app -- todo lo que llame a la API del backend
 * pasa por acá, nunca por una instancia de axios propia.
 *
 * * Construido sobre createBaseApi (logging genérico) y le suma el
 * * desempaquetado del sobre {status,data,message,error,timestamp,meta}:
 * * en éxito, `response.data` queda como el `data` real del backend (y
 * * `meta` si vino); en error, se rechaza con un ApiError tipado en vez de
 * * un AxiosError crudo.
 */
export const apiClient = createBaseApi({
  baseURL: import.meta.env.VITE_API_BASE_URL,
  // * Los tokens viajan en cookies HttpOnly: hay que enviarlas, y adjuntar el
  // * token CSRF (double-submit) también en dev, donde la API es otro origen
  // * (otro puerto) y axios por defecto no lo adjuntaría.
  withCredentials: true,
  xsrfCookieName: 'XSRF-TOKEN',
  xsrfHeaderName: 'X-XSRF-TOKEN',
  withXSRFToken: true,
})

// * Convierte cualquier fallo en un ApiError: con sobre (código de negocio) o
// * sin sobre (red, timeout, respuesta no JSON, 502/504) con códigos CLIENT_*.
export function normalizeError(error: unknown): unknown {
  if (error instanceof ApiError) return error
  if (!isAxiosError(error)) return error

  const envelope = error.response?.data as ApiEnvelope<unknown> | undefined
  if (envelope && typeof envelope === 'object' && envelope.error) {
    return new ApiError(envelope.error, envelope.message)
  }
  if (error.code === 'ECONNABORTED' || error.code === 'ETIMEDOUT') {
    return new ApiError('CLIENT_TIMEOUT', 'La solicitud tardó demasiado. Intentá de nuevo.')
  }
  if (!error.response) {
    return new ApiError('CLIENT_NETWORK', 'No pudimos conectarnos. Revisá tu conexión e intentá de nuevo.')
  }
  return new ApiError('CLIENT_UNKNOWN', 'Ocurrió un error inesperado. Intentá de nuevo.')
}

apiClient.interceptors.response.use(
  (response) => {
    const envelope = response.data as ApiEnvelope<unknown> | undefined | ''
    if (envelope && typeof envelope === 'object' && 'status' in envelope) {
      response.data = envelope.meta ? { data: envelope.data, meta: envelope.meta } : envelope.data
    }
    return response
  },
  (error) => Promise.reject(normalizeError(error)),
)

let csrfRequest: Promise<void> | null = null

/** Pide la cookie XSRF-TOKEN una sola vez al iniciar la app (GET /auth/csrf). */
export function ensureCsrfCookie(): Promise<void> {
  csrfRequest ??= apiClient
    .get('/auth/csrf')
    .then(() => undefined)
    .catch((error: unknown) => {
      csrfRequest = null
      logger.warn('No se pudo obtener el token CSRF', error instanceof ApiError ? error.code : 'desconocido')
    })
  return csrfRequest
}
