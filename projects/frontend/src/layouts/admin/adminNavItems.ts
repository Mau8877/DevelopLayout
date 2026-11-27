import {
  LayoutDashboardIcon,
  UsersIcon,
  ClipboardListIcon,
  MonitorSmartphoneIcon,
  ScrollTextIcon,
  ShieldCheckIcon,
} from 'lucide-react'

import type { NavItem } from '@/components/layout'

export const adminNavItems: NavItem[] = [
  { label: 'Dashboard', href: '/panel', icon: LayoutDashboardIcon },
  { label: 'Mis sesiones', href: '/mis-sesiones', icon: ShieldCheckIcon },
  { label: 'Sesiones de usuarios', href: '/sesiones-usuarios', icon: MonitorSmartphoneIcon },
  { label: 'Bitácora', href: '/bitacora', icon: ScrollTextIcon },
  { label: 'Usuarios', href: '/usuarios', icon: UsersIcon },
  { label: 'Pedidos', href: '/pedidos', icon: ClipboardListIcon },
]
