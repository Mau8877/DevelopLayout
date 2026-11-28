import { expect, test } from '@playwright/test'
import { entorno } from './soporte/entorno'
import { iniciarSesion } from './soporte/pagina'

test('el sistema arranca y el administrador inicial puede entrar', async ({ page }) => {
  const errores: string[] = []
  page.on('pageerror', (error) => errores.push(error.message))

  await iniciarSesion(page, entorno.admin)

  await expect(page.getByRole('link', { name: 'Usuarios' }).first()).toBeVisible()
  expect(errores).toEqual([])
})
