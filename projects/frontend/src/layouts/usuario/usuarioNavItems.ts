import { HomeIcon, ShieldCheckIcon } from 'lucide-react'

import type { ElementoDeMenu } from '../navegacion'

export const usuarioNavItems: ElementoDeMenu[] = [
  { clave: 'navegacion.inicio', href: '/', icon: HomeIcon },
  { clave: 'navegacion.misSesiones', href: '/my-sessions', icon: ShieldCheckIcon },
]
