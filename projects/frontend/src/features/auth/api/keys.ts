export const authKeys = {
  all: ['auth'] as const,
  session: ['auth', 'session'] as const,
  sesionesAdmin: ['auth', 'admin', 'sesiones', 'list'] as const,
  sesionesAdminList: (params: { busqueda: number; page: number; pageSize: number }) =>
    ['auth', 'admin', 'sesiones', 'list', params] as const,
}
