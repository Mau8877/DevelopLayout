/** Lo que entrega scripts/e2e.mjs. Nada de esto sale de un archivo del proyecto. */
function exigir(nombre: string): string {
  const valor = process.env[nombre]
  if (!valor) throw new Error(`Falta ${nombre}: las pruebas se corren con «pnpm test:e2e»`)
  return valor
}

export const entorno = {
  get api() {
    return exigir('E2E_API_URL')
  },
  get mailpit() {
    return exigir('E2E_MAILPIT_URL')
  },
  get admin() {
    return { correo: exigir('E2E_ADMIN_EMAIL'), password: exigir('E2E_ADMIN_PASSWORD') }
  },
}
