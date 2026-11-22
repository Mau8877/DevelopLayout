# UT-HUI-BITACORA-BE-03 — Tests de consulta de bitácora

| Campo | Valor |
|-------|-------|
| **Código** | `UT-HUI-BITACORA-BE-03` |
| **Nombre** | Tests de consulta de bitácora |
| **Historia** | HU-INICIAL-Bitacora-BE |
| **Depende de** | UT-HUI-BITACORA-BE-02 |

## Antes de implementar

Leer `backend/TESTING_BACKEND.md` y `.claude/rules/SEGURIDAD_OWASP.md`.

## Solicitud de negocio

Probar autorización, filtros y que no se filtran secretos.

## Objetivo

Tests de CA01–CA06.

## Alcance

- `BitacoraControllerTest` con verificación del sobre `ApiResponse`: orden, paginación, cada filtro, 400 de rango inválido, 403 para `USUARIO`, y un test que recorre eventos de login, refresh y recuperación comprobando que `detalle` no contiene contraseñas, tokens ni hashes.

**Archivos esperados (crear/tocar):**

- `projects/backend/src/test/java/com/example/backend/features/bitacora/BitacoraControllerTest.java`

## Fuera de alcance (no tocar)

- Código de producción.

## Resultado esperado

Pasan.

## Validación

1. `./mvnw test` en verde.

## Seguridad

Evidencia de A09 y API5.
