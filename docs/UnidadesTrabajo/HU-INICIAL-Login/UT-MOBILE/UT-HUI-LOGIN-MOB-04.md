# UT-HUI-LOGIN-MOB-04 — authProvider: estado de sesión

| Campo | Valor |
|-------|-------|
| **Código** | `UT-HUI-LOGIN-MOB-04` |
| **Nombre** | authProvider: estado de sesión |
| **Historia** | HU-INICIAL-Login-MOB |
| **Depende de** | UT-HUI-LOGIN-MOB-03 |

## Antes de implementar

Leer `mobile/ESTADO_GLOBAL_MOBILE.md` y `.claude/rules/SEGURIDAD_OWASP.md`.

## Solicitud de negocio

El router y las pantallas necesitan saber si hay sesión.

## Objetivo

Existe `authProvider` (`AsyncNotifier`) con el usuario de la sesión.

## Alcance

- `providers/auth_provider.dart`: `build()` intenta `me()` si hay token; `login()` actualiza el estado; expone `AsyncValue<UsuarioSesion?>`.
- Nombres según la convención (`authProvider`, `loginUiProvider` si hace falta).

**Archivos esperados (crear/tocar):**

- `projects/mobile/lib/features/auth/providers/auth_provider.dart`

## Fuera de alcance (no tocar)

- Pantallas, router, logout.

## Resultado esperado

El estado refleja sesión activa o ausente.

## Validación

1. Test con `ProviderContainer` y service falso: login exitoso y fallido, arranque con y sin token.

## Seguridad

MASVS-AUTH: la sesión se valida contra el backend.
