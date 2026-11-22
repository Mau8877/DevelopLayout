# UT-HUI-OLVIDEPASSWORD-BE-06 — Tests de recuperación de contraseña

| Campo | Valor |
|-------|-------|
| **Código** | `UT-HUI-OLVIDEPASSWORD-BE-06` |
| **Nombre** | Tests de recuperación de contraseña |
| **Historia** | HU-INICIAL-OlvidePassword-BE |
| **Depende de** | UT-HUI-OLVIDEPASSWORD-BE-05 |

## Antes de implementar

Leer `backend/TESTING_BACKEND.md` y `.claude/rules/SEGURIDAD_OWASP.md`.

## Solicitud de negocio

Probar uniformidad, caducidad y revocación.

## Objetivo

Tests que cubren CA01–CA07, CA09 y CA10.

## Alcance

- `AuthPasswordResetTest` (Testcontainers Postgres con `app_runtime` + Redis) con verificación del sobre `ApiResponse`.
- Casos de uniformidad de respuesta y tiempo, un solo uso, invalidación del token anterior, 429, cierre de sesiones, evento `INFO` sin secretos.

**Archivos esperados (crear/tocar):**

- `projects/backend/src/test/java/com/example/backend/features/auth/AuthPasswordResetTest.java`

## Fuera de alcance (no tocar)

- Código de producción.

## Resultado esperado

Pasan.

## Validación

1. `./mvnw test` en verde.

## Seguridad

Evidencia de A07/API6/A09.
