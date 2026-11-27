# UT-HUI-LOGOUT-FE-01 — Hook useLogout y limpieza de caché

| Campo | Valor |
|-------|-------|
| **Código** | `UT-HUI-LOGOUT-FE-01` |
| **Nombre** | Hook useLogout y limpieza de caché |
| **Historia** | HU-INICIAL-Logout-FE |
| **Depende de** | — |

## Antes de implementar

Leer `frontend/TANSTACK_QUERY_FRONTEND.md` y `.claude/rules/SEGURIDAD_OWASP.md`.

## Solicitud de negocio

Al salir no deben quedar datos del usuario en memoria.

## Objetivo

Existe `useLogout` que llama a la API y limpia todo.

## Alcance

- `api/useLogout.ts`: `POST /auth/logout`; en `onSettled` (haya o no respuesta) `queryClient.clear()`, deja la sesión en `null` (para que `/login` no vuelva a consultarla) y navega a `/login`. La petición usa el mismo candado que el refresh, para no cruzarse con una rotación en curso.

**Archivos esperados (crear/tocar):**

- `projects/frontend/src/features/auth/api/useLogout.ts`

## Fuera de alcance (no tocar)

- El botón en la interfaz (UT 02).

## Resultado esperado

Aunque el servidor falle, el cliente queda sin sesión.

## Validación

1. `pnpm test`: éxito y fallo de red limpian la caché.

## Seguridad

A07: nada de datos del usuario tras salir.
