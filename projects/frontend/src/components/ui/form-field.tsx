import * as React from "react"

import { cn } from "@/lib/utils"
import { Label } from "@/components/ui/label"

interface FormFieldProps {
  /** Id del control; vincula el label, el error y la descripción (accesibilidad). */
  id: string
  label: string
  /** Mensaje de error inline; si viene, se muestra bajo el control. */
  error?: string
  description?: string
  className?: string
  /** Recibe las props de accesibilidad que el control debe aplicar (id, aria-*). */
  children: (controlProps: {
    id: string
    "aria-invalid": boolean
    "aria-describedby": string | undefined
  }) => React.ReactNode
}

/**
 * Campo de formulario genérico: label + control + descripción + error inline.
 *
 * * No conoce ningún formulario concreto ni la librería de formularios: el
 * * control se pasa como render-prop y recibe los atributos de accesibilidad.
 */
function FormField({ id, label, error, description, className, children }: FormFieldProps) {
  const descriptionId = description ? `${id}-description` : undefined
  const errorId = error ? `${id}-error` : undefined
  const describedBy = [descriptionId, errorId].filter(Boolean).join(" ") || undefined

  return (
    <div data-slot="form-field" className={cn("flex flex-col gap-1.5", className)}>
      <Label htmlFor={id}>{label}</Label>
      {children({ id, "aria-invalid": Boolean(error), "aria-describedby": describedBy })}
      {description && (
        <p id={descriptionId} className="text-xs text-muted-foreground">
          {description}
        </p>
      )}
      {error && (
        <p id={errorId} role="alert" className="text-xs text-destructive">
          {error}
        </p>
      )}
    </div>
  )
}

export { FormField }
export type { FormFieldProps }
