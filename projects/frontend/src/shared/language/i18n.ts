import i18n from 'i18next'
import { initReactI18next } from 'react-i18next'
import { detectarIdioma, IDIOMA_POR_DEFECTO, IDIOMAS, type CodigoIdioma } from './idiomas'

type Textos = Record<string, unknown>

// * Se DESCUBREN los archivos: así `shared/` no importa código de ninguna feature.
const archivos = import.meta.glob<Textos>('/src/**/language/{es,en}.json', { eager: true, import: 'default' })

/** El espacio de nombres es la carpeta dueña: `features/auth/language` -> `auth`. */
function recursos(): Record<CodigoIdioma, Record<string, Textos>> {
  const resultado: Record<CodigoIdioma, Record<string, Textos>> = { es: {}, en: {} }
  for (const [ruta, textos] of Object.entries(archivos)) {
    const [, duena, idioma] = /\/([^/]+)\/language\/(es|en)\.json$/.exec(ruta) ?? []
    if (duena) resultado[idioma as CodigoIdioma][duena] = textos
  }
  return resultado
}

function reflejarEnElDocumento(idioma: string) {
  document.documentElement.lang = idioma
}

i18n.on('languageChanged', reflejarEnElDocumento)

void i18n.use(initReactI18next).init({
  resources: recursos(),
  lng: detectarIdioma(),
  fallbackLng: IDIOMA_POR_DEFECTO,
  supportedLngs: IDIOMAS.map((idioma) => idioma.codigo),
  defaultNS: 'shared',
  // * Los textos están en el paquete: se inicia de forma síncrona, antes del primer render.
  initAsync: false,
  // * React ya escapa lo que muestra. ! Nunca usar una traducción con dangerouslySetInnerHTML.
  interpolation: { escapeValue: false },
})

export { i18n }
