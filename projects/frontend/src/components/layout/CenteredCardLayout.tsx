import type { ReactNode } from 'react'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'

interface CenteredCardLayoutProps {
  titulo: string
  descripcion?: string
  children: ReactNode
}

/** Layout centrado con una tarjeta, para pantallas públicas (login, recuperación, etc.). */
export function CenteredCardLayout({ titulo, descripcion, children }: CenteredCardLayoutProps) {
  return (
    <main className="flex min-h-screen items-center justify-center bg-background p-4 text-foreground">
      <Card className="w-full max-w-sm">
        <CardHeader>
          <CardTitle className="text-xl">{titulo}</CardTitle>
          {descripcion && <CardDescription>{descripcion}</CardDescription>}
        </CardHeader>
        <CardContent>{children}</CardContent>
      </Card>
    </main>
  )
}
