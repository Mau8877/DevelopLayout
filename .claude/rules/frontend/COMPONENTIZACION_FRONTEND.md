---
globs: projects/frontend/**/*
---

# Componentización — Frontend

**Todo lo que se pueda reutilizar se construye como componente genérico, desde
la primera vez que se necesita.** Una pantalla no tiene markup repetido ni
patrones visuales escritos "inline" que otro módulo va a tener que volver a
escribir. `features/{feature}/components/` es el último recurso, no el default.

## Dónde vive cada componente (árbol de decisión)

Antes de escribir markup nuevo, en este orden:

1. **¿Ya existe?** Buscar en `src/components/ui/` y `src/components/layout/`
   (y en el CLI de shadcn: `pnpm dlx shadcn@latest add <componente>`). Si existe,
   se usa. Si existe pero le falta una variante, se extiende el componente
   existente (nueva prop/variante) en vez de crear uno paralelo.
2. **¿Conoce el dominio de una feature?** (importa tipos, hooks o textos de
   negocio de esa feature) → `src/features/{feature}/components/`. **Solo en
   este caso**, y solo si no hay otro lugar donde pueda vivir.
3. **Si no conoce ningún dominio** → es genérico y va en `src/components/`:
   - `components/ui/`: primitivos del sistema de diseño y componentes genéricos
     armados con ellos (ej. `tanstack-table/`, `modal-layout/`,
     `modal-destructive.tsx`).
   - `components/layout/`: piezas del shell de la app (`AppShell`, sidebar, header).

Un componente genérico **nunca** nace dentro de una feature "porque ahí lo
necesité primero" para moverlo después: nace directo en `components/ui/`. Un
componente de feature que otra feature termina necesitando se exporta por el
`index.ts` de su feature (ver [ARQUITECTURA_FRONTEND.md](../ARQUITECTURA_FRONTEND.md#indexts-qué-es-público-de-la-feature));
si en realidad no tenía nada de dominio, se baja a `components/ui/`.

Esto no contradice el criterio de "no anticipar" de `shared/api/`: aquello es
sobre endpoints (cuyo dueño es una feature); acá se trata de UI, donde un
componente genérico no tiene dueño de dominio.

## Qué se componentiza siempre

Antes de crear uno, revisa los que ya existen: `FormField`,
`CheckboxGroupField`, `ErrorState`, `ModalConfirmacion`, `ModalDestructive`,
`modal-layout/`, `tanstack-table/` y los avisos de `toast.ts` en
`components/ui/`; `CenteredCardLayout`, `SelectorIdioma` y
`PaginaNoEncontrada` en `components/layout/`.

Cada patrón de esta lista es un componente, no markup repetido:

- Campo de formulario (label + input + error inline + descripción) — la forma en
  que se muestran los errores de Zod, ver
  [FORMULARIOS_ZOD_TANSTACK_FRONTEND.md](FORMULARIOS_ZOD_TANSTACK_FRONTEND.md).
- Encabezado de página (título, acciones, breadcrumbs).
- Estados de una query: cargando (skeleton), vacío, error con "Reintentar" (ver
  [ERRORES_LOGGING_FRONTEND.md](ERRORES_LOGGING_FRONTEND.md)).
- Confirmación de acción destructiva (`modal-destructive`), tablas
  (`tanstack-table/`), modales (`modal-layout/`), badges de estado, tarjetas.
- Cualquier bloque que aparezca **dos o más veces** (en la misma pantalla o en
  módulos distintos).

Si el componente que hace falta todavía no existe, la UT que lo necesita lo
crea en `components/ui/` — no lo deja para "después" y no lo escribe inline.

## Cómo se escribe un componente genérico

- **Sin conocimiento de dominio ni de datos:** no importa de `features/`, no usa
  `apiClient`, no llama a hooks de `api/`, no lee stores de feature. Recibe todo
  por props (datos, callbacks, textos).
- **Composición antes que flags:** `children`/slots y variantes (`cva`, como
  hace shadcn) en vez de acumular booleans (`showX`, `hasY`, `isZ`). Si un
  componente pasa de ~8 props, probablemente son dos componentes.
- **Textos por props**, con defaults tomados de `shared/language` (ver
  [IDIOMAS_FRONTEND.md](IDIOMAS_FRONTEND.md)) solo si son universales
  ("Cancelar", "Reintentar"); nunca un texto de negocio fijo adentro.
- **Estilos:** solo clases de tema (ver [TAILWIND_STYLES_FRONTEND.md](TAILWIND_STYLES_FRONTEND.md)).
  Sin colores ni tamaños arbitrarios.
- **Accesibilidad:** elementos semánticos, `label` asociado a cada input,
  `aria-*` donde corresponda, foco visible y operable por teclado. Un componente
  que solo funciona con el mouse no está terminado.
- **Estructura:** un componente = una carpeta si tiene más de un archivo
  (`componente.tsx`, `types.ts`, `componente.test.tsx`, `index.ts` barrel), como
  `modal-layout/`. Las props se tipan y exportan.
- **Tests co-ubicados** junto al archivo (ver [TESTING_FRONTEND.md](TESTING_FRONTEND.md)):
  comportamiento (qué renderiza, qué eventos dispara), no implementación.
- No se edita el output del CLI de shadcn salvo necesidad real (ver
  [TAILWIND_STYLES_FRONTEND.md](TAILWIND_STYLES_FRONTEND.md)); lo que se compone
  sobre esos primitivos vive al lado, como componente propio.

## Pantallas delgadas

Un archivo de `screens/` arma la pantalla: obtiene datos con un hook de `api/`
y **compone** componentes. No define componentes grandes dentro del mismo
archivo ni repite bloques. Si una pantalla supera ~150 líneas de JSX o tiene un
bloque que se repite, se extrae.

## Límites (para no sobre-abstraer)

Componentizar es extraer un patrón real, no inventar una "librería de
componentes" por adelantado: no se crean componentes que ninguna UT necesita
todavía, y un componente de un solo uso con 15 props de configuración es peor
que markup claro. El criterio es: **cuando algo se construye y es genérico, se
construye genérico y en el lugar correcto**.
