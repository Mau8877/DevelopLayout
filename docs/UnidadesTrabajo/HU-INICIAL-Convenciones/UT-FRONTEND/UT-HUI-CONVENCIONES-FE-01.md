# UT-HUI-CONVENCIONES-FE-01 — Rutas de la web y llamadas en inglés

| Campo | Valor |
|-------|-------|
| **Código** | `UT-HUI-CONVENCIONES-FE-01` |
| **Nombre** | Rutas de la web y llamadas en inglés |
| **Historia** | HU-INICIAL-Convenciones-FE |
| **Depende de** | — (se integra junto con la UT BE-01) |

## Antes de implementar

RUTAS_NAVEGACION_FRONTEND y TANSTACK_QUERY_FRONTEND.

## Solicitud de negocio

La web usa las direcciones nuevas, las suyas y las de la API.

## Objetivo

URL de la web en inglés y llamadas al contrato nuevo.

## Alcance

| Antes | Después |
|---|---|
| `/panel` | `/dashboard` |
| `/mis-sesiones` | `/my-sessions` |
| `/sesiones-usuarios` | `/user-sessions` |
| `/bitacora` | `/audit-log` |
| `/usuarios` | `/users` |

- Renombrar los archivos de `routes/`, regenerar `routeTree.gen.ts` y actualizar guards, redirecciones, menús y pruebas.
- Quitar del menú los enlaces de ejemplo sin pantalla (`/pedidos`, `/perfil`).
- Llamadas y parámetros a la API nueva; `pnpm openapi:types`.
- Actualizar las reglas que describen las rutas actuales de la web.

**Archivos esperados (crear/tocar):**

- `projects/frontend/src/routes/`
- `projects/frontend/src/routeTree.gen.ts`
- `projects/frontend/src/layouts/`
- `projects/frontend/src/features/`
- `projects/frontend/src/shared/api/schema.d.ts`
- `.claude/rules/`

## Seguridad

El guard de administrador sigue cubriendo las mismas pantallas. El texto buscado sigue fuera de la URL.

## Fuera de alcance (no tocar)

- Cambiar textos visibles (historia de idiomas).
- Barriles (UT FE-02).

## Resultado esperado

La web navega y consume la API en inglés.

## Validación

1. Un `USUARIO` en `/users`, `/user-sessions` y `/audit-log` termina en `/dashboard`.
2. Una URL anterior muestra la página de no encontrado.
3. Ningún enlace del menú lleva a una pantalla inexistente.
4. `pnpm exec vite build`, `pnpm build`, `pnpm lint` y `pnpm test` en verde.
5. Con la UT BE-01: `pnpm test:prod` en verde.
