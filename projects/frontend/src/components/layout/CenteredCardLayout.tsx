import type { ReactNode } from 'react'
import { SelectorIdioma } from './SelectorIdioma'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'

interface CenteredCardLayoutProps {
  titulo: string
  descripcion?: string
  children: ReactNode
}

/** Layout centrado con una tarjeta, para pantallas públicas (login, recuperación, etc.). */
export function CenteredCardLayout({ titulo, descripcion, children }: CenteredCardLayoutProps) {
  return (
    <main className="relative flex min-h-screen items-center justify-center bg-background p-4 text-foreground">
      <SelectorIdioma className="absolute top-4 right-4" />
      <Card className="w-full max-w-sm">
        <CardHeader className="text-center">
          <CardTitle className="text-xl">{titulo}</CardTitle>
          {descripcion && <CardDescription>{descripcion}</CardDescription>}
        </CardHeader>
        <CardContent>{children}</CardContent>
      </Card>
    </main>
  )
}
