/** Usuario de la sesión actual (respuesta de GET /auth/me y de POST /auth/login). */
export interface UsuarioSesion {
  codigo: string
  email: string
  roles: string[]
}
