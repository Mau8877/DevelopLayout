import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { useState } from 'react'
import { describe, expect, it, vi } from 'vitest'
import { CheckboxGroupField } from './checkbox-group-field'

const OPCIONES = [
  { value: 'a', label: 'Primera', descripcion: 'La de arriba' },
  { value: 'b', label: 'Segunda' },
  { value: 'c', label: 'Bloqueada', disabled: true },
]

function Controlado({ onChange, inicial = [] }: { onChange: (v: string[]) => void; inicial?: string[] }) {
  const [valor, setValor] = useState(inicial)
  return (
    <CheckboxGroupField
      id="grupo"
      legend="Opciones"
      options={OPCIONES}
      value={valor}
      onChange={(nuevo) => {
        setValor(nuevo)
        onChange(nuevo)
      }}
    />
  )
}

describe('CheckboxGroupField', () => {
  it('nombra el grupo y cada casilla', () => {
    render(<Controlado onChange={vi.fn()} />)

    expect(screen.getByRole('group', { name: 'Opciones' })).toBeInTheDocument()
    expect(screen.getByRole('checkbox', { name: 'Primera' })).toHaveAccessibleDescription('La de arriba')
    expect(screen.getByRole('checkbox', { name: 'Segunda' })).not.toBeChecked()
  })

  it('marca y desmarca con el mouse, respetando el orden de las opciones', async () => {
    const onChange = vi.fn()
    render(<Controlado onChange={onChange} />)

    await userEvent.click(screen.getByRole('checkbox', { name: 'Segunda' }))
    await userEvent.click(screen.getByText('Primera'))
    expect(onChange).toHaveBeenLastCalledWith(['a', 'b'])

    await userEvent.click(screen.getByRole('checkbox', { name: 'Segunda' }))
    expect(onChange).toHaveBeenLastCalledWith(['a'])
  })

  it('se opera con el teclado', async () => {
    const onChange = vi.fn()
    render(<Controlado onChange={onChange} />)

    await userEvent.tab()
    await userEvent.keyboard(' ')

    expect(onChange).toHaveBeenLastCalledWith(['a'])
    expect(screen.getByRole('checkbox', { name: 'Primera' })).toBeChecked()
  })

  it('no cambia una opción deshabilitada', async () => {
    const onChange = vi.fn()
    render(<Controlado onChange={onChange} inicial={['c']} />)

    await userEvent.click(screen.getByText('Bloqueada'))

    expect(onChange).not.toHaveBeenCalled()
    expect(screen.getByRole('checkbox', { name: 'Bloqueada' })).toBeChecked()
  })

  it('muestra el error y marca las casillas como inválidas', () => {
    render(
      <CheckboxGroupField
        id="grupo"
        legend="Opciones"
        options={OPCIONES}
        value={[]}
        onChange={vi.fn()}
        error="Elegí al menos una"
      />,
    )

    expect(screen.getByRole('alert')).toHaveTextContent('Elegí al menos una')
    expect(screen.getByRole('group')).toHaveAccessibleDescription('Elegí al menos una')
    expect(screen.getByRole('checkbox', { name: 'Segunda' })).toHaveAttribute('aria-invalid', 'true')
  })
})
