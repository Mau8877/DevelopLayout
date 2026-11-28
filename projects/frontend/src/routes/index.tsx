import { createFileRoute } from '@tanstack/react-router'
import { APP_NAME } from '@/utils'

export const Route = createFileRoute('/')({
  component: HomePage,
})

function HomePage() {
  return (
    <div className="flex min-h-screen items-center justify-center bg-background text-foreground">
      <h1 className="text-3xl font-bold">{APP_NAME}</h1>
    </div>
  )
}
