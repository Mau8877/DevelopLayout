import { keepPreviousData, useQuery } from '@tanstack/react-query'
import type { DataTablePageMeta } from '@/components/ui/tanstack-table'
import { apiClient } from '@/store'
import type { MiSesion } from '../types/sesion'
import { authKeys } from './keys'

interface Pagina {
  data: MiSesion[]
  meta: DataTablePageMeta
}

export function useMisSesiones(page: number, pageSize: number) {
  return useQuery({
    queryKey: authKeys.misSesionesList({ page, pageSize }),
    queryFn: async () => {
      const response = await apiClient.get<Pagina>('/auth/sesiones', { params: { page, pageSize } })
      return response.data
    },
    // * Todas las páginas son del mismo usuario (la caché se limpia al cambiar de sesión).
    placeholderData: keepPreviousData,
  })
}
