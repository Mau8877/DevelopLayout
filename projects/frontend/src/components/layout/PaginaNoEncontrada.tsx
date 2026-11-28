import { useTranslation } from 'react-i18next'
import { ErrorState } from '@/components/ui/error-state'

export function PaginaNoEncontrada() {
  const { t } = useTranslation('shared')
  return <ErrorState titulo={t('error.noEncontradaTitulo')} descripcion={t('error.noEncontradaTexto')} />
}
