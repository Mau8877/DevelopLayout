import js from '@eslint/js'
import i18next from 'eslint-plugin-i18next'
import globals from 'globals'
import reactHooks from 'eslint-plugin-react-hooks'
import reactRefresh from 'eslint-plugin-react-refresh'
import tseslint from 'typescript-eslint'
import { defineConfig, globalIgnores } from 'eslint/config'

export default defineConfig([
  globalIgnores(['dist', 'src/shared/api/schema.d.ts']),
  {
    files: ['**/*.{ts,tsx}'],
    extends: [
      js.configs.recommended,
      tseslint.configs.recommended,
      reactHooks.configs.flat.recommended,
      reactRefresh.configs.vite,
    ],
    languageOptions: {
      globals: globals.browser,
    },
    rules: {
      // Único punto de log: utils/logger.ts (ver ERRORES_LOGGING_FRONTEND.md).
      'no-console': 'error',
    },
  },
  {
    // Los archivos de src/routes/ exportan `Route` (config de TanStack
    // Router) además del componente de la página -- es el patrón esperado
    // del file-based routing, no un archivo mal armado. React Fast Refresh
    // igual funciona bien acá porque TanStack Router maneja su propio HMR.
    files: ['src/routes/**/*.tsx'],
    rules: {
      'react-refresh/only-export-components': 'off',
    },
  },
  {
    // src/components/ui/ es output del CLI de shadcn (ver
    // .claude/rules/frontend/TAILWIND_STYLES_FRONTEND.md -- no se edita a
    // mano). Varios de esos componentes exportan también un helper de
    // variantes (ej. `buttonVariants`) junto al componente, a propósito.
    files: ['src/components/ui/**/*.tsx'],
    rules: {
      'react-refresh/only-export-components': 'off',
    },
  },
  {
    // Sin texto fijo en pantalla: todo sale de los archivos de idioma (IDIOMAS_FRONTEND.md).
    // Lo que esta regla no ve (avisos, mensajes de Zod, voseo) lo cubre sinTextoFijo.test.ts.
    files: ['src/**/*.tsx'],
    ignores: ['src/**/*.test.tsx', 'src/**/tests/**'],
    plugins: { i18next },
    rules: {
      'i18next/no-literal-string': [
        'error',
        {
          mode: 'jsx-only',
          'jsx-attributes': {
            include: [
              'aria-label', 'aria-description', 'placeholder', 'title', 'alt', 'label', 'legend', 'description',
              'subtitle', 'titulo', 'descripcion', 'confirmLabel', 'cancelLabel', 'closeLabel', 'actionLabel',
              'emptyMessage', 'actionsLabel', 'searchPlaceholder', 'textoBoton', 'mensajeGenerico', 'appName',
            ],
          },
          words: { exclude: ['[0-9!-/:-@[-`{-~—…·•]+', '[A-Z_-]+'] },
          'jsx-components': { exclude: ['Trans'] },
        },
      ],
    },
  },
])
