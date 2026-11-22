# UT-HUI-REFRESH-MOB-02 — Tests del refresh mobile

| Campo | Valor |
|-------|-------|
| **Código** | `UT-HUI-REFRESH-MOB-02` |
| **Nombre** | Tests del refresh mobile |
| **Historia** | HU-INICIAL-Refresh-MOB |
| **Depende de** | UT-HUI-REFRESH-MOB-01 |

## Antes de implementar

Leer `mobile/TESTING_MOBILE.md` y `.claude/rules/SEGURIDAD_OWASP.md`.

## Solicitud de negocio

Lógica crítica de sesión.

## Objetivo

Tests del interceptor.

## Alcance

- `test/core/network/refresh_interceptor_test.dart`.

**Archivos esperados (crear/tocar):**

- `projects/mobile/test/core/network/refresh_interceptor_test.dart`

## Fuera de alcance (no tocar)

- Código de producción.

## Resultado esperado

Pasan.

## Validación

1. `flutter test` en verde.

## Seguridad

Cubre CA01, CA02 y CA04.
