# UT-HUI-SESIONES-FE-01 — Tipos, hooks y resumen de dispositivo

| Campo | Valor |
|-------|-------|
| **Código** | `UT-HUI-SESIONES-FE-01` |
| **Nombre** | Tipos, hooks y resumen de dispositivo |
| **Historia** | HU-INICIAL-Sesiones-FE |
| **Depende de** | UT-HUI-SESIONES-BE-02 (contrato OpenAPI) |

## Antes de implementar

Leer `frontend/TANSTACK_QUERY_FRONTEND.md` y `frontend/ERRORES_LOGGING_FRONTEND.md`.

## Solicitud de negocio

La pantalla necesita los datos y un nombre de dispositivo entendible.

## Objetivo

Existen los hooks de «Mis sesiones» y `resumirDispositivo`.

## Alcance

- `pnpm openapi:types`; tipo `MiSesion` derivado de `MiSesionResponse`.
- `api/useMisSesiones.ts` (listado paginado; conserva la página anterior mientras carga la siguiente), `api/useCerrarMiSesion.ts` y `api/useCerrarOtrasSesiones.ts`. Las dos mutaciones invalidan el listado al terminar, **con éxito o con error** (un 404 significa que la lista quedó vieja).
- Keys `authKeys.misSesiones` en `api/keys.ts`. Al cerrar sesión la caché ya se limpia.
- Un 401 lo resuelve `apiClient`: los hooks no hacen nada especial.
- `utils/resumirDispositivo.ts`: función pura `(userAgent, tipoCliente)` → «Chrome en Windows», «Safari en iPhone», «App móvil» (para `MOBILE`) o «Dispositivo desconocido». Sin librerías nuevas; reconoce los navegadores y sistemas comunes y no inventa los demás. Cuidado con el orden: el user agent de Edge y Opera contiene «Chrome», y el de Chrome contiene «Safari».

**Archivos esperados (crear/tocar):**

- `projects/frontend/src/features/auth/api/useMisSesiones.ts`
- `projects/frontend/src/features/auth/api/useCerrarMiSesion.ts`
- `projects/frontend/src/features/auth/api/useCerrarOtrasSesiones.ts`
- `projects/frontend/src/features/auth/api/keys.ts`
- `projects/frontend/src/features/auth/types/sesion.ts`
- `projects/frontend/src/utils/resumirDispositivo.ts`
- `projects/frontend/src/utils/resumirDispositivo.test.ts`
- `projects/frontend/src/shared/api/schema.d.ts`

## Seguridad

A05: el user agent es texto que controla el cliente; se muestra como texto, nunca como HTML. Sin datos personales en `queryKey`.

## Fuera de alcance (no tocar)

- Pantallas y rutas (UT 02).

## Resultado esperado

Los hooks llaman a los tres endpoints y el resumen es legible.

## Validación

1. Tests de `resumirDispositivo` (Chrome, Edge, Firefox, Safari, iPhone, Android, mobile, vacío) y de los hooks con `apiClient` simulado.
2. `pnpm build` y `pnpm lint` en verde.
