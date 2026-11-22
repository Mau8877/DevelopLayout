# UT-HUI-REFRESH-FE-01 — Interceptor de refresh con una sola llamada concurrente

| Campo | Valor |
|-------|-------|
| **Código** | `UT-HUI-REFRESH-FE-01` |
| **Nombre** | Interceptor de refresh con una sola llamada concurrente |
| **Historia** | HU-INICIAL-Refresh-FE |
| **Depende de** | — |

## Antes de implementar

Leer `frontend/TANSTACK_QUERY_FRONTEND.md` y `.claude/rules/SEGURIDAD_OWASP.md`.

## Solicitud de negocio

Si varias peticiones fallan a la vez por token vencido, solo debe haber un refresh.

## Objetivo

`apiClient` renueva la sesión una vez y reintenta las peticiones fallidas.

## Alcance

- En `store/apiClient.ts`: interceptor de respuesta que ante 401 (excepto en `/auth/login` y `/auth/refresh`) llama a `POST /auth/refresh` con una promesa compartida, reintenta la request original una sola vez, y si el refresh falla limpia la sesión (`queryClient`) y redirige a `/login`.
- Sin tocar tokens: son cookies.

**Archivos esperados (crear/tocar):**

- `projects/frontend/src/store/apiClient.ts`

## Fuera de alcance (no tocar)

- Pantallas o hooks de auth.
- Logout.

## Resultado esperado

Varios 401 simultáneos producen un solo `POST /auth/refresh`.

## Validación

1. Test: 3 requests en paralelo con 401 → 1 refresh y 3 reintentos.
2. Test: refresh 401 → sesión limpia y redirect a `/login`.

## Seguridad

A07: no hay bucles de reintento.
