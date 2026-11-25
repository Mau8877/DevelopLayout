# UT-HUI-LOGIN-MOB-07 — Tests de login mobile

| Campo | Valor |
|-------|-------|
| **Código** | `UT-HUI-LOGIN-MOB-07` |
| **Nombre** | Tests de login mobile |
| **Historia** | HU-INICIAL-Login-MOB |
| **Depende de** | UT-HUI-LOGIN-MOB-06 |

## Antes de implementar

Leer `mobile/TESTING_MOBILE.md` y `.claude/rules/SEGURIDAD_OWASP.md`.

## Solicitud de negocio

Los tests de lógica crítica evitan regresiones en sesión.

## Objetivo

Quedan tests de parsing, service, provider y redirect.

## Alcance

- Reunir en `test/features/auth/` los tests de UT 01–04 y agregar el test de `redirect` con provider simulado.

- Incluir el test de `mensajeDeError` y de la normalización de errores del interceptor.

**Archivos esperados (crear/tocar):**

- `projects/mobile/test/features/auth/app_router_redirect_test.dart`

## Fuera de alcance (no tocar)

- Tests de widgets triviales (la rule no los exige).

## Resultado esperado

Todos los tests pasan.

## Validación

1. `flutter test` y `flutter analyze` en verde.

## Seguridad

Cubre CA01, CA02 y CA06 del lado mobile.
