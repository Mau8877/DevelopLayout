import {
  columnVisibilityFeature,
  rowPaginationFeature,
  rowSortingFeature,
  tableFeatures,
} from '@tanstack/react-table'

/**
 * Features de TanStack Table v9 que usa este paquete: paginación, orden y
 * visibilidad de columnas (necesaria para `row.getVisibleCells()`). En v9 cada tabla declara qué features usa (ya no viene todo
 * incluido como en v8).
 *
 * * Es una constante a nivel de módulo (identidad estable entre renders).
 * * No se registran row models de paginación/orden/filtro (`paginatedRowModel`,
 * * `sortedRowModel`, `filteredRowModel`): el backend es la única fuente de
 * * verdad (ver .claude/rules/backend/ENDPOINTS_BACKEND.md), y el modelo de
 * * filas base (core) siempre está incluido automáticamente.
 * ! No agregar `columnFilteringFeature` ni row models acá sin cambiar antes
 * ! la premisa de tabla 100% server-side.
 */
export const dataTableFeatures = tableFeatures({
  columnVisibilityFeature,
  rowPaginationFeature,
  rowSortingFeature,
})

export type DataTableFeatures = typeof dataTableFeatures
