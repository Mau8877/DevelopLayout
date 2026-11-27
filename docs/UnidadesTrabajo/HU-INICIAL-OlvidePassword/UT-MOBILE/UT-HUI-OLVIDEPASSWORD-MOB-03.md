# UT-HUI-OLVIDEPASSWORD-MOB-03 — Tests de recuperación mobile

| Campo | Valor |
|-------|-------|
| **Código** | `UT-HUI-OLVIDEPASSWORD-MOB-03` |
| **Nombre** | Tests de recuperación mobile |
| **Historia** | HU-INICIAL-OlvidePassword-MOB |
| **Depende de** | UT-HUI-OLVIDEPASSWORD-MOB-02 |

## Antes de implementar

Leer `mobile/TESTING_MOBILE.md` y `.claude/rules/SEGURIDAD_OWASP.md`.

## Solicitud de negocio

Lógica crítica de recuperación.

## Objetivo

Tests del service, del validador y de la redirección.

## Alcance

- `test/features/auth/forgot_password_test.dart`: servicio (éxito y 429), validación del correo y `redirectSegunSesion` con `/forgot-password` en los tres estados (sin sesión se queda; con sesión va a inicio; cargando va a la carga).

**Archivos esperados (crear/tocar):**

- `projects/mobile/test/features/auth/forgot_password_test.dart`

## Fuera de alcance (no tocar)

- Código de producción.

## Resultado esperado

Pasan.

## Validación

1. `flutter test` en verde.

## Seguridad

Cubre CA01 y CA06 del lado mobile.
