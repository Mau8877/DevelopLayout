# UT-HUI-REFRESH-BE-03 — Tests de renovación

| Campo | Valor |
|-------|-------|
| **Código** | `UT-HUI-REFRESH-BE-03` |
| **Nombre** | Tests de renovación |
| **Historia** | HU-INICIAL-Refresh-BE |
| **Depende de** | UT-HUI-REFRESH-BE-02 |

## Antes de implementar

Leer `backend/TESTING_BACKEND.md` y `.claude/rules/SEGURIDAD_OWASP.md`.

## Solicitud de negocio

Probar rotación, reuso y fallo cerrado.

## Objetivo

Tests que cubren CA01–CA07 de la HU.

## Alcance

- `AuthRefreshTest`: rotación OK web y mobile, token inválido, reutilización, usuario desactivado, Redis caído → 500 genérico, CSRF ausente → 403, verificación del sobre `ApiResponse`.

**Archivos esperados (crear/tocar):**

- `projects/backend/src/test/java/com/example/backend/features/auth/AuthRefreshTest.java`

## Fuera de alcance (no tocar)

- Código de producción.

## Resultado esperado

Todos pasan.

## Validación

1. `./mvnw test` en verde.

## Seguridad

Evidencia de A07/A10.
