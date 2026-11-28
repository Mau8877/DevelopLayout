import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { useForm } from '@tanstack/react-form'
import type { TFunction } from 'i18next'
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
import { formatDateTime } from '@/utils'
import { primerError } from '@/utils'
import { useBitacora } from '../api'
import { bitacoraFiltrosSchema, SIN_FILTROS, type BitacoraFiltros } from '../schemas'
import type { EventoBitacora } from '../types'

// * Se guardan claves, nunca textos: el valor del contrato no se traduce.
const CLAVE_RESULTADO = {
  '': 'resultado.todos',
  EXITO: 'resultado.exito',
  FALLO: 'resultado.fallo',
} as const

function Resultado({ valor }: { valor: EventoBitacora['resultado'] }) {
  const { t } = useTranslation('bitacora')
  return <Badge variant={valor === 'FALLO' ? 'destructive' : 'secondary'}>{t(CLAVE_RESULTADO[valor])}</Badge>
}

function crearColumnas(t: TFunction<'bitacora'>): DataTableColumnDef<EventoBitacora>[] {
  return [
    {
      accessorKey: 'fecha',
      header: t('campo.fecha'),
      meta: { width: 'w-40' },
      cell: ({ row }) => formatDateTime(row.original.fecha),
    },
    { accessorKey: 'accion', header: t('campo.accion'), meta: { grow: true, cardTitle: true } },
    { accessorKey: 'modulo', header: t('campo.modulo'), meta: { width: 'w-24' } },
    {
      accessorKey: 'resultado',
      header: t('campo.resultado'),
      meta: { width: 'w-28' },
      cell: ({ row }) => <Resultado valor={row.original.resultado} />,
    },
    {
      accessorKey: 'usuarioCodigo',
      header: t('campo.usuario'),
      meta: { width: 'w-40' },
      cell: ({ row }) => <span title={row.original.usuarioCodigo}>{row.original.usuarioCodigo ?? '—'}</span>,
    },
  ]
}

const CAMPOS_TEXTO = [
  { name: 'accion', clave: 'campo.accion', type: 'text' },
  { name: 'modulo', clave: 'campo.modulo', type: 'text' },
  { name: 'usuarioCodigo', clave: 'campo.codigoUsuario', type: 'text' },
  { name: 'desde', clave: 'campo.desde', type: 'date' },
  { name: 'hasta', clave: 'campo.hasta', type: 'date' },
] as const

function Dato({ nombre, valor }: { nombre: string; valor: string | undefined }) {
  return (
    <div className="flex flex-col">
      <dt className="text-xs text-muted-foreground">{nombre}</dt>
      <dd className="break-all">{valor ?? '—'}</dd>
    </div>
  )
}

export function BitacoraScreen() {
  const { t } = useTranslation('bitacora')
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
    columns: crearColumnas(t),
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
    <RowActionButton icon={<EyeIcon />} label={t('verDetalle')} onClick={() => setDetalle(evento)} />
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
                label={t(campo.clave)}
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
              <span className="text-sm font-medium">{t('campo.resultado')}</span>
              <Select
                value={field.state.value}
                onValueChange={(valor) => field.handleChange((valor ?? '') as BitacoraFiltros['resultado'])}
              >
                <SelectTrigger aria-label={t('campo.resultado')} className="w-full">
                  <SelectValue>
                    {(valor: string | null) => t(CLAVE_RESULTADO[(valor ?? '') as BitacoraFiltros['resultado']])}
                  </SelectValue>
                </SelectTrigger>
                <SelectContent>
                  {Object.entries(CLAVE_RESULTADO).map(([valor, clave]) => (
                    <SelectItem key={valor} value={valor}>
                      {t(clave)}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
          )}
        </form.Field>
        <div className="flex gap-2 sm:col-span-2 lg:col-span-3">
          <Button type="submit">{t('filtrar')}</Button>
          <Button
            type="button"
            variant="ghost"
            onClick={() => {
              form.reset()
              setPage(1)
              setFiltros(SIN_FILTROS)
            }}
          >
            {t('limpiar')}
          </Button>
        </div>
      </form>

      {consulta.isError ? (
        <ErrorState
          descripcion={
            consulta.error instanceof ApiError ? consulta.error.message : t('errorDeCarga')
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
              emptyMessage={t('vacio')}
              renderActions={(row) => verDetalle(row.original)}
              actionsLabel={t('detalle')}
            />
          </div>
          <div className="md:hidden">
            <DataCards
              table={table}
              isLoading={consulta.isPending}
              isError={false}
              emptyMessage={t('vacio')}
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
        title={detalle?.accion ?? t('evento')}
        subtitle={detalle ? formatDateTime(detalle.fecha) : undefined}
        icon={ScrollTextIcon}
      >
        {detalle && (
          <div className="flex flex-col gap-4 text-sm">
            <dl className="grid gap-3 sm:grid-cols-2">
              <Dato nombre={t('campo.modulo')} valor={detalle.modulo} />
              <Dato nombre={t('campo.resultado')} valor={t(CLAVE_RESULTADO[detalle.resultado])} />
              <Dato nombre={t('campo.usuario')} valor={detalle.usuarioCodigo} />
              <Dato nombre={t('campo.ip')} valor={detalle.ip} />
              <Dato nombre={t('campo.entidad')} valor={detalle.entidad} />
              <Dato nombre={t('campo.codigoEntidad')} valor={detalle.entidadCodigo} />
            </dl>
            {/* * Texto plano: el detalle nunca se interpreta como HTML. */}
            <pre className="overflow-x-auto rounded-md bg-muted p-3 text-xs whitespace-pre-wrap">
              {detalle.detalle ? JSON.stringify(detalle.detalle, null, 2) : t('sinDetalle')}
            </pre>
          </div>
        )}
      </ModalDetailLayout>
    </div>
  )
}
