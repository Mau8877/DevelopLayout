import { i18n } from '@/shared/language'

/** Los roles que existen; el sistema no permite crear otros. Guardan claves, no textos. */
const ROLES = [
  { value: 'SUPER_ADMIN', clave: 'superAdmin' },
  { value: 'USUARIO', clave: 'usuario' },
] as const

/** Las opciones de rol en el idioma vigente. Se llama al pintar. */
export function opcionesDeRol(): { value: string; label: string; descripcion: string }[] {
  return ROLES.map(({ value, clave }) => ({
    value,
    label: i18n.t(`usuarios:rol.${clave}.nombre`),
    descripcion: i18n.t(`usuarios:rol.${clave}.descripcion`),
  }))
}

export function etiquetaDeRol(rol: string): string {
  return opcionesDeRol().find((r) => r.value === rol)?.label ?? rol
}
