/**
 * ! El texto buscado (parte de un correo) nunca va en una clave: se identifica
 * ! cada búsqueda con un número.
 */
export interface ClaveDeListado {
  busqueda: number
  estado: string
  rol: string
  page: number
  pageSize: number
}

export const usuariosKeys = {
  all: ['usuarios'] as const,
  lists: () => [...usuariosKeys.all, 'list'] as const,
  list: (params: ClaveDeListado) => [...usuariosKeys.lists(), params] as const,
}
