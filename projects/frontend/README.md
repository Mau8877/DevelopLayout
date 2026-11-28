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

Las pruebas de navegador (Playwright, en `e2e/`) se lanzan **desde la raíz**
con `pnpm test:e2e`: necesitan Docker y JDK 21, y la primera vez
`pnpm --dir projects/frontend exec playwright install chromium`. Ver
[`scripts/README.md`](../../scripts/README.md).

## Estructura

Feature-based: `src/features/{feature}/` por funcionalidad de negocio y, para
lo transversal, `src/routes/` (TanStack Router, file-based), `src/layouts/`,
`src/store/` (Zustand para UI y cliente API/TanStack Query),
`src/shared/` (tipos generados del contrato y textos comunes),
`src/components/ui/` (shadcn y el paquete de tablas `tanstack-table/`),
`src/components/layout/` y `src/utils/`. Cada carpeta de código tiene su barril
`index.ts`. El detalle está en
[`ARQUITECTURA_FRONTEND.md`](../../.claude/rules/ARQUITECTURA_FRONTEND.md) y
las reglas de `.claude/rules/frontend/`.

## Textos e idiomas

Ningún texto visible va en el código: cada feature tiene
`language/es.json` y `en.json`, y lo común vive en `src/shared/language/`.
ESLint y las pruebas rechazan texto fijo, claves que no existen y diferencias
entre idiomas. Una carpeta `language` nueva se declara en `src/i18next.d.ts`.
Ver [`IDIOMAS_FRONTEND.md`](../../.claude/rules/frontend/IDIOMAS_FRONTEND.md).

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
