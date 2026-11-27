import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import { ModalConfirmacion } from './modal-confirmacion'

function montar(isSubmitting = false) {
  const onConfirm = vi.fn()
  const onOpenChange = vi.fn()
  render(
    <ModalConfirmacion
      open
      onOpenChange={onOpenChange}
      title="Pausar"
      description="Se pausará la cuenta."
      confirmLabel="Pausar cuenta"
      isSubmitting={isSubmitting}
      onConfirm={onConfirm}
    />,
  )
  return { onConfirm, onOpenChange }
}

describe('ModalConfirmacion', () => {
  it('explica la acción sin afirmar que es irreversible', () => {
    montar()

    expect(screen.getByRole('alertdialog', { name: 'Pausar' })).toHaveAccessibleDescription('Se pausará la cuenta.')
    expect(screen.queryByText(/no se puede deshacer/i)).not.toBeInTheDocument()
  })

  it('confirma solo con el botón de confirmar', async () => {
    const { onConfirm } = montar()

    await userEvent.click(screen.getByRole('button', { name: 'Pausar cuenta' }))

    expect(onConfirm).toHaveBeenCalledTimes(1)
  })

  it('cancelar cierra sin confirmar', async () => {
    const { onConfirm, onOpenChange } = montar()

    await userEvent.click(screen.getByRole('button', { name: 'Cancelar' }))

    expect(onOpenChange).toHaveBeenCalledWith(false)
    expect(onConfirm).not.toHaveBeenCalled()
  })

  it('mientras envía no se cierra ni confirma otra vez', async () => {
    const { onConfirm, onOpenChange } = montar(true)

    await userEvent.keyboard('{Escape}')
    await userEvent.click(screen.getByRole('button', { name: /Pausar cuenta/ }))
    await userEvent.click(screen.getByRole('button', { name: 'Cancelar' }))

    expect(onOpenChange).not.toHaveBeenCalled()
    expect(onConfirm).not.toHaveBeenCalled()
  })
})
