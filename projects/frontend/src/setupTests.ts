import '@testing-library/jest-dom/vitest'
import { beforeEach } from 'vitest'
import { i18n } from '@/shared/language'

// * Las pruebas buscan los textos en español, sin depender del idioma de la máquina.
beforeEach(async () => {
  window.localStorage.removeItem('idioma')
  if (i18n.resolvedLanguage !== 'es') await i18n.changeLanguage('es')
})
