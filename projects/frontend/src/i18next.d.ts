// * Solo tipos: una clave mal escrita rompe `pnpm build`.
// * Toda carpeta `language/` se declara acá (lo comprueba una prueba).
import 'i18next'
import type auth from './features/auth/language/es.json'
import type bitacora from './features/bitacora/language/es.json'
import type usuarios from './features/usuarios/language/es.json'
import type shared from './shared/language/es.json'

declare module 'i18next' {
  interface CustomTypeOptions {
    defaultNS: 'shared'
    resources: {
      auth: typeof auth
      bitacora: typeof bitacora
      shared: typeof shared
      usuarios: typeof usuarios
    }
  }
}
