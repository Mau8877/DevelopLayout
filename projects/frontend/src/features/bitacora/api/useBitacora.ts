import { keepPreviousData, useQuery } from '@tanstack/react-query'
import type { DataTablePageMeta } from '@/components/ui/tanstack-table'
import { apiClient } from '@/store'
import { aParametros } from '../schemas'
import type { EventoBitacora } from '../types'
import { bitacoraKeys, type BitacoraListParams } from './keys'

interface Pagina {
  data: EventoBitacora[]
  meta: DataTablePageMeta
}

export function useBitacora(params: BitacoraListParams) {
  return useQuery({
    queryKey: bitacoraKeys.list(params),
    queryFn: async () => {
      const response = await apiClient.get<Pagina>('/audit-log', {
        params: { page: params.page, pageSize: params.pageSize, ...aParametros(params.filtros) },
      })
      return response.data
    },
    placeholderData: keepPreviousData,
  })
}
