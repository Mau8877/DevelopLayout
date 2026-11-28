import { useTranslation } from 'react-i18next'
import { Loader2Icon } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { DialogFooter } from '@/components/ui/dialog'
import { cn } from '@/lib/utils'

export interface ModalLayoutFooterProps {
  formId: string
  isSubmitting?: boolean
  confirmLabel?: string
  cancelLabel?: string
  onCancel: () => void
  className?: string
}

export function ModalLayoutFooter({
  formId,
  isSubmitting = false,
  confirmLabel,
  cancelLabel,
  onCancel,
  className,
}: ModalLayoutFooterProps) {
  const { t } = useTranslation('shared')
  return (
    <DialogFooter className={cn('shrink-0 border-t border-border p-6', className)}>
      <Button type="button" variant="destructive-solid" onClick={onCancel} disabled={isSubmitting}>
        {cancelLabel ?? t('comun.cancelar')}
      </Button>
      <Button type="submit" variant="success" form={formId} disabled={isSubmitting}>
        {isSubmitting && <Loader2Icon data-icon="inline-start" className="animate-spin" />}
        {confirmLabel ?? t('comun.guardar')}
      </Button>
    </DialogFooter>
  )
}
