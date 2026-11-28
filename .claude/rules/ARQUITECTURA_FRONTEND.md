---
globs: projects/frontend/**/*
---

# Arquitectura Frontend

**Feature-based**: el código se organiza por funcionalidad de negocio
(`features/{feature}/`), no por tipo técnico (nunca una carpeta global
`components/`, `hooks/`, `types/` con todo mezclado de todas las
features). Lo transversal a toda la app (no específico de una sola
feature) vive en carpetas de nivel superior dedicadas.

## Estructura de `src/`

```
src/
├── routes/              # File-based routing de TanStack Router (ver RUTAS_NAVEGACION_FRONTEND.md)
├── layouts/             # Layouts por rol (AdminLayout, UsuarioLayout, ver RUTAS_NAVEGACION_FRONTEND.md)
├── features/
│   └── {feature}/       # Un feature de negocio (ver estructura abajo)
├── shared/
│   ├── api/             # Endpoints usados por 2+ features (ver TANSTACK_QUERY_FRONTEND.md)
│   └── language/        # i18next y textos comunes (ver IDIOMAS_FRONTEND.md)
├── store/                # Zustand (estado de UI) + cliente API/TanStack Query (ver ESTADO_GLOBAL_FRONTEND.md)
├── components/
│   ├── ui/               # Output del CLI de shadcn + componentes genéricos armados con él (ver COMPONENTIZACION_FRONTEND.md)
│   └── layout/           # Piezas del shell de la app (AppShell, sidebar, header)
├── lib/
│   └── utils.ts          # `cn()` que shadcn espera en @/lib/utils -- no se toca ni se mueve
├── utils/                # Utilidades genéricas propias del proyecto (ver TANSTACK_QUERY_FRONTEND.md)
├── routeTree.gen.ts      # Generado por TanStack Router (se commitea, no se edita)
├── setupTests.ts         # Setup global de Vitest (matchers de jest-dom)
├── main.tsx              # Composition root: QueryProvider + RouterProvider
└── index.css             # Directivas de Tailwind + tokens de tema
```

`shared/api/schema.d.ts` son tipos **generados** del contrato OpenAPI
(`pnpm openapi:sync` + `pnpm openapi:types`, ver `scripts/README.md`). No se
edita a mano; los tipos de request/response se toman de ahí. Además de
`shared/api/` está `shared/language/` (textos comunes). Lo demás se crea con el
primer caso concreto que lo necesite (ver
[TANSTACK_QUERY_FRONTEND.md](frontend/TANSTACK_QUERY_FRONTEND.md#endpoints-usados-por-más-de-una-feature)).

`utils/index.ts` es la lista de lo que hay en `utils/`. Dos piezas que conviene
conocer: `dayjsLocal.ts` (dayjs siguiendo el idioma vigente) y `marca.ts` (el
nombre del producto, la única constante que cambia `project:init`). `layouts/`
tiene un layout por rol (`admin/`, `usuario/`) y `navegacion/` (los enlaces del
menú guardan claves de idioma, no textos) — ver
[Layouts por rol](frontend/RUTAS_NAVEGACION_FRONTEND.md#layouts-por-rol).

## Estructura de un feature (`src/features/{feature}/`)

`src/features/auth/` y `src/features/usuarios/` son las referencias reales
(`api/`, `components/`, `language/`, `schemas/`, `screens/`, `tests/`,
`types/` e `index.ts`); `src/features/home/` es el scaffold vacío.
**Cualquier feature nueva sigue esta misma forma**:

```
src/features/usuarios/
├── api/            # Queries/mutations de TanStack Query (useUsuarios.ts, keys.ts...)
├── language/       # Textos de ESTA feature: es.json y en.json (ver IDIOMAS_FRONTEND.md)
├── components/     # Solo componentes que conocen el dominio de ESTA feature (ver COMPONENTIZACION_FRONTEND.md)
├── schemas/        # Schemas de Zod (validación de formularios + tipado de requests)
├── screens/        # Pantallas completas que se montan desde routes/ (ListaUsuarios, DetalleUsuario...)
├── tests/          # Tests de esta feature (ver nota de Testing abajo)
├── types/          # Tipos TS propios de la feature que no salen de un schema de Zod
└── index.ts        # Barrel: re-exporta lo que otras partes de la app pueden importar de esta feature
```

Referencia de cada subcarpeta, en el resto de las rules:
[ESTADO_GLOBAL_FRONTEND.md](frontend/ESTADO_GLOBAL_FRONTEND.md) (cuándo un
store va dentro de la feature), [FORMULARIOS_ZOD_TANSTACK_FRONTEND.md](frontend/FORMULARIOS_ZOD_TANSTACK_FRONTEND.md)
(`schemas/`), [TANSTACK_QUERY_FRONTEND.md](frontend/TANSTACK_QUERY_FRONTEND.md)
(`api/`), [RUTAS_NAVEGACION_FRONTEND.md](frontend/RUTAS_NAVEGACION_FRONTEND.md)
(cómo `screens/` se conecta con `routes/`).

### `index.ts`: qué es público de la feature

El `index.ts` de la raíz de la feature es el único punto de entrada que
otras partes de la app (u otras features) pueden importar. Un componente
de `features/pedidos/` que necesita algo de `features/usuarios/` importa
desde `features/usuarios` (el barrel), nunca de un archivo interno como
`features/usuarios/screens/DetalleUsuario.tsx` directo — así la feature
puede reorganizar su estructura interna sin romper a quien la consume.

### Barriles: un `index.ts` en cada carpeta

> La migración de lo escrito antes es
> [HU-INICIAL-Convenciones](../../docs/HistoriasDeUsuario/HU-INICIAL-Convenciones.md).

**Obligatorio** en toda carpeta con código de una feature (`api/`,
`components/`, `schemas/`, `screens/`, `types/` y las que se agreguen) y en las
carpetas transversales propias (`utils/`, `shared/api/`, `shared/language/`,
`store/`, `components/layout/`, `layouts/`). El barril **exporta todo lo de su
carpeta**; al crear, renombrar o borrar un archivo se actualiza en el mismo
cambio.

**No llevan barril:** `components/ui/` y `lib/` (el CLI de shadcn trabaja archivo
por archivo; las subcarpetas propias de `ui/`, como `tanstack-table/`, sí lo
tienen), `routes/` (ahí un `index.tsx` es una página), `tests/` y las carpetas
`language/` que solo tienen JSON.

Cómo se importa, para no crear ciclos:

| Desde | Se importa de |
| --- | --- |
| Fuera de la feature | Solo el barril raíz: `@/features/usuarios` |
| Otra carpeta de la misma feature | El barril de esa carpeta: `'../api'` |
| Un archivo de la misma carpeta | El archivo directo: `'./keys'`, **nunca** su propio barril |
| Una carpeta transversal propia, desde fuera | Su barril: `@/utils`, `@/shared/api` |

- El barril de una carpeta puede usar `export * from './archivo'`.
- El barril **raíz** de la feature no usa `export *` de sus carpetas: nombra lo
  que es público, para que la feature no exponga sus detalles.
- Una prueba falla si una carpeta obligatoria no tiene barril, si un barril deja
  un archivo sin exportar o si alguien salta el barril desde fuera.

### Tests: co-ubicados a nivel de app, centralizados a nivel de feature

Dos criterios distintos según dónde vive el código, ambos documentados en
[TESTING_FRONTEND.md](frontend/TESTING_FRONTEND.md):

- Código transversal (`src/utils/`, `src/lib/`, `src/components/`): el
  test vive **al lado** del archivo (`debounce.ts` + `debounce.test.ts` en
  la misma carpeta).
- Código de una feature (`src/features/{feature}/`): el test vive en la
  carpeta `tests/` de esa misma feature, no al lado del archivo que
  prueba — mismo criterio que ya trae el scaffold de `features/home/`.

## Regla de dependencia

`features/{feature}/` puede importar de `store/`, `components/ui/`,
`lib/`, `utils/` y `shared/` — nunca al revés. Ninguna carpeta transversal
(`store/`, `components/ui/`, etc.) importa nada de `features/`, porque
dejaría de ser transversal (quedaría acoplada a una feature puntual).

**Única excepción:** `layouts/`, que arma el shell de cada rol y por eso importa
de `features/auth` el cierre de sesión (`useLogout`) y el tipo del usuario en
sesión (`UsuarioSesion`), siempre por el barril. `components/layout/` no: recibe
todo por props.
