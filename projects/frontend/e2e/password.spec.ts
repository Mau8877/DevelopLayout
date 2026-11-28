import { expect, test } from '@playwright/test'
import { crearCuenta, nuevaPassword } from './soporte/api'
import { esperarCorreo } from './soporte/correo'
import {
  abrirCambioDePassword,
  esperarSesionCerrada,
  iniciarSesion,
  llenarCambioDePassword,
  llenarLogin,
  otroNavegador,
} from './soporte/pagina'

test('cambiar la contraseña cierra todas las sesiones y avisa por correo', async ({ page, context, browser }) => {
  const cuenta = await crearCuenta()
  const nueva = nuevaPassword()
  const otro = await otroNavegador(browser, cuenta)
  await iniciarSesion(page, cuenta)
  await abrirCambioDePassword(page, cuenta.correo)

  await llenarCambioDePassword(page, cuenta.password, nueva)

  await expect(page).toHaveURL(/\/login$/)
  await expect(page.getByText('Contraseña cambiada')).toBeVisible()
  const cookies = (await context.cookies()).map((c) => c.name)
  expect(cookies).not.toContain('accessToken')
  expect(cookies).not.toContain('refreshToken')
  // * Volver atrás no devuelve al formulario con lo escrito.
  await page.goBack()
  await expect(page).toHaveURL(/\/login$/)
  await expect(page.getByLabel('Contraseña actual')).toHaveCount(0)
  await esperarSesionCerrada(page)
  await esperarSesionCerrada(otro.page)

  await llenarLogin(page, cuenta)
  await expect(page.getByText('Correo o contraseña incorrectos')).toBeVisible()
  await llenarLogin(page, { correo: cuenta.correo, password: nueva })
  await expect(page).toHaveURL(/\/dashboard$/)

  // * El primer correo de esta cuenta fue la invitación.
  const aviso = await esperarCorreo(cuenta.correo, { cantidad: 2 })
  expect(aviso.asunto).toBe('Tu contraseña cambió')
  expect(aviso.texto).not.toContain('http')
  expect(aviso.texto).not.toContain(nueva)
  await otro.contexto.close()
})

test('con la contraseña actual incorrecta no cambia nada y la sesión sigue', async ({ page }) => {
  const cuenta = await crearCuenta()
  await iniciarSesion(page, cuenta)
  await abrirCambioDePassword(page, cuenta.correo)

  await llenarCambioDePassword(page, nuevaPassword(), nuevaPassword())

  await expect(page.getByText('La contraseña actual no es correcta')).toBeVisible()
  await expect(page).toHaveURL(/\/change-password$/)
  await page.reload()
  await expect(page).toHaveURL(/\/change-password$/)
  await expect(page.getByLabel('Contraseña actual')).toHaveValue('')
})
