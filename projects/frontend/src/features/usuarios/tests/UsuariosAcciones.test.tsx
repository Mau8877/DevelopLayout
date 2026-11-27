import { screen, waitFor, within } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { toastError, toastSuccess, toastWarning } from '@/components/ui/toast'
import { apiClient, ApiError } from '@/store'
import { UsuariosScreen } from '../screens/UsuariosScreen'
import type { Usuario } from '../types/usuario'
import { montar, pagina, usuario } from './soporte'

vi.mock('@/components/ui/toast', () => ({
  toastSuccess: vi.fn(),
  toastError: vi.fn(),
  toastWarning: vi.fn(),
}))

/** El listado responde siempre con `filas`; el resto de los POST, con `accion`. */
function servidor(filas: Usuario[], accion: () => Promise<unknown> = () => Promise.resolve({ data: null })) {
  return vi
    .spyOn(apiClient, 'post')
    .mockImplementation((url) => (url === '/usuarios/buscar' ? Promise.resolve(pagina(filas)) : accion()) as never)
}

async function enLaTabla(filas: Usuario[], accion?: () => Promise<unknown>) {
  const post = servidor(filas, accion)
  const montado = montar(<UsuariosScreen />)
  await waitFor(() => expect(screen.getByRole('table')).toHaveTextContent(filas[0].email))
  return { post, ...montado, tabla: within(screen.getByRole('table')) }
}

const acciones = (post: ReturnType<typeof servidor>) => post.mock.calls.filter(([url]) => url !== '/usuarios/buscar')
const boton = (tabla: ReturnType<typeof within>, name: string) => tabla.queryByRole('button', { name })

describe('UsuariosScreen: acciones', () => {
  beforeEach(() => vi.clearAllMocks())

  it('una cuenta activa y pendiente ofrece reenviar y desactivar', async () => {
    const { tabla } = await enLaTabla([usuario({ invitacionPendiente: true })])

    expect(boton(tabla, 'Reenviar invitación')).toBeInTheDocument()
    expect(boton(tabla, 'Desactivar')).toBeInTheDocument()
    expect(boton(tabla, 'Reactivar')).not.toBeInTheDocument()
  })

  it('una cuenta inactiva solo ofrece reactivar, aunque esté pendiente', async () => {
    const { tabla } = await enLaTabla([usuario({ estado: 'INACTIVO', invitacionPendiente: true })])

    expect(boton(tabla, 'Reactivar')).toBeInTheDocument()
    expect(boton(tabla, 'Desactivar')).not.toBeInTheDocument()
    expect(boton(tabla, 'Reenviar invitación')).not.toBeInTheDocument()
  })

  it('«Cambiar roles» abre la ventana con los roles de esa cuenta', async () => {
    const { user, tabla } = await enLaTabla([usuario({ estado: 'INACTIVO' })])

    await user.click(boton(tabla, 'Cambiar roles')!)

    const ventana = await screen.findByRole('dialog')
    expect(ventana).toHaveTextContent('ana@test.local')
    expect(within(ventana).getByRole('checkbox', { name: 'Usuario' })).toBeChecked()
  })

  it('la cuenta propia no ofrece desactivar', async () => {
    const { tabla } = await enLaTabla([usuario({ codigo: 'yo' })])

    expect(boton(tabla, 'Desactivar')).not.toBeInTheDocument()
  })

  it('no desactiva sin confirmar; al confirmar llama una vez y vuelve a consultar', async () => {
    const { post, user, tabla } = await enLaTabla([usuario()])

    await user.click(boton(tabla, 'Desactivar')!)
    const dialogo = await screen.findByRole('alertdialog')
    expect(dialogo).toHaveTextContent('se cerrarán sus sesiones')
    expect(acciones(post)).toHaveLength(0)
    const consultas = post.mock.calls.length

    await user.click(within(dialogo).getByRole('button', { name: 'Desactivar' }))

    await waitFor(() => expect(toastSuccess).toHaveBeenCalledWith('Usuario desactivado'))
    expect(acciones(post)).toEqual([['/usuarios/u-1/desactivar']])
    expect(post.mock.calls.length).toBeGreaterThan(consultas + 1)
  })

  it('reactivar no pide confirmación', async () => {
    const { post, user, tabla } = await enLaTabla([usuario({ estado: 'INACTIVO' })])

    await user.click(boton(tabla, 'Reactivar')!)

    await waitFor(() => expect(acciones(post)).toEqual([['/usuarios/u-1/reactivar']]))
  })

  it('avisa si el correo reenviado no salió', async () => {
    const { user, tabla } = await enLaTabla([usuario({ invitacionPendiente: true })], () =>
      Promise.resolve({ data: { invitacionEnviada: false } }),
    )

    await user.click(boton(tabla, 'Reenviar invitación')!)

    await waitFor(() => expect(toastWarning).toHaveBeenCalled())
    expect(toastSuccess).not.toHaveBeenCalled()
  })

  it('un rechazo del servidor muestra su mensaje y la fila no cambia', async () => {
    const { user, tabla } = await enLaTabla([usuario({ estado: 'INACTIVO' })], () =>
      Promise.reject(new ApiError('ERR_AUTH_08', 'No tenés permiso')),
    )

    await user.click(boton(tabla, 'Reactivar')!)

    await waitFor(() => expect(toastError).toHaveBeenCalledWith('No tenés permiso'))
    expect(screen.getByRole('table')).toHaveTextContent('Inactiva')
  })
})
