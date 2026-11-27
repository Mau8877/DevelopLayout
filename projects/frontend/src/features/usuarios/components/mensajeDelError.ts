import { ApiError } from '@/store'

export function mensajeDelError(error: unknown): string {
  return error instanceof ApiError ? error.message : 'No pudimos completar la acción. Intentá de nuevo.'
}
