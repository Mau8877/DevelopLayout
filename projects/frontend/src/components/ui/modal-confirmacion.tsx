import type { AlertDialog as AlertDialogPrimitive } from '@base-ui/react/alert-dialog'
import { Loader2Icon } from 'lucide-react'
import {
  AlertDialog,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from '@/components/ui/alert-dialog'
import { Button } from '@/components/ui/button'

export interface ModalConfirmacionProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  title: string
  /** Qué va a pasar al confirmar. */
  description: string
  confirmLabel: string
  cancelLabel?: string
  isSubmitting?: boolean
  onConfirm: () => void
}

/**
 * Confirmación de una acción que se puede deshacer.
 *
 * * Para un borrado definitivo se usa `ModalDestructive`, que lo advierte.
 */
export function ModalConfirmacion({
  open,
  onOpenChange,
  title,
  description,
  confirmLabel,
  cancelLabel = 'Cancelar',
  isSubmitting = false,
  onConfirm,
}: ModalConfirmacionProps) {
  const handleOpenChange = (
    siguiente: boolean,
    detalles: AlertDialogPrimitive.Root.ChangeEventDetails,
  ) => {
    // * Mientras se envía, ni Escape cierra el modal a mitad de camino.
    if (isSubmitting) {
      detalles.cancel()
      return
    }
    onOpenChange(siguiente)
  }

  return (
    <AlertDialog open={open} onOpenChange={handleOpenChange}>
      <AlertDialogContent>
        <AlertDialogHeader>
          <AlertDialogTitle>{title}</AlertDialogTitle>
          <AlertDialogDescription>{description}</AlertDialogDescription>
        </AlertDialogHeader>
        <AlertDialogFooter>
          <Button type="button" variant="outline" disabled={isSubmitting} onClick={() => onOpenChange(false)}>
            {cancelLabel}
          </Button>
          <Button type="button" disabled={isSubmitting} onClick={onConfirm}>
            {isSubmitting && <Loader2Icon data-icon="inline-start" className="animate-spin" />}
            {confirmLabel}
          </Button>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  )
}
