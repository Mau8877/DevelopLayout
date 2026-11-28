import { useTranslation } from 'react-i18next'
import { Button } from "@/components/ui/button"

interface ErrorStateProps {
  titulo?: string
  descripcion?: string
  /** Si viene, se muestra el botón "Reintentar". */
  onReintentar?: () => void
}

/** Estado de error genérico: título, descripción y botón de reintento opcional. */
function ErrorState({
  titulo,
  descripcion,
  onReintentar,
}: ErrorStateProps) {
  const { t } = useTranslation('shared')
  return (
    <div
      data-slot="error-state"
      role="alert"
      className="flex min-h-[50vh] flex-col items-center justify-center gap-3 p-6 text-center"
    >
      <h2 className="text-lg font-semibold text-foreground">{titulo ?? t("error.titulo")}</h2>
      <p className="max-w-md text-sm text-muted-foreground">{descripcion ?? t("error.descripcion")}</p>
      {onReintentar && (
        <Button variant="outline" onClick={onReintentar}>
          {t("comun.reintentar")}
        </Button>
      )}
    </div>
  )
}

export { ErrorState }
export type { ErrorStateProps }
