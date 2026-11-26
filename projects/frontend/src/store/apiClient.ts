import { isAxiosError, type InternalAxiosRequestConfig } from 'axios'
import { logger } from '@/utils/logger'
import { conCandadoDeSesion } from './candadoSesion'
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
  /** Status HTTP, si hubo respuesta. */
  readonly status?: number

  constructor(code: string, message: string, status?: number) {
    super(message)
    this.name = 'ApiError'
    this.code = code
    this.status = status
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
    return new ApiError(envelope.error, envelope.message, error.response?.status)
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
  (error) => renovarYReintentar(error),
)

// ------------------------------------------------------------------ renovación

declare module 'axios' {
  interface InternalAxiosRequestConfig {
    /** Cuántas renovaciones había cuando se envió la request. */
    _renovaciones?: number
    _reintentado?: boolean
  }
}

// * Un 401 de estas rutas no se arregla renovando: son las que crean, rotan o cierran la sesión.
const SIN_RENOVACION = ['/auth/login', '/auth/refresh', '/auth/logout']

/** Sin límite, una request colgada retendría el candado de sesión de todas las pestañas. */
export const TIMEOUT_SESION_MS = 15_000

let renovaciones = 0
let renovacionEnCurso: Promise<void> | null = null
let alExpirarSesion: () => void = () => {}

/** Registra qué hacer cuando el backend ya no deja renovar la sesión (lo conecta main.tsx). */
export function onSesionExpirada(handler: () => void): void {
  alExpirarSesion = handler
}

function renovarSesion(): Promise<void> {
  renovacionEnCurso ??= conCandadoDeSesion(async () => {
    await ensureCsrfCookie()
    await apiClient.post('/auth/refresh', undefined, { timeout: TIMEOUT_SESION_MS })
    renovaciones += 1
  }).finally(() => {
    renovacionEnCurso = null
  })
  return renovacionEnCurso
}

async function renovarYReintentar(error: unknown) {
  const config: InternalAxiosRequestConfig | undefined = isAxiosError(error) ? error.config : undefined
  const renovable =
    isAxiosError(error) &&
    error.response?.status === 401 &&
    config !== undefined &&
    !config._reintentado &&
    !SIN_RENOVACION.some((ruta) => config.url?.startsWith(ruta))
  if (!renovable) throw normalizeError(error)

  // * Si ya se renovó después de enviar esta request, alcanza con reintentarla.
  if (config._renovaciones === renovaciones) {
    try {
      await renovarSesion()
    } catch (fallo) {
      if (fallo instanceof ApiError && (fallo.status === 401 || fallo.status === 403)) {
        alExpirarSesion()
        throw normalizeError(error)
      }
      // * Red, 429 o 500: la sesión puede seguir viva, no se cierra.
      throw fallo
    }
  }
  config._reintentado = true
  return apiClient(config)
}

apiClient.interceptors.request.use((config) => {
  config._renovaciones ??= renovaciones
  return config
})

let csrfRequest: Promise<void> | null = null

/** Pide la cookie XSRF-TOKEN una sola vez al iniciar la app (GET /auth/csrf). */
export function ensureCsrfCookie(): Promise<void> {
  csrfRequest ??= apiClient
    .get('/auth/csrf', { timeout: TIMEOUT_SESION_MS })
    .then(() => undefined)
    .catch((error: unknown) => {
      csrfRequest = null
      logger.warn('No se pudo obtener el token CSRF', error instanceof ApiError ? error.code : 'desconocido')
    })
  return csrfRequest
}
