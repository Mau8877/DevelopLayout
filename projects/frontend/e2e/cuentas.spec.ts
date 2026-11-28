import { expect, test, type Page } from '@playwright/test'
import { crearCuenta, nuevaPassword, nuevoCorreo } from './soporte/api'
import { enlaceDe, esperarCorreo } from './soporte/correo'
import { entorno } from './soporte/entorno'
import { iniciarSesion, llenarLogin, otroNavegador } from './soporte/pagina'

async function definirPassword(page: Page, password: string, boton: string) {
  await page.getByLabel('Contraseña nueva').fill(password)
  await page.getByLabel('Repite la contraseña').fill(password)
  await page.getByRole('button', { name: boton }).click()
}

test('recuperar la contraseña con el enlace del correo, una sola vez', async ({ page, browser }) => {
  const cuenta = await crearCuenta()
  const nueva = nuevaPassword()
  await page.goto('/login')
  await page.getByRole('link', { name: '¿Olvidaste tu contraseña?' }).click()
  await page.getByLabel('Correo', { exact: true }).fill(cuenta.correo)
  await page.getByRole('button', { name: 'Enviar enlace' }).click()
  await expect(page.getByRole('status')).toContainText('Si hay una cuenta con ese correo')

  // * El primer correo de esta cuenta fue la invitación.
  const correo = await esperarCorreo(cuenta.correo, { cantidad: 2 })
  expect(correo.asunto).toBe('Restablece tu contraseña')
  const enlace = enlaceDe(correo.texto)
  await page.goto(enlace)
  await expect(page.getByLabel('Contraseña nueva')).toBeVisible()
  // * El token se saca de la barra de direcciones y del historial.
  await expect(page).toHaveURL(/\/reset-password$/)
  await definirPassword(page, nueva, 'Cambiar contraseña')
  await expect(page).toHaveURL(/\/login$/)

  await llenarLogin(page, cuenta)
  await expect(page.getByText('Correo o contraseña incorrectos')).toBeVisible()
  await llenarLogin(page, { correo: cuenta.correo, password: nueva })
  await expect(page).toHaveURL(/\/dashboard$/)

  // * Tras la invitación y el enlace llega el aviso de que la contraseña cambió.
  const aviso = await esperarCorreo(cuenta.correo, { cantidad: 3 })
  expect(aviso.asunto).toBe('Tu contraseña cambió')
  expect(aviso.texto).not.toContain('http')

  const otro = await otroNavegador(browser)
  await otro.page.goto(enlace)
  await definirPassword(otro.page, nuevaPassword(), 'Cambiar contraseña')
  await expect(otro.page.getByText('El enlace no sirve')).toBeVisible()
  await otro.contexto.close()
})

test('el administrador invita, la persona activa su cuenta y entra; el enlace no se reutiliza', async ({
  page,
  browser,
}) => {
  const correoNuevo = nuevoCorreo()
  const password = nuevaPassword()
  await iniciarSesion(page, entorno.admin)
  await page.goto('/users')
  await page.getByRole('button', { name: 'Crear usuario' }).click()
  const dialogo = page.getByRole('dialog')
  await dialogo.getByLabel('Correo', { exact: true }).fill(correoNuevo)
  await dialogo.getByRole('checkbox', { name: 'Usuario' }).click()
  await dialogo.getByRole('button', { name: 'Crear' }).click()
  await expect(page.getByText('Usuario creado')).toBeVisible()

  const correo = await esperarCorreo(correoNuevo)
  expect(correo.asunto).toBe('Te invitaron a usar el sistema')
  const enlace = enlaceDe(correo.texto)
  const invitada = await otroNavegador(browser)
  await invitada.page.goto(enlace)
  await expect(invitada.page.getByText('Activa tu cuenta')).toBeVisible()
  await definirPassword(invitada.page, password, 'Activar cuenta')
  await expect(invitada.page).toHaveURL(/\/login$/)
  await llenarLogin(invitada.page, { correo: correoNuevo, password })
  await expect(invitada.page).toHaveURL(/\/dashboard$/)
  await invitada.contexto.close()

  const repetida = await otroNavegador(browser)
  await repetida.page.goto(enlace)
  await definirPassword(repetida.page, nuevaPassword(), 'Activar cuenta')
  await expect(repetida.page.getByText('La invitación no sirve')).toBeVisible()
  await repetida.contexto.close()
})
