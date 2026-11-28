import { expect, test, type Page } from '@playwright/test'
import { crearCuenta, nuevaPassword } from './soporte/api'
import { enlaceDe, esperarCorreo } from './soporte/correo'
import { iniciarSesion } from './soporte/pagina'

async function elegirIdioma(page: Page, boton: RegExp, idioma: 'Español' | 'English') {
  await page.getByRole('button', { name: boton }).click()
  await page.getByRole('menuitem', { name: idioma }).click()
}

const DETECCION = [
  { navegador: 'en-US', idioma: 'en', titulo: 'Sign in with your email and password' },
  { navegador: 'es-AR', idioma: 'es', titulo: 'Ingresa con tu correo y contraseña' },
  { navegador: 'fr-FR', idioma: 'es', titulo: 'Ingresa con tu correo y contraseña' },
]

for (const { navegador, idioma, titulo } of DETECCION) {
  test.describe(`navegador en ${navegador}`, () => {
    test.use({ locale: navegador })

    test(`se muestra en ${idioma}`, async ({ page }) => {
      await page.goto('/login')

      await expect(page.getByText(titulo)).toBeVisible()
      await expect(page.locator('html')).toHaveAttribute('lang', idioma)
    })
  })
}

test('el selector cambia el idioma antes y después de entrar, y se recuerda', async ({ page }) => {
  const cuenta = await crearCuenta()
  await page.goto('/login')

  await elegirIdioma(page, /^Cambiar idioma/, 'English')
  await expect(page.getByText('Sign in with your email and password')).toBeVisible()
  await page.reload()
  await expect(page.getByText('Sign in with your email and password')).toBeVisible()

  await page.getByLabel('Email', { exact: true }).fill(cuenta.correo)
  await page.getByLabel('Password', { exact: true }).fill(cuenta.password)
  await page.getByRole('button', { name: 'Sign in' }).click()
  await expect(page.getByText('There is no content on the dashboard yet.')).toBeVisible()
  await expect(page.getByRole('link', { name: 'My sessions' }).first()).toBeVisible()

  await elegirIdioma(page, /^Change language/, 'Español')
  await expect(page.getByText('Todavía no hay contenido en el panel.')).toBeVisible()
  await page.reload()
  await expect(page.getByText('Todavía no hay contenido en el panel.')).toBeVisible()
})

test('el error del servidor y los de validación salen en el idioma elegido', async ({ page }) => {
  await page.goto('/login')
  await page.getByRole('button', { name: 'Iniciar sesión' }).click()
  await expect(page.getByText('El correo es obligatorio')).toBeVisible()

  await elegirIdioma(page, /^Cambiar idioma/, 'English')
  // * Un error ya visible cambia con la pantalla.
  await expect(page.getByText('Email is required')).toBeVisible()

  await page.getByLabel('Email', { exact: true }).fill('nadie@e2e.test')
  await page.getByLabel('Password', { exact: true }).fill(nuevaPassword())
  await page.getByRole('button', { name: 'Sign in' }).click()
  await expect(page.getByText('Incorrect email or password')).toBeVisible()
})

test.describe('con el navegador en inglés', () => {
  test.use({ locale: 'en-US' })

  test('el correo de recuperación llega en inglés y su enlace abre la página en inglés', async ({ page }) => {
    const cuenta = await crearCuenta()
    await page.goto('/forgot-password')
    await page.getByLabel('Email', { exact: true }).fill(cuenta.correo)
    await page.getByRole('button', { name: 'Send link' }).click()
    await expect(page.getByRole('status')).toContainText('If there is an account with that email')

    const correo = await esperarCorreo(cuenta.correo, { cantidad: 2 })

    expect(correo.asunto).toBe('Reset your password')
    await page.goto(enlaceDe(correo.texto))
    await expect(page.getByText('Choose a new password')).toBeVisible()
  })
})

test('las fechas siguen el idioma', async ({ page }) => {
  await iniciarSesion(page, await crearCuenta())
  await page.goto('/my-sessions')
  const fila = page.getByRole('row').filter({ hasText: 'Esta sesión' })
  await expect(fila).toBeVisible()
  await expect(fila).not.toContainText(/\b(AM|PM)\b/)

  await elegirIdioma(page, /^Cambiar idioma/, 'English')

  const enIngles = page.getByRole('row').filter({ hasText: 'This session' })
  await expect(enIngles).toContainText(/\d{1,2}\/\d{1,2}\/\d{2}, \d{1,2}:\d{2}:\d{2}\s(AM|PM)/)
})
