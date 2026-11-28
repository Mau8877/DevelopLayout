import { CheckIcon, LanguagesIcon } from 'lucide-react'
import { useTranslation } from 'react-i18next'
import { buttonVariants } from '@/components/ui/button'
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu'
import { cn } from '@/lib/utils'
import { IDIOMAS, useIdioma } from '@/shared/language'

export interface SelectorIdiomaProps {
  className?: string
  /** En pantallas angostas muestra solo el ícono (para barras con poco ancho). */
  compacto?: boolean
}

/** Cambia el idioma sin recargar. Va también en las pantallas sin sesión. */
export function SelectorIdioma({ className, compacto = false }: SelectorIdiomaProps) {
  const { t } = useTranslation('shared')
  const { idioma, cambiarIdioma } = useIdioma()
  const actual = IDIOMAS.find((i) => i.codigo === idioma)

  return (
    <DropdownMenu>
      <DropdownMenuTrigger
        aria-label={`${t('idioma.cambiar')}: ${actual?.nombre}`}
        className={cn(buttonVariants({ variant: 'ghost', size: 'sm' }), className)}
      >
        <LanguagesIcon data-icon="inline-start" />
        <span className={cn(compacto && 'max-sm:hidden')}>{idioma.toUpperCase()}</span>
      </DropdownMenuTrigger>
      <DropdownMenuContent align="end">
        {IDIOMAS.map((opcion) => (
          <DropdownMenuItem
            key={opcion.codigo}
            lang={opcion.codigo}
            aria-current={opcion.codigo === idioma ? 'true' : undefined}
            onClick={() => void cambiarIdioma(opcion.codigo)}
          >
            <CheckIcon className={cn(opcion.codigo !== idioma && 'invisible')} />
            {opcion.nombre}
          </DropdownMenuItem>
        ))}
      </DropdownMenuContent>
    </DropdownMenu>
  )
}
