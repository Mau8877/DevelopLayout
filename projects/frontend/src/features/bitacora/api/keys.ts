import type { BitacoraFiltros } from '../schemas/bitacoraFiltrosSchema'

export interface BitacoraListParams {
  page: number
  pageSize: number
  filtros: BitacoraFiltros
}

export const bitacoraKeys = {
  all: ['bitacora'] as const,
  lists: () => [...bitacoraKeys.all, 'list'] as const,
  list: (params: BitacoraListParams) => [...bitacoraKeys.lists(), params] as const,
}
