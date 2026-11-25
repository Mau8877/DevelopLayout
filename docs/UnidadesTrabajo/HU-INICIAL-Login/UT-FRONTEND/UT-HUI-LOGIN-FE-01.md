# UT-HUI-LOGIN-FE-01 — Cliente HTTP con cookies y CSRF

| Campo | Valor |
|-------|-------|
| **Código** | `UT-HUI-LOGIN-FE-01` |
| **Nombre** | Cliente HTTP con cookies y CSRF |
| **Historia** | HU-INICIAL-Login-FE |
| **Depende de** | — |

## Antes de implementar

Leer `frontend/ERRORES_LOGGING_FRONTEND.md`, `frontend/TANSTACK_QUERY_FRONTEND.md`, `backend/SEGURIDAD_AUTH_BACKEND.md` y `.claude/rules/SEGURIDAD_OWASP.md`.

## Solicitud de negocio

Con tokens solo en cookies, cada request debe enviarlas y adjuntar el token CSRF.

## Objetivo

`apiClient` envía credenciales y el header `X-XSRF-TOKEN`.

## Alcance

- En `createBaseApi`/`apiClient`: `withCredentials: true`, `xsrfCookieName: 'XSRF-TOKEN'`, `xsrfHeaderName: 'X-XSRF-TOKEN'`.
- `ensureCsrfCookie()` en `store/apiClient.ts`: `GET /auth/csrf` una vez al iniciar, llamado desde `main.tsx` antes de montar el router.
- Sigue sin guardar tokens en ningún storage.

- `apiClient` normaliza los fallos sin sobre (sin conexión, timeout, respuesta no JSON, 502/504) a `ApiError` con códigos `CLIENT_NETWORK`, `CLIENT_TIMEOUT`, `CLIENT_UNKNOWN` y mensaje genérico en español.
- El logging de `createBaseApi` redacta `Authorization`, `Cookie`, `X-XSRF-TOKEN` y el body de `/auth/**`.
- `utils/logger.ts`: único punto de log (sin salida `debug`/`info` en producción) y regla `no-console` en ESLint que solo permite el logger.

**Archivos esperados (crear/tocar):**

- `projects/frontend/src/store/createBaseApi.ts`
- `projects/frontend/src/store/apiClient.ts`
- `projects/frontend/src/main.tsx`
- `projects/frontend/src/utils/logger.ts`
- `projects/frontend/eslint.config.js`
- `projects/frontend/src/store/apiClient.test.ts`

## Fuera de alcance (no tocar)

- Interceptor de refresh (HU-INICIAL-Refresh).
- Hooks o pantallas de auth.

## Resultado esperado

Toda request mutadora sale con `X-XSRF-TOKEN` y las cookies se envían a la API.

## Validación

1. `pnpm test` (con axios mockeado) comprueba las opciones y la normalización a `ApiError` (`CLIENT_NETWORK`, `CLIENT_TIMEOUT`, `CLIENT_UNKNOWN`).
2. En Network, un `POST` lleva `X-XSRF-TOKEN` y `Cookie`.

## Seguridad

A01/A07: CSRF cubierto; sin tokens en storage.
