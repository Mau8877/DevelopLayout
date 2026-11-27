import { Checkbox } from '@/components/ui/checkbox'
import { cn } from '@/lib/utils'

export interface CheckboxGroupOption {
  value: string
  label: string
  descripcion?: string
  disabled?: boolean
}

export interface CheckboxGroupFieldProps {
  /** Id del grupo; vincula las descripciones y el error (accesibilidad). */
  id: string
  legend: string
  options: CheckboxGroupOption[]
  /** Valores marcados. */
  value: string[]
  /** Recibe los valores marcados, en el orden de `options`. */
  onChange: (value: string[]) => void
  /** Mensaje de error inline; si viene, se muestra bajo las casillas. */
  error?: string
  className?: string
}

/** Grupo de casillas con leyenda y error inline. No conoce ningún formulario ni dominio. */
export function CheckboxGroupField({
  id,
  legend,
  options,
  value,
  onChange,
  error,
  className,
}: CheckboxGroupFieldProps) {
  const errorId = error ? `${id}-error` : undefined

  function alternar(opcion: string, marcada: boolean) {
    onChange(
      options.map((o) => o.value).filter((v) => (v === opcion ? marcada : value.includes(v))),
    )
  }

  return (
    <fieldset aria-describedby={errorId} className={cn('flex flex-col gap-2', className)}>
      <legend className="mb-2 text-sm font-medium">{legend}</legend>
      {options.map((option) => {
        const labelId = `${id}-${option.value}-label`
        const descripcionId = option.descripcion ? `${id}-${option.value}-descripcion` : undefined
        return (
          <label key={option.value} className="flex items-start gap-2 text-sm">
            <Checkbox
              // * La casilla no es un `button`: su estado deshabilitado llega como `data-disabled`.
              className="mt-0.5 data-disabled:cursor-not-allowed data-disabled:opacity-50"
              checked={value.includes(option.value)}
              disabled={option.disabled}
              aria-invalid={Boolean(error)}
              aria-labelledby={labelId}
              aria-describedby={descripcionId}
              onCheckedChange={(marcada) => alternar(option.value, marcada)}
            />
            <span className="flex flex-col">
              <span id={labelId}>{option.label}</span>
              {option.descripcion && (
                <span id={descripcionId} className="text-xs text-muted-foreground">
                  {option.descripcion}
                </span>
              )}
            </span>
          </label>
        )
      })}
      {error && (
        <p id={errorId} role="alert" className="text-xs text-destructive">
          {error}
        </p>
      )}
    </fieldset>
  )
}
