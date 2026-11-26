import { AppShell } from '@/components/layout'
import { useLogout } from '@/features/auth'

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

  return (
    <AppShell
      appName="App"
      title={title}
      navItems={usuarioNavItems}
      activeHref={activeHref}
      user={user}
      notifications={notifications}
      onLogout={() => logout.mutate()}
    >
      {children}
    </AppShell>
  )
}
