import { AppShell } from '@/components/layout'
import { useLogout } from '@/features/auth'

import { adminNavItems } from './adminNavItems'
import type { AdminLayoutProps } from './adminTypes'

export function AdminLayout({
  title,
  user,
  notifications,
  activeHref,
  children,
}: AdminLayoutProps) {
  const logout = useLogout()

  return (
    <AppShell
      appName="App Admin"
      title={title}
      navItems={adminNavItems}
      activeHref={activeHref}
      user={user}
      notifications={notifications}
      onLogout={() => logout.mutate()}
    >
      {children}
    </AppShell>
  )
}
