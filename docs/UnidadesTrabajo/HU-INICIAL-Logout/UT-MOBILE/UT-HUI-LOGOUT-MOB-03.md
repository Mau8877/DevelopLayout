# UT-HUI-LOGOUT-MOB-03 — Tests de logout mobile

| Campo | Valor |
|-------|-------|
| **Código** | `UT-HUI-LOGOUT-MOB-03` |
| **Nombre** | Tests de logout mobile |
| **Historia** | HU-INICIAL-Logout-MOB |
| **Depende de** | UT-HUI-LOGOUT-MOB-02 |

## Antes de implementar

Leer `mobile/TESTING_MOBILE.md` y `.claude/rules/SEGURIDAD_OWASP.md`.

## Solicitud de negocio

Lógica crítica de sesión.

## Objetivo

Tests de `logout`.

## Alcance

- Agregar casos de logout a `test/features/auth/`.

**Archivos esperados (crear/tocar):**

- `projects/mobile/test/features/auth/logout_test.dart`

## Fuera de alcance (no tocar)

- Código de producción.

## Resultado esperado

Pasan.

## Validación

1. `flutter test` en verde.

## Seguridad

Cubre CA03 y CA04.
