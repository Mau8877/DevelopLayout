# Frontend

Panel web en React 19 + TypeScript + Vite, con TanStack (Query, Router, Form,
Table), Tailwind CSS 4 y shadcn (base-ui).

Es parte del workspace pnpm de la raíz: el `pnpm-lock.yaml` vive allá, así que
`pnpm install` se corre desde la raíz del repo.

## Comandos

```bash
pnpm dev       # servidor de desarrollo (Vite, puerto 5173)
pnpm build     # tsc -b + build de producción
pnpm lint      # ESLint
pnpm test      # Vitest (corre una vez y termina)
```

## Estructura

Feature-based: `src/features/{feature}/` por funcionalidad de negocio y, para
lo transversal, `src/routes/` (TanStack Router, file-based), `src/layouts/`,
`src/store/` (Zustand para UI y cliente API/TanStack Query),
`src/components/ui/` (shadcn y el paquete de tablas `tanstack-table/`) y
`src/utils/`. El detalle está en
[`ARQUITECTURA_FRONTEND.md`](../../.claude/rules/ARQUITECTURA_FRONTEND.md) y
las reglas de `.claude/rules/frontend/`.

## Variables de entorno

Se copia `.env.example` a `.env`. Vite solo expone al navegador las que
empiezan con `VITE_`:

- `VITE_API_BASE_URL`: URL base de la API (en desarrollo apunta directo al
  backend; en la imagen de producción es `/api/v1`, relativa al proxy).
- `VITE_ENABLE_API_LOGS`: logging de requests HTTP en consola (apagado por
  defecto).

## Imagen de producción

`Dockerfile` (contexto de build = raíz del repo): compila con Node y sirve los
estáticos con `nginx-unprivileged` en el puerto `8080`. Ver
[`docs/INFRAESTRUCTURA.md`](../../docs/INFRAESTRUCTURA.md).

## Notas de versiones

- `@tanstack/react-table` está en la v9: cada tabla declara sus *features*
  (`components/ui/tanstack-table/shared/tableFeatures.ts`).
- TypeScript se mantiene en 6.x hasta que `typescript-eslint` soporte la 7.
