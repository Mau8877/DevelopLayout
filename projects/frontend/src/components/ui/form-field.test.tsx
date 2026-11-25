import { describe, it, expect } from 'vitest'
import { render, screen } from '@testing-library/react'
import { FormField } from './form-field'

describe('FormField', () => {
  it('asocia el label con el control', () => {
    render(<FormField id="correo" label="Correo">{(p) => <input {...p} />}</FormField>)

    expect(screen.getByLabelText('Correo')).toBeInTheDocument()
  })

  it('muestra el error inline y marca el control como inválido', () => {
    render(
      <FormField id="correo" label="Correo" error="El correo es obligatorio">
        {(p) => <input {...p} />}
      </FormField>,
    )

    const input = screen.getByLabelText('Correo')
    expect(screen.getByText('El correo es obligatorio')).toBeInTheDocument()
    expect(input).toHaveAttribute('aria-invalid', 'true')
    expect(input).toHaveAttribute('aria-describedby', 'correo-error')
  })

  it('sin error no muestra mensaje y el control es válido', () => {
    render(<FormField id="correo" label="Correo">{(p) => <input {...p} />}</FormField>)

    expect(screen.queryByRole('alert')).not.toBeInTheDocument()
    expect(screen.getByLabelText('Correo')).toHaveAttribute('aria-invalid', 'false')
  })

  it('muestra la descripción', () => {
    render(
      <FormField id="correo" label="Correo" description="Usá tu correo institucional">
        {(p) => <input {...p} />}
      </FormField>,
    )

    expect(screen.getByText('Usá tu correo institucional')).toBeInTheDocument()
  })
})
