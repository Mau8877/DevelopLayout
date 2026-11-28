import { expect, test, type BrowserContext } from '@playwright/test'
import { crearCuenta, nuevaPassword } from './soporte/api'
import { entorno } from './soporte/entorno'
import {
  cerrarSesionDesdeElMenu,
  confirmar,
  esperarSesionCerrada,
  iniciarSesion,
  llenarLogin,
  otroNavegador,
} from './soporte/pagina'

const cookie = async (contexto: BrowserContext, nombre: string) =>
  (await contexto.cookies()).find((c) => c.name === nombre)

test('con credenciales incorrectas no entra y muestra el mensaje del servidor', async ({ page }) => {
  const cuenta = await crearCuenta()
  await page.goto('/login')

  await llenarLogin(page, { correo: cuenta.correo, password: nuevaPassword() })

  await expect(page.getByText('Correo o contraseña incorrectos')).toBeVisible()
  await expect(page).toHaveURL(/\/login$/)
})

test('sin sesión, una ruta protegida lleva al login', async ({ page }) => {
  await page.goto('/my-sessions')

  await expect(page).toHaveURL(/\/login$/)
})

test('los tokens viven solo en cookies que JavaScript no puede leer', async ({ page, context }) => {
  await iniciarSesion(page, await crearCuenta())

  for (const nombre of ['accessToken', 'refreshToken']) {
    expect(await cookie(context, nombre), nombre).toMatchObject({ httpOnly: true, sameSite: 'Strict' })
  }
  const legible = await page.evaluate(() => ({
    cookies: document.cookie,
    local: JSON.stringify(window.localStorage),
    sesion: JSON.stringify(window.sessionStorage),
  }))
  const todo = `${legible.cookies} ${legible.local} ${legible.sesion}`
  expect(todo).not.toContain('accessToken')
  expect(todo).not.toContain('refreshToken')
  for (const nombre of ['accessToken', 'refreshToken']) {
    expect(todo).not.toContain((await cookie(context, nombre))!.value)
  }
})

test('al recargar conserva la sesión', async ({ page }) => {
  await iniciarSesion(page, await crearCuenta())

  await page.reload()

  await expect(page).toHaveURL(/\/dashboard$/)
  await expect(page.getByText('Todavía no hay contenido en el panel.')).toBeVisible()
})

test('cuando vence el access token, la sesión se renueva sola', async ({ page, context }) => {
  await iniciarSesion(page, await crearCuenta())
  const refreshAnterior = (await cookie(context, 'refreshToken'))!.value
  // * El entorno de prueba usa un access token de 20 s: el navegador descarta la cookie al vencer.
  await expect.poll(() => cookie(context, 'accessToken'), { timeout: 45_000 }).toBeUndefined()

  const renovacion = page.waitForResponse((r) => r.url().endsWith('/auth/refresh') && r.ok())
  await page.getByRole('link', { name: 'Mis sesiones' }).first().click()
  await renovacion

  await expect(page).toHaveURL(/\/my-sessions$/)
  await expect(page.getByText('Esta sesión')).toBeVisible()
  expect(await cookie(context, 'accessToken')).toBeDefined()
  expect((await cookie(context, 'refreshToken'))!.value).not.toBe(refreshAnterior)
})

test('cerrar sesión borra las cookies y deja las rutas protegidas fuera de alcance', async ({ page, context }) => {
  const cuenta = await crearCuenta()
  await iniciarSesion(page, cuenta)

  await cerrarSesionDesdeElMenu(page, cuenta.correo)

  expect(await cookie(context, 'accessToken')).toBeUndefined()
  expect(await cookie(context, 'refreshToken')).toBeUndefined()
  await esperarSesionCerrada(page)
})

test('desde «Mis sesiones» se cierra el otro dispositivo, que pierde el acceso', async ({ page, browser }) => {
  const cuenta = await crearCuenta()
  // * La preparación de la cuenta no abre sesiones: quedan solo estas dos.
  const otro = await otroNavegador(browser, cuenta)
  await iniciarSesion(page, cuenta)
  await page.goto('/my-sessions')

  await page.getByRole('button', { name: 'Cerrar esta otra sesión' }).click()
  await confirmar(page, 'Cerrar')

  await expect(page.getByText('Sesión cerrada')).toBeVisible()
  await expect(page.getByRole('button', { name: 'Cerrar esta otra sesión' })).toHaveCount(0)
  await esperarSesionCerrada(otro.page)
  await page.reload()
  await expect(page).toHaveURL(/\/my-sessions$/)
  await otro.contexto.close()
})

test('el administrador cierra las sesiones de otra persona', async ({ page, browser }) => {
  const cuenta = await crearCuenta()
  const afectada = await otroNavegador(browser, cuenta)
  await iniciarSesion(page, entorno.admin)
  await page.goto('/user-sessions')

  await page.getByLabel('Correo del usuario').fill(cuenta.correo)
  await page.getByRole('button', { name: 'Buscar' }).click()
  await page.getByRole('button', { name: 'Cerrar todas' }).click()
  await confirmar(page, 'Cerrar')

  await expect(page.getByText('Sesiones cerradas')).toBeVisible()
  await expect(page.getByText('No hay sesiones activas para ese correo')).toBeVisible()
  await esperarSesionCerrada(afectada.page)
  await afectada.contexto.close()
})
