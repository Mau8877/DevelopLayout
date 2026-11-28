import { useEffect, useState } from 'react'
import { useTranslation } from 'react-i18next'

import { dayjs } from '@/utils'

interface HeaderClockProps {
  className?: string
}

// * Cada 30s, no cada segundo -- el formato no muestra segundos, así que
// * actualizar más seguido solo gastaría ciclos sin cambiar lo que se ve.
const UPDATE_INTERVAL_MS = 30_000

/** Fecha y hora actuales en el idioma vigente; se refresca sola. */
export function HeaderClock({ className }: HeaderClockProps) {
  const { t } = useTranslation('shared')
  const [ahora, setAhora] = useState(() => Date.now())

  useEffect(() => {
    const id = window.setInterval(() => setAhora(Date.now()), UPDATE_INTERVAL_MS)
    return () => window.clearInterval(id)
  }, [])

  // * Se formatea al pintar: al cambiar de idioma cambia al instante.
  return <span className={className}>{dayjs(ahora).format(t('cabecera.formatoFecha'))}</span>
}
