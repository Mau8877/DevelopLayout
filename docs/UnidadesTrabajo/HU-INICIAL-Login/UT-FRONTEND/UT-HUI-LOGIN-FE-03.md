# UT-HUI-LOGIN-FE-03 — Hooks de sesión: useLogin y useSession

| Campo | Valor |
|-------|-------|
| **Código** | `UT-HUI-LOGIN-FE-03` |
| **Nombre** | Hooks de sesión: useLogin y useSession |
| **Historia** | HU-INICIAL-Login-FE |
| **Depende de** | UT-HUI-LOGIN-FE-01, UT-HUI-LOGIN-FE-02 |

## Antes de implementar

Leer `frontend/ERRORES_LOGGING_FRONTEND.md`, `frontend/TANSTACK_QUERY_FRONTEND.md`, `frontend/ESTADO_GLOBAL_FRONTEND.md` y `.claude/rules/SEGURIDAD_OWASP.md`.

## Solicitud de negocio

La app necesita iniciar sesión y saber si hay sesión vigente sin copiar el usuario a un store.

## Objetivo

Existen `useLogin` (mutation) y `useSession` (query a `/auth/me`).

## Alcance

- `api/keys.ts` con `authKeys` (`all: ['auth']`, `session: ['auth','session']`).
- `api/useSession.ts`: `GET /auth/me`, `retry: false`; el 401 significa "sin sesión".
- `api/useLogin.ts`: `POST /auth/login`; al éxito escribe el usuario en la query de sesión.
- El usuario de sesión vive en TanStack Query, no en Zustand.

**Archivos esperados (crear/tocar):**

- `projects/frontend/src/features/auth/api/keys.ts`
- `projects/frontend/src/features/auth/api/useSession.ts`
- `projects/frontend/src/features/auth/api/useLogin.ts`

## Fuera de alcance (no tocar)

- Pantalla de login y guard.
- Logout y refresh.

## Resultado esperado

Los hooks consultan y actualizan la sesión con `apiClient`.

## Validación

1. `pnpm test`: `useSession` y `useLogin` con `apiClient` mockeado (éxito y 401).

## Seguridad

A07: la sesión la decide siempre el backend.
