import { useState } from 'react'
import { useForm } from '@tanstack/react-form'
import { EyeIcon, ScrollTextIcon } from 'lucide-react'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { ErrorState } from '@/components/ui/error-state'
import { FormField } from '@/components/ui/form-field'
import { Input } from '@/components/ui/input'
import { ModalDetailLayout } from '@/components/ui/modal-layout'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import {
  DataCards,
  DataTable,
  DataTablePagination,
  RowActionButton,
  useDataTable,
  type DataTableColumnDef,
} from '@/components/ui/tanstack-table'
import { ApiError } from '@/store'
import { formatDateTime } from '@/utils/formatDateTime'
import { useBitacora } from '../api/useBitacora'
import { bitacoraFiltrosSchema, SIN_FILTROS, type BitacoraFiltros } from '../schemas/bitacoraFiltrosSchema'
import type { EventoBitacora } from '../types/bitacora'

const ETIQUETA_RESULTADO: Record<string, string> = { '': 'Todos', EXITO: 'Éxito', FALLO: 'Fallo' }

function Resultado({ valor }: { valor: EventoBitacora['resultado'] }) {
  return <Badge variant={valor === 'FALLO' ? 'destructive' : 'secondary'}>{ETIQUETA_RESULTADO[valor]}</Badge>
}

const columnas: DataTableColumnDef<EventoBitacora>[] = [
  {
    accessorKey: 'fecha',
    header: 'Fecha',
    meta: { width: 'w-40' },
    cell: ({ row }) => formatDateTime(row.original.fecha),
  },
  { accessorKey: 'accion', header: 'Acción', meta: { grow: true, cardTitle: true } },
  { accessorKey: 'modulo', header: 'Módulo', meta: { width: 'w-24' } },
  {
    accessorKey: 'resultado',
    header: 'Resultado',
    meta: { width: 'w-28' },
    cell: ({ row }) => <Resultado valor={row.original.resultado} />,
  },
  {
    accessorKey: 'usuarioCodigo',
    header: 'Usuario',
    meta: { width: 'w-40' },
    cell: ({ row }) => <span title={row.original.usuarioCodigo}>{row.original.usuarioCodigo ?? '—'}</span>,
  },
]

const CAMPOS_TEXTO = [
  { name: 'accion', label: 'Acción', type: 'text' },
  { name: 'modulo', label: 'Módulo', type: 'text' },
  { name: 'usuarioCodigo', label: 'Código de usuario', type: 'text' },
  { name: 'desde', label: 'Desde', type: 'date' },
  { name: 'hasta', label: 'Hasta', type: 'date' },
] as const

function primerError(errores: unknown[]): string | undefined {
  const primero = errores[0]
  return primero && typeof primero === 'object' && 'message' in primero ? String(primero.message) : undefined
}

function Dato({ nombre, valor }: { nombre: string; valor: string | undefined }) {
  return (
    <div className="flex flex-col">
      <dt className="text-xs text-muted-foreground">{nombre}</dt>
      <dd className="break-all">{valor ?? '—'}</dd>
    </div>
  )
}

export function BitacoraScreen() {
  const [filtros, setFiltros] = useState<BitacoraFiltros>(SIN_FILTROS)
  const [page, setPage] = useState(1)
  const [pageSize, setPageSize] = useState(20)
  const [detalle, setDetalle] = useState<EventoBitacora | null>(null)

  const consulta = useBitacora({ page, pageSize, filtros })
  const cambiarTamanio = (nuevo: number) => {
    setPageSize(nuevo)
    setPage(1)
  }
  const { table } = useDataTable({
    data: consulta.data?.data ?? [],
    columns: columnas,
    meta: consulta.data?.meta,
    page,
    pageSize,
    onPageChange: setPage,
    onPageSizeChange: cambiarTamanio,
    getRowId: (evento) => evento.codigo,
  })

  const form = useForm({
    defaultValues: SIN_FILTROS,
    validators: { onSubmit: bitacoraFiltrosSchema },
    onSubmit: ({ value }) => {
      setPage(1)
      setFiltros(value)
    },
  })

  const verDetalle = (evento: EventoBitacora) => (
    <RowActionButton icon={<EyeIcon />} label="Ver detalle" onClick={() => setDetalle(evento)} />
  )

  return (
    <div className="flex flex-col gap-4">
      <form
        noValidate
        className="grid items-start gap-3 sm:grid-cols-2 lg:grid-cols-3"
        onSubmit={(event) => {
          event.preventDefault()
          void form.handleSubmit()
        }}
      >
        {CAMPOS_TEXTO.map((campo) => (
          <form.Field key={campo.name} name={campo.name}>
            {(field) => (
              <FormField
                id={`filtro-${campo.name}`}
                label={campo.label}
                error={primerError(field.state.meta.errors)}
              >
                {(controlProps) => (
                  <Input
                    {...controlProps}
                    type={campo.type}
                    autoComplete="off"
                    value={field.state.value}
                    onChange={(event) => field.handleChange(event.target.value)}
                  />
                )}
              </FormField>
            )}
          </form.Field>
        ))}
        <form.Field name="resultado">
          {(field) => (
            <div className="flex flex-col gap-1.5">
              <span className="text-sm font-medium">Resultado</span>
              <Select
                value={field.state.value}
                onValueChange={(valor) => field.handleChange((valor ?? '') as BitacoraFiltros['resultado'])}
              >
                <SelectTrigger aria-label="Resultado" className="w-full">
                  <SelectValue>{(valor: string | null) => ETIQUETA_RESULTADO[valor ?? '']}</SelectValue>
                </SelectTrigger>
                <SelectContent>
                  {Object.entries(ETIQUETA_RESULTADO).map(([valor, etiqueta]) => (
                    <SelectItem key={valor} value={valor}>
                      {etiqueta}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
          )}
        </form.Field>
        <div className="flex gap-2 sm:col-span-2 lg:col-span-3">
          <Button type="submit">Filtrar</Button>
          <Button
            type="button"
            variant="ghost"
            onClick={() => {
              form.reset()
              setPage(1)
              setFiltros(SIN_FILTROS)
            }}
          >
            Limpiar filtros
          </Button>
        </div>
      </form>

      {consulta.isError ? (
        <ErrorState
          descripcion={
            consulta.error instanceof ApiError ? consulta.error.message : 'No pudimos cargar la bitácora.'
          }
          onReintentar={() => void consulta.refetch()}
        />
      ) : (
        <>
          <div className="hidden md:block">
            <DataTable
              table={table}
              isLoading={consulta.isPending}
              isError={false}
              emptyMessage="No hay eventos con esos filtros"
              renderActions={(row) => verDetalle(row.original)}
              actionsLabel="Detalle"
            />
          </div>
          <div className="md:hidden">
            <DataCards
              table={table}
              isLoading={consulta.isPending}
              isError={false}
              emptyMessage="No hay eventos con esos filtros"
              renderActions={(row) => verDetalle(row.original)}
            />
          </div>
          <DataTablePagination
            meta={consulta.data?.meta}
            page={page}
            onPageChange={setPage}
            onPageSizeChange={cambiarTamanio}
            isLoading={consulta.isFetching}
          />
        </>
      )}

      <ModalDetailLayout
        open={detalle !== null}
        onOpenChange={(open) => !open && setDetalle(null)}
        title={detalle?.accion ?? 'Evento'}
        subtitle={detalle ? formatDateTime(detalle.fecha) : undefined}
        icon={ScrollTextIcon}
      >
        {detalle && (
          <div className="flex flex-col gap-4 text-sm">
            <dl className="grid gap-3 sm:grid-cols-2">
              <Dato nombre="Módulo" valor={detalle.modulo} />
              <Dato nombre="Resultado" valor={ETIQUETA_RESULTADO[detalle.resultado]} />
              <Dato nombre="Usuario" valor={detalle.usuarioCodigo} />
              <Dato nombre="IP" valor={detalle.ip} />
              <Dato nombre="Entidad" valor={detalle.entidad} />
              <Dato nombre="Código de la entidad" valor={detalle.entidadCodigo} />
            </dl>
            {/* * Texto plano: el detalle nunca se interpreta como HTML. */}
            <pre className="overflow-x-auto rounded-md bg-muted p-3 text-xs whitespace-pre-wrap">
              {detalle.detalle ? JSON.stringify(detalle.detalle, null, 2) : 'Sin detalle'}
            </pre>
          </div>
        )}
      </ModalDetailLayout>
    </div>
  )
}
