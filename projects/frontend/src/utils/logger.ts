/**
 * Único punto de log del frontend (ver ERRORES_LOGGING_FRONTEND.md).
 *
 * * En producción `debug` e `info` no emiten nada; `warn` y `error` sí.
 * * El resto del código no usa `console.*` directo (regla ESLint `no-console`).
 * ! Nunca pasar tokens, cookies, contraseñas ni bodies de /auth/** acá.
 */
const isProd = import.meta.env.PROD

/* eslint-disable no-console */
export const logger = {
  debug: (...args: unknown[]) => {
    if (!isProd) console.debug(...args)
  },
  info: (...args: unknown[]) => {
    if (!isProd) console.info(...args)
  },
  warn: (...args: unknown[]) => console.warn(...args),
  error: (...args: unknown[]) => console.error(...args),
  /** Grupo colapsable (solo fuera de producción); lo usa el log HTTP de createBaseApi. */
  group: (title: string, styles: string[], body: unknown) => {
    if (isProd) return
    console.groupCollapsed(title, ...styles)
    console.log(body)
    console.groupEnd()
  },
}
/* eslint-enable no-console */
