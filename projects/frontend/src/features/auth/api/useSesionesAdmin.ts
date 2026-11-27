import { useQuery } from '@tanstack/react-query'
import type { DataTablePageMeta } from '@/components/ui/tanstack-table'
import { apiClient } from '@/store'
import type { Sesion } from '../types/sesion'
import { authKeys } from './keys'

/** `id` distingue una búsqueda de otra sin poner el correo en la clave de caché. */
export interface Busqueda {
  id: number
  email: string
}

let ultimaBusqueda = 0

export function nuevaBusqueda(email: string): Busqueda {
  ultimaBusqueda += 1
  return { id: ultimaBusqueda, email }
}

interface Pagina {
  data: Sesion[]
  meta: DataTablePageMeta
}

export function useSesionesAdmin(busqueda: Busqueda | null, page: number, pageSize: number) {
  return useQuery({
    queryKey: authKeys.sesionesAdminList({ busqueda: busqueda?.id ?? 0, page, pageSize }),
    enabled: busqueda !== null,
    queryFn: async () => {
      // * POST con el correo en el body: no queda en URLs ni en logs de acceso.
      const response = await apiClient.post<Pagina>('/admin/sesiones/buscar', {
        email: busqueda?.email,
        page,
        pageSize,
      })
      return response.data
    },
    // * Se conserva la página anterior solo dentro de la misma búsqueda: nunca
    // * se muestran sesiones de otra persona mientras carga.
    placeholderData: (anterior, consultaAnterior) => {
      const params = consultaAnterior?.queryKey[4] as { busqueda: number } | undefined
      return params?.busqueda === busqueda?.id ? anterior : undefined
    },
  })
}
