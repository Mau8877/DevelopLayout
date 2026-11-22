import { useTable } from '@tanstack/react-table'
import type { PaginationState, RowData, SortingState, Updater } from '@tanstack/react-table'
import { dataTableFeatures } from './tableFeatures'
import type { UseDataTableOptions, UseDataTableReturn } from './types'

function resolveUpdater<T>(updater: Updater<T>, current: T): T {
  return typeof updater === 'function' ? (updater as (old: T) => T)(current) : updater
}

/**
 * El "cerebro" del paquete: wrapper tipado sobre `useTable` (TanStack Table
 * v9) con los defaults de una tabla 100% server-side -- `manualPagination`
 * y `manualSorting` siempre en `true`, solo las features de paginación y
 * orden (ver `tableFeatures.ts`) y el modelo de filas core. Nunca se
 * registran row models de paginación/orden/filtro: agregarlos rompería la
 * premisa de que el backend es la única fuente de verdad de paginación/
 * orden/filtro (ver .claude/rules/backend/ENDPOINTS_BACKEND.md). Con
 * `enableSorting`, además fuerza `enableMultiSort: false` -- el backend
 * solo acepta un campo de orden por request (`sort=campo`/`sort=-campo`).
 *
 * Convierte `page` (1-based, misma convención que el query param del
 * backend) a `pageIndex` (0-based, interno de TanStack Table) puertas
 * adentro -- la feature que consume este hook nunca ve `pageIndex`.
 */
export function useDataTable<TData extends RowData>(
  options: UseDataTableOptions<TData>,
): UseDataTableReturn<TData> {
  const {
    data,
    columns,
    meta,
    page,
    pageSize,
    onPageChange,
    onPageSizeChange,
    enableSorting = false,
    sorting = [],
    onSortingChange,
    getRowId,
  } = options

  const pageCount = meta ? meta.totalPages : -1

  const table = useTable({
    features: dataTableFeatures,
    data,
    columns,
    state: {
      pagination: { pageIndex: page - 1, pageSize },
      ...(enableSorting ? { sorting } : {}),
    },
    pageCount,
    manualPagination: true,
    manualSorting: true,
    enableSorting,
    enableMultiSort: false,
    onPaginationChange: (updater) => {
      const current: PaginationState = { pageIndex: page - 1, pageSize }
      const next = resolveUpdater(updater, current)
      if (next.pageSize !== pageSize) {
        onPageSizeChange(next.pageSize)
      } else if (next.pageIndex !== current.pageIndex) {
        onPageChange(next.pageIndex + 1)
      }
    },
    onSortingChange: enableSorting
      ? (updater) => onSortingChange?.(resolveUpdater<SortingState>(updater, sorting))
      : undefined,
    getRowId,
  })

  return {
    table,
    pageCount,
    canPreviousPage: page > 1,
    // Sin `meta` (primera carga) no se puede afirmar que hay una página
    // siguiente -- se asume que no, en vez de habilitar un botón que
    // todavía no tiene a dónde ir.
    canNextPage: meta ? page < meta.totalPages : false,
  }
}
