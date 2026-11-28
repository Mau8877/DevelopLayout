import { useNavigate } from '@tanstack/react-router'
import { AppShell } from '@/components/layout'
import { useLogout } from '@/features/auth'
import { APP_NAME } from '@/utils'
import { useMenuTraducido } from '../navegacion'

import { usuarioNavItems } from './usuarioNavItems'
import type { UsuarioLayoutProps } from './usuarioTypes'

export function UsuarioLayout({
  title,
  user,
  notifications,
  activeHref,
  children,
}: UsuarioLayoutProps) {
  const logout = useLogout()
  const navigate = useNavigate()
  const menu = useMenuTraducido(usuarioNavItems)

  return (
    <AppShell
      appName={APP_NAME}
      title={title}
      navItems={menu}
      activeHref={activeHref}
      user={user}
      notifications={notifications}
      onLogout={() => logout.mutate()}
      onChangePassword={() => void navigate({ to: '/change-password' })}
    >
      {children}
    </AppShell>
  )
}
