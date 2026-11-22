# UT-HUI-OLVIDEPASSWORD-MOB-01 — AuthService.solicitarRecuperacion

| Campo | Valor |
|-------|-------|
| **Código** | `UT-HUI-OLVIDEPASSWORD-MOB-01` |
| **Nombre** | AuthService.solicitarRecuperacion |
| **Historia** | HU-INICIAL-OlvidePassword-MOB |
| **Depende de** | — |

## Antes de implementar

Leer `mobile/CONSUMO_API_MOBILE.md` y `.claude/rules/SEGURIDAD_OWASP.md`.

## Solicitud de negocio

La app debe poder pedir el enlace.

## Objetivo

El service envía el correo al endpoint de recuperación.

## Alcance

- Editar `services/auth_service.dart` agregando solo `solicitarRecuperacion(email)` contra `POST /auth/password/forgot`.

**Archivos esperados (crear/tocar):**

- `projects/mobile/lib/features/auth/services/auth_service.dart`

## Fuera de alcance (no tocar)

- Pantalla y router (UT 02).
- Restablecer (solo web).

## Resultado esperado

El service llama al endpoint y devuelve el mensaje genérico.

## Validación

1. Test con Dio falso: éxito y 429.

## Seguridad

MASVS-PRIVACY: sin PII en logs.
