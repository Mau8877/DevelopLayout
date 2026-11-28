import { useTranslation } from 'react-i18next'
import type { NavItem } from '@/components/layout'

type ClaveDeMenu =
  | 'navegacion.panel'
  | 'navegacion.inicio'
  | 'navegacion.misSesiones'
  | 'navegacion.sesionesUsuarios'
  | 'navegacion.bitacora'
  | 'navegacion.usuarios'

/** Un enlace del menú: guarda la CLAVE de su etiqueta, nunca el texto. */
export type ElementoDeMenu = Omit<NavItem, 'label'> & { clave: ClaveDeMenu }

export function useMenuTraducido(elementos: ElementoDeMenu[]): NavItem[] {
  const { t } = useTranslation('shared')
  return elementos.map(({ clave, ...resto }) => ({ ...resto, label: t(clave) }))
}
