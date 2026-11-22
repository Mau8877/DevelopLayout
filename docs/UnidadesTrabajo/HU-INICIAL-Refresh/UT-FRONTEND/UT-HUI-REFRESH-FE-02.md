# UT-HUI-REFRESH-FE-02 — Tests del interceptor

| Campo | Valor |
|-------|-------|
| **Código** | `UT-HUI-REFRESH-FE-02` |
| **Nombre** | Tests del interceptor |
| **Historia** | HU-INICIAL-Refresh-FE |
| **Depende de** | UT-HUI-REFRESH-FE-01 |

## Antes de implementar

Leer `frontend/TESTING_FRONTEND.md` y `.claude/rules/SEGURIDAD_OWASP.md`.

## Solicitud de negocio

Evitar regresiones en el manejo de sesión vencida.

## Objetivo

Tests del interceptor.

## Alcance

- `store/apiClient.refresh.test.ts` cubre concurrencia, reintento único y fallo.

**Archivos esperados (crear/tocar):**

- `projects/frontend/src/store/apiClient.refresh.test.ts`

## Fuera de alcance (no tocar)

- Código de producción.

## Resultado esperado

Todos pasan.

## Validación

1. `pnpm test` en verde.

## Seguridad

Cubre CA02 y CA04 del lado cliente.
