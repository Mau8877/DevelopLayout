export const authKeys = {
  all: ['auth'] as const,
  session: ['auth', 'session'] as const,
  sesionesAdmin: ['auth', 'admin', 'sesiones', 'list'] as const,
  sesionesAdminList: (params: { busqueda: number; page: number; pageSize: number }) =>
    ['auth', 'admin', 'sesiones', 'list', params] as const,
  misSesiones: ['auth', 'mis-sesiones', 'list'] as const,
  misSesionesList: (params: { page: number; pageSize: number }) =>
    ['auth', 'mis-sesiones', 'list', params] as const,
}
