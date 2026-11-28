import type { components } from '@/shared/api'

type Esquema = components['schemas']['BitacoraResponse']
type Opcionales = 'usuarioCodigo' | 'entidad' | 'entidadCodigo' | 'ip' | 'detalle'

/** Evento de auditoría (contrato OpenAPI). */
export type EventoBitacora = Required<Omit<Esquema, Opcionales>> & Pick<Esquema, Opcionales>
