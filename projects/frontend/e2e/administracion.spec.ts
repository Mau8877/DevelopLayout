import { expect, test, type Page } from '@playwright/test'
import { crearCuenta, llamar, tokenDe } from './soporte/api'
import { entorno } from './soporte/entorno'
import { confirmar, esperarSesionCerrada, iniciarSesion, llenarLogin, otroNavegador } from './soporte/pagina'

/** Busca una cuenta en «Usuarios» y devuelve su fila. */
async function buscarUsuario(page: Page, correo: string) {
  await page.goto('/users')
  await page.getByPlaceholder('Buscar por correo').fill(correo)
  const fila = page.getByRole('row').filter({ hasText: correo })
  await expect(fila).toBeVisible()
  // * La lista queda filtrada: solo el encabezado y esa fila.
  await expect(page.getByRole('row')).toHaveCount(2)
  return fila
}

async function cambiarRoles(page: Page, rol: string) {
  const dialogo = page.getByRole('dialog')
  await dialogo.getByRole('checkbox', { name: rol }).click()
  await dialogo.getByRole('button', { name: 'Guardar' }).click()
  await confirmar(page, 'Cambiar roles')
}

test('desactivar corta el acceso y reactivar lo devuelve', async ({ page, browser }) => {
  const cuenta = await crearCuenta()
  const afectada = await otroNavegador(browser, cuenta)
  await iniciarSesion(page, entorno.admin)
  const fila = await buscarUsuario(page, cuenta.correo)

  await fila.getByRole('button', { name: 'Desactivar' }).click()
  await confirmar(page, 'Desactivar')

  await expect(page.getByText('Usuario desactivado')).toBeVisible()
  await expect(fila).toContainText('Inactiva')
  await esperarSesionCerrada(afectada.page)
  await llenarLogin(afectada.page, cuenta)
  await expect(afectada.page.getByRole('alert')).toBeVisible()
  await expect(afectada.page).toHaveURL(/\/login$/)

  await fila.getByRole('button', { name: 'Reactivar' }).click()
  await expect(page.getByText('Usuario reactivado')).toBeVisible()
  await expect(fila).toContainText('Activa')
  await llenarLogin(afectada.page, cuenta)
  await expect(afectada.page).toHaveURL(/\/dashboard$/)
  await afectada.contexto.close()
})

test('cambiar los roles de otra persona le cierra la sesión y rige al volver a entrar', async ({ page, browser }) => {
  const cuenta = await crearCuenta()
  const afectada = await otroNavegador(browser, cuenta)
  await expect(afectada.page.getByRole('link', { name: 'Usuarios' })).toHaveCount(0)
  await iniciarSesion(page, entorno.admin)
  const fila = await buscarUsuario(page, cuenta.correo)

  await fila.getByRole('button', { name: 'Cambiar roles' }).click()
  await cambiarRoles(page, 'Administrador')

  await expect(page.getByText('Roles actualizados')).toBeVisible()
  await expect(fila).toContainText('Administrador')
  await esperarSesionCerrada(afectada.page)
  await llenarLogin(afectada.page, cuenta)
  await expect(afectada.page.getByRole('link', { name: 'Usuarios' }).first()).toBeVisible()
  await afectada.contexto.close()
})

test('al cambiar sus propios roles, la sesión del administrador termina', async ({ page }) => {
  const cuenta = await crearCuenta(['SUPER_ADMIN'])
  await iniciarSesion(page, cuenta)
  const fila = await buscarUsuario(page, cuenta.correo)

  await fila.getByRole('button', { name: 'Cambiar roles' }).click()
  await expect(page.getByRole('checkbox', { name: 'Administrador' })).toBeDisabled()
  await cambiarRoles(page, 'Usuario')

  await expect(page).toHaveURL(/\/login$/)
  await esperarSesionCerrada(page)
})

test('sin rol de administrador no se llega a sus pantallas ni a sus datos', async ({ page }) => {
  const cuenta = await crearCuenta()
  await iniciarSesion(page, cuenta)

  for (const ruta of ['/users', '/audit-log', '/user-sessions']) {
    await page.goto(ruta)
    await expect(page, ruta).toHaveURL(/\/dashboard$/)
  }
  for (const enlace of ['Usuarios', 'Bitácora', 'Sesiones de usuarios']) {
    await expect(page.getByRole('link', { name: enlace })).toHaveCount(0)
  }
  // * La autorización real es del backend.
  const token = await tokenDe(cuenta)
  const lista = await llamar('/users/search', { metodo: 'POST', token, body: { page: 1, pageSize: 20 } })
  expect(lista.status).toBe(403)
  expect((await llamar('/audit-log', { token })).status).toBe(403)
})

test('la bitácora muestra lo que se hizo', async ({ page }) => {
  await crearCuenta()
  await iniciarSesion(page, entorno.admin)
  await page.goto('/audit-log')

  await page.getByLabel('Acción').fill('USUARIOS_USUARIO_CREADO')
  await page.getByRole('button', { name: 'Filtrar' }).click()

  await expect(page.getByRole('cell', { name: 'USUARIOS_USUARIO_CREADO' }).first()).toBeVisible()
})
