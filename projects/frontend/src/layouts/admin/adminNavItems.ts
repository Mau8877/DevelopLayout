import {
  LayoutDashboardIcon,
  UsersIcon,
  MonitorSmartphoneIcon,
  ScrollTextIcon,
  ShieldCheckIcon,
} from 'lucide-react'

import type { ElementoDeMenu } from '../navegacion'

export const adminNavItems: ElementoDeMenu[] = [
  { clave: 'navegacion.panel', href: '/dashboard', icon: LayoutDashboardIcon },
  { clave: 'navegacion.misSesiones', href: '/my-sessions', icon: ShieldCheckIcon },
  { clave: 'navegacion.sesionesUsuarios', href: '/user-sessions', icon: MonitorSmartphoneIcon },
  { clave: 'navegacion.bitacora', href: '/audit-log', icon: ScrollTextIcon },
  { clave: 'navegacion.usuarios', href: '/users', icon: UsersIcon },
]
