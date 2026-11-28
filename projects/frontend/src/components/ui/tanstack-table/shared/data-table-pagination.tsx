import { useTranslation } from 'react-i18next'
import { ChevronLeftIcon, ChevronRightIcon } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { cn } from '@/lib/utils'
import { ELLIPSIS, getPaginationRange } from './paginationRange'
import type { DataTablePageMeta } from './types'

export interface DataTablePaginationProps {
  meta: DataTablePageMeta | undefined
  page: number
  onPageChange: (page: number) => void
  onPageSizeChange: (pageSize: number) => void
  pageSizeOptions?: number[]
  /** Deshabilita los controles mientras la feature refetchea. */
  isLoading?: boolean
  /** Números de página mostrados a cada lado de la actual antes del "…" (default 1). */
  siblingCount?: number
  className?: string
}

// Mismo set fijo definido en .claude/rules/backend/ENDPOINTS_BACKEND.md --
// 20 es el default (mismo valor que asume el backend cuando se omite el
// query param `pageSize`), no simplemente "el primer valor del array".
const DEFAULT_PAGE_SIZE_OPTIONS = [10, 20, 50]
const DEFAULT_PAGE_SIZE = 20

/**
 * Controles de paginación basados 100% en la `PageMeta` que devuelve el
 * backend (ver .claude/rules/backend/RESPONSES_BACKEND.md) -- agnóstico de
 * si se usa debajo de `DataTable` o de `DataCards`.
 */
export function DataTablePagination({
  meta,
  page,
  onPageChange,
  onPageSizeChange,
  pageSizeOptions = DEFAULT_PAGE_SIZE_OPTIONS,
  isLoading = false,
  siblingCount = 1,
  className,
}: DataTablePaginationProps) {
  const { t } = useTranslation('shared')
  const pageSize = meta?.pageSize ?? DEFAULT_PAGE_SIZE
  const canPreviousPage = page > 1 && !isLoading
  const canNextPage = !!meta && page < meta.totalPages && !isLoading
  // Con totalItems en 0, totalPages viene en 0 (Math.ceil(0 / pageSize)) --
  // se muestra como 1 para no mostrar un rango vacío de páginas.
  const totalPagesDisplay = meta ? Math.max(1, meta.totalPages) : 1
  const pageRange = getPaginationRange(page, totalPagesDisplay, siblingCount)

  return (
    <div className={cn('flex flex-wrap items-center justify-between gap-4', className)}>
      <div className="flex items-center gap-2 text-sm text-muted-foreground">
        <span>{t('tabla.filasPorPagina')}</span>
        <Select value={pageSize} onValueChange={(value) => onPageSizeChange(Number(value))}>
          <SelectTrigger size="sm" aria-label={t('tabla.filasPorPagina')}>
            <SelectValue />
          </SelectTrigger>
          <SelectContent>
            {pageSizeOptions.map((option) => (
              <SelectItem key={option} value={option}>
                {option}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
        {meta && <span>· {t('tabla.resultados', { count: meta.totalItems })}</span>}
      </div>

      <nav className="flex items-center gap-1" aria-label={t('tabla.paginacion')}>
        <Button
          variant="ghost"
          size="sm"
          aria-label={t('tabla.paginaAnterior')}
          disabled={!canPreviousPage}
          onClick={() => onPageChange(page - 1)}
        >
          <ChevronLeftIcon data-icon="inline-start" />
          {t('tabla.anterior')}
        </Button>

        {pageRange.map((item, index) =>
          item === ELLIPSIS ? (
            <span key={`ellipsis-${index}`} className="px-1.5 text-sm text-muted-foreground">
              …
            </span>
          ) : (
            <Button
              key={item}
              variant="ghost"
              size="icon-sm"
              className={cn(
                'rounded-full',
                item === page && 'bg-accent text-accent-foreground hover:bg-accent/90',
              )}
              aria-label={t('tabla.pagina', { numero: item })}
              aria-current={item === page ? 'page' : undefined}
              disabled={isLoading}
              onClick={() => onPageChange(item)}
            >
              {item}
            </Button>
          ),
        )}

        <Button
          variant="ghost"
          size="sm"
          aria-label={t('tabla.paginaSiguiente')}
          disabled={!canNextPage}
          onClick={() => onPageChange(page + 1)}
        >
          {t('tabla.siguiente')}
          <ChevronRightIcon data-icon="inline-end" />
        </Button>
      </nav>
    </div>
  )
}
