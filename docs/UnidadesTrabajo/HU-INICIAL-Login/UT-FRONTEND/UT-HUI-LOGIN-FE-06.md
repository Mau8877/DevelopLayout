# UT-HUI-LOGIN-FE-06 — Tests de la pantalla y la sesión

| Campo | Valor |
|-------|-------|
| **Código** | `UT-HUI-LOGIN-FE-06` |
| **Nombre** | Tests de la pantalla y la sesión |
| **Historia** | HU-INICIAL-Login-FE |
| **Depende de** | UT-HUI-LOGIN-FE-05 |

## Antes de implementar

Leer `frontend/TESTING_FRONTEND.md` y `.claude/rules/SEGURIDAD_OWASP.md`.

## Solicitud de negocio

Evita regresiones en el flujo de entrada.

## Objetivo

Hay tests de `LoginScreen` y del guard.

## Alcance

- `tests/LoginScreen.test.tsx`: errores inline, envío válido llama a `useLogin`, error genérico visible.
- `tests/authGuard.test.ts`: redirige sin sesión.

**Archivos esperados (crear/tocar):**

- `projects/frontend/src/features/auth/tests/LoginScreen.test.tsx`
- `projects/frontend/src/features/auth/tests/authGuard.test.ts`

## Fuera de alcance (no tocar)

- Código de producción.

## Resultado esperado

Todos los tests pasan.

## Validación

1. `pnpm test` y `pnpm lint` en verde.

## Seguridad

Cubre CA05, CA07 y CA08 del lado cliente.
