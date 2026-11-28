import dayjs from 'dayjs'
import relativeTime from 'dayjs/plugin/relativeTime'
import 'dayjs/locale/es'
import { i18n } from '@/shared/language'

// * Un solo lugar configura dayjs: el plugin de fechas relativas y el idioma, que
// * sigue al de la pantalla. Quien necesite dayjs lo importa desde `@/utils`.
dayjs.extend(relativeTime)

const seguirIdioma = (idioma: string | undefined) => dayjs.locale(idioma === 'en' ? 'en' : 'es')

seguirIdioma(i18n.resolvedLanguage)
i18n.on('languageChanged', seguirIdioma)

export { dayjs }
