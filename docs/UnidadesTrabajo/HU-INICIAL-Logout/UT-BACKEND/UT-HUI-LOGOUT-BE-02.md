# UT-HUI-LOGOUT-BE-02 — Tests de logout

| Campo | Valor |
|-------|-------|
| **Código** | `UT-HUI-LOGOUT-BE-02` |
| **Nombre** | Tests de logout |
| **Historia** | HU-INICIAL-Logout-BE |
| **Depende de** | UT-HUI-LOGOUT-BE-01 |

## Antes de implementar

Leer `backend/TESTING_BACKEND.md` y `.claude/rules/SEGURIDAD_OWASP.md`.

## Solicitud de negocio

Verificar que el cierre sea efectivo e idempotente.

## Objetivo

Tests que cubren CA01, CA02, CA05 y CA06.

## Alcance

- `AuthLogoutTest` con Postgres (`app_runtime`) y Redis de Testcontainers y verificación del sobre `ApiResponse`.
- Casos: cierre efectivo, access token rechazado de inmediato, idempotencia, CSRF ausente → 403, evento de bitácora sin secretos.

**Archivos esperados (crear/tocar):**

- `projects/backend/src/test/java/com/example/backend/features/auth/AuthLogoutTest.java`

## Fuera de alcance (no tocar)

- Código de producción.

## Resultado esperado

Pasan.

## Validación

1. `./mvnw test` en verde.

## Seguridad

Evidencia de A07.
