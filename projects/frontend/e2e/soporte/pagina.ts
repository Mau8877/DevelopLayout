import { expect, type Browser, type Page } from '@playwright/test'
import type { Cuenta } from './api'

export async function llenarLogin(page: Page, { correo, password }: Cuenta) {
  await page.getByLabel('Correo', { exact: true }).fill(correo)
  await page.getByLabel('Contraseña', { exact: true }).fill(password)
  await page.getByRole('button', { name: 'Iniciar sesión' }).click()
}

export async function iniciarSesion(page: Page, cuenta: Cuenta) {
  await page.goto('/login')
  await llenarLogin(page, cuenta)
  await expect(page).toHaveURL(/\/dashboard$/)
}

/** Otro navegador, con sus propias cookies. */
export async function otroNavegador(browser: Browser, cuenta?: Cuenta) {
  const contexto = await browser.newContext({ locale: 'es-ES' })
  const page = await contexto.newPage()
  if (cuenta) await iniciarSesion(page, cuenta)
  return { contexto, page }
}

export async function cerrarSesionDesdeElMenu(page: Page, correo: string) {
  await page.getByRole('banner').getByRole('button', { name: correo }).click()
  await page.getByRole('menuitem', { name: 'Cerrar sesión' }).click()
  await expect(page).toHaveURL(/\/login$/)
}

/** Al volver a pedir una pantalla privada, una sesión cerrada termina en el login. */
export async function esperarSesionCerrada(page: Page) {
  await page.goto('/dashboard')
  await expect(page).toHaveURL(/\/login$/)
}

/** Confirma en el diálogo abierto. */
export async function confirmar(page: Page, boton: string) {
  await page.getByRole('alertdialog').getByRole('button', { name: boton, exact: true }).click()
}
