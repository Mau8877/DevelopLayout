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

- En `store/apiClient.ts`: interceptor de respuesta que ante 401 (excepto en `/auth/login`, `/auth/refresh` y `/auth/logout`) llama a `POST /auth/refresh` con una promesa compartida y reintenta la request original una sola vez. Un 401 de una request enviada antes del último refresh se reintenta sin renovar otra vez.
- El refresh se ejecuta bajo un candado entre pestañas (`store/candadoSesion.ts`): `navigator.locks` y, sin soporte (navegador viejo o `http://` fuera de localhost), una reserva con vencimiento en `localStorage` que guarda solo un id al azar y una hora, **nunca tokens**. La reserva no es atómica: deja una ventana mínima.
- Solo un **401 o 403** del refresh avisa que la sesión expiró. Sin conexión, 429 o 500 se propaga ese error y la sesión se conserva.
- `features/auth/api/manejarSesionExpirada.ts`: si había sesión en caché, limpia el `queryClient`, deja la sesión en `null` y navega a `/login` solo si no está ya ahí. Sin sesión previa no hace nada (el guard redirige): así no hay bucle. Se conecta en `main.tsx`.
- `ensureSession` consulta con `retry: false`: repetir un 401 gasta otro refresh de la cuota por IP.
- Sin tocar tokens: son cookies.

**Archivos esperados (crear/tocar):**

- `projects/frontend/src/store/apiClient.ts`
- `projects/frontend/src/store/candadoSesion.ts` (y su test)
- `projects/frontend/src/features/auth/api/manejarSesionExpirada.ts`
- `projects/frontend/src/features/auth/index.ts`
- `projects/frontend/src/main.tsx`

## Fuera de alcance (no tocar)

- Pantallas o hooks de auth.
- Logout.

## Resultado esperado

Varios 401 simultáneos producen un solo `POST /auth/refresh`.

## Validación

1. Test: 3 requests en paralelo con 401 → 1 refresh y 3 reintentos.
2. Test: refresh 401 → sesión limpia y redirect a `/login`; fallo de red o 500 → sesión intacta.

## Seguridad

A07: no hay bucles de reintento.
