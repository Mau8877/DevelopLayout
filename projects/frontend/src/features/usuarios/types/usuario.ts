import type { components } from '@/shared/api/schema'

/** Cuenta vista por el administrador (contrato OpenAPI). */
export type Usuario = Required<components['schemas']['UsuarioResponse']>

export type EstadoUsuario = Usuario['estado']

export interface UsuarioCreado {
  usuario: Usuario
  /** `false`: la cuenta existe pero el correo no salió. */
  invitacionEnviada: boolean
}
