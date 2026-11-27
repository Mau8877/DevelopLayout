import type { ReactNode } from 'react'
import type { UsuarioSesion } from '@/features/auth'
import { AdminLayout } from './admin'
import { UsuarioLayout } from './usuario'

interface LayoutPorRolProps {
  usuario: UsuarioSesion
  title: string
  children: ReactNode
}

/**
 * Layout para las pantallas COMUNES a todos los roles. Es solo experiencia de
 * usuario: la autorización es del backend. Una pantalla de un solo rol va bajo
 * el layout de ese rol en las rutas, no acá.
 */
export function LayoutPorRol({ usuario, title, children }: LayoutPorRolProps) {
  const esAdmin = usuario.roles.includes('SUPER_ADMIN')
  const Layout = esAdmin ? AdminLayout : UsuarioLayout

  return (
    <Layout title={title} user={{ nombre: usuario.email, rol: esAdmin ? 'Administrador' : 'Usuario' }}>
      {children}
    </Layout>
  )
}
