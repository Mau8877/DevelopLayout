# UT-HUI-PRUEBASNAVEGADOR-FE-02 — Flujos de sesión

| Campo | Valor |
|-------|-------|
| **Código** | `UT-HUI-PRUEBASNAVEGADOR-FE-02` |
| **Nombre** | Flujos de sesión |
| **Historia** | HU-INICIAL-PruebasNavegador-FE |
| **Depende de** | 01 |

## Antes de implementar

SEGURIDAD_AUTH_BACKEND y TANSTACK_QUERY_FRONTEND (renovación).

## Solicitud de negocio

Las sesiones dependen de cookies y CSRF, que solo un navegador real comprueba.

## Objetivo

Los flujos de sesión pasan en el navegador.

## Alcance

- Entrar, credenciales incorrectas, ruta protegida sin sesión, recargar con sesión, renovación automática al vencer el access token, cerrar sesión.
- Dos navegadores: cerrar la otra sesión desde «Mis sesiones» y desde la administración; la afectada pierde el acceso.
- Comprobar que ningún token queda en `localStorage`, `sessionStorage` ni es legible por JavaScript.

**Archivos esperados (crear/tocar):**

- `projects/frontend/e2e/sesiones.spec.ts`
- `projects/frontend/e2e/soporte/`

## Seguridad

A07: comprueba en el navegador las cookies `HttpOnly`, CSRF y la revocación.

## Fuera de alcance (no tocar)

- Cambiar el código de la aplicación salvo que una prueba descubra un defecto (se informa).

## Resultado esperado

La sesión está probada de punta a punta.

## Validación

1. Las pruebas pasan sin esperas fijas.
2. Acceso denegado: la sesión cerrada termina en el login.
