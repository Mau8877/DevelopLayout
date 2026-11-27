/** Los roles que existen; el sistema no permite crear otros. */
export const ROLES = [
  { value: 'SUPER_ADMIN', label: 'Administrador', descripcion: 'Control total del sistema' },
  { value: 'USUARIO', label: 'Usuario', descripcion: 'Acceso estándar' },
]

export function etiquetaDeRol(rol: string): string {
  return ROLES.find((r) => r.value === rol)?.label ?? rol
}
