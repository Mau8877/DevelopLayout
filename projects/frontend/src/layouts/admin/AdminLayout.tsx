import { useNavigate } from '@tanstack/react-router'
import { AppShell } from '@/components/layout'
import { useLogout } from '@/features/auth'
import { APP_NAME_ADMIN } from '@/utils'
import { useMenuTraducido } from '../navegacion'

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
  const navigate = useNavigate()
  const menu = useMenuTraducido(adminNavItems)

  return (
    <AppShell
      appName={APP_NAME_ADMIN}
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
