import { i18n } from '@/shared/language'
import { ApiError } from '@/store'

export function mensajeDelError(error: unknown): string {
  return error instanceof ApiError ? error.message : i18n.t('usuarios:errorDeAccion')
}
