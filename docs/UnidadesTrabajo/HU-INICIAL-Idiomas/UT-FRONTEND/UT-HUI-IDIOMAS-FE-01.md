# UT-HUI-IDIOMAS-FE-01 — Base de idiomas en la web

| Campo | Valor |
|-------|-------|
| **Código** | `UT-HUI-IDIOMAS-FE-01` |
| **Nombre** | Base de idiomas en la web |
| **Historia** | HU-INICIAL-Idiomas-FE |
| **Depende de** | — (el servidor de la UT BE-01 ayuda a probar) |

## Antes de implementar

IDIOMAS_FRONTEND, ESTADO_GLOBAL_FRONTEND y TANSTACK_QUERY_FRONTEND.

## Solicitud de negocio

Hace falta una base única antes de migrar pantallas.

## Objetivo

i18next configurado, idioma detectado y guardado, y enviado a la API.

## Alcance

- Dependencias nuevas: `i18next` y `react-i18next` (descarga de npm).
- `shared/language/`: `i18n.ts` (descubre `language/{es,en}.json` con `import.meta.glob`), `idiomas.ts` (soportados y detección), `useIdioma.ts`, `es.json`, `en.json`, `index.ts`.
- `src/i18next.d.ts`: claves tipadas.
- `main.tsx` inicia i18n antes de renderizar; `setupTests.ts` lo inicia en `es`.
- `apiClient` envía `Accept-Language`, también en la renovación y en la petición de CSRF.
- Al cambiar: `lang` de `<html>`, `localStorage` (con `try/catch`) e invalidación de la caché de TanStack Query.
- Prueba de paridad de claves y marcadores, y de que cada carpeta `language` está declarada en los tipos.

**Archivos esperados (crear/tocar):**

- `projects/frontend/src/shared/language/i18n.ts`
- `projects/frontend/src/shared/language/idiomas.ts`
- `projects/frontend/src/shared/language/useIdioma.ts`
- `projects/frontend/src/shared/language/es.json`
- `projects/frontend/src/shared/language/en.json`
- `projects/frontend/src/shared/language/index.ts`
- `projects/frontend/src/i18next.d.ts`
- `projects/frontend/src/main.tsx`
- `projects/frontend/src/setupTests.ts`
- `projects/frontend/src/store/apiClient.ts`
- `projects/frontend/src/shared/language/idiomas.test.ts`
- `projects/frontend/src/shared/language/paridad.test.ts`
- `projects/frontend/package.json`
- `pnpm-lock.yaml`

## Seguridad

La preferencia no es sensible. El valor guardado se valida contra la lista antes de usarlo. Las interpolaciones se escapan.

## Fuera de alcance (no tocar)

- Migrar pantallas.
- El selector.

## Resultado esperado

La web sabe en qué idioma está y lo comunica.

## Validación

1. Detección: guardada, `en-US`, `es-BO`, `fr-FR` y valor guardado inválido.
2. Cada petición lleva `Accept-Language` y cambia al cambiar el idioma.
3. Quitar una clave de `en.json` hace fallar la prueba; una clave mal escrita rompe `pnpm build`.
4. Sin `localStorage` disponible, la web funciona.
