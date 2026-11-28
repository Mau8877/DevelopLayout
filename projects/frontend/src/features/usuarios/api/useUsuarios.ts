import { useQuery } from '@tanstack/react-query'
import type { DataTablePageMeta } from '@/components/ui/tanstack-table'
import { apiClient } from '@/store'
import type { EstadoUsuario, Usuario } from '../types'
import { usuariosKeys, type ClaveDeListado } from './keys'

/** `id` distingue una búsqueda de otra sin poner el texto en la clave de caché. */
export interface Busqueda {
  id: number
  texto: string
}

export const SIN_BUSQUEDA: Busqueda = { id: 0, texto: '' }

let ultimaBusqueda = 0

export function nuevaBusqueda(texto: string): Busqueda {
  const limpio = texto.trim()
  if (!limpio) return SIN_BUSQUEDA
  ultimaBusqueda += 1
  return { id: ultimaBusqueda, texto: limpio }
}

export interface ParametrosDeListado {
  busqueda: Busqueda
  estado?: EstadoUsuario
  rol?: string
  page: number
  pageSize: number
}

interface Pagina {
  data: Usuario[]
  meta: DataTablePageMeta
}

export function useUsuarios({ busqueda, estado, rol, page, pageSize }: ParametrosDeListado) {
  return useQuery({
    queryKey: usuariosKeys.list({ busqueda: busqueda.id, estado: estado ?? '', rol: rol ?? '', page, pageSize }),
    queryFn: async () => {
      // * POST con el texto en el body: no queda en URLs ni en logs de acceso.
      const response = await apiClient.post<Pagina>('/users/search', {
        page,
        pageSize,
        search: busqueda.texto || undefined,
        estado,
        rol,
      })
      return response.data
    },
    // * La página anterior se conserva solo dentro de la misma búsqueda.
    placeholderData: (anterior, consultaAnterior) => {
      const previa = consultaAnterior?.queryKey[2] as ClaveDeListado | undefined
      return previa?.busqueda === busqueda.id ? anterior : undefined
    },
  })
}
