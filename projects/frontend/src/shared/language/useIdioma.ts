import { useQueryClient } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { guardarPreferencia, IDIOMA_POR_DEFECTO, esIdioma, type CodigoIdioma } from './idiomas'

/** Única vía para leer y cambiar el idioma. */
export function useIdioma() {
  const { i18n } = useTranslation()
  const queryClient = useQueryClient()
  const idioma: CodigoIdioma = esIdioma(i18n.resolvedLanguage) ? i18n.resolvedLanguage : IDIOMA_POR_DEFECTO

  async function cambiarIdioma(nuevo: CodigoIdioma) {
    if (nuevo === idioma) return
    guardarPreferencia(nuevo)
    await i18n.changeLanguage(nuevo)
    // * Los mensajes que vinieron del servidor se piden de nuevo en el idioma elegido.
    await queryClient.invalidateQueries()
  }

  return { idioma, cambiarIdioma }
}
