# UT-HUI-LOGIN-MOB-03 — Modelos y AuthService (login y me)

| Campo | Valor |
|-------|-------|
| **Código** | `UT-HUI-LOGIN-MOB-03` |
| **Nombre** | Modelos y AuthService (login y me) |
| **Historia** | HU-INICIAL-Login-MOB |
| **Depende de** | UT-HUI-LOGIN-MOB-01, UT-HUI-LOGIN-MOB-02 |

## Antes de implementar

Leer `mobile/CONSUMO_API_MOBILE.md` y `.claude/rules/SEGURIDAD_OWASP.md`.

## Solicitud de negocio

La app necesita llamar al login y a `/me` por el cliente común.

## Objetivo

Existen los modelos y el service de login.

## Alcance

- `models/usuario_sesion.dart` y `models/login_response.dart` con `fromJson`.
- `services/auth_service.dart`: `login(email, password)` y `me()`; guarda access y refresh en `SecureTokenStorage` al éxito.

**Archivos esperados (crear/tocar):**

- `projects/mobile/lib/features/auth/models/usuario_sesion.dart`
- `projects/mobile/lib/features/auth/models/login_response.dart`
- `projects/mobile/lib/features/auth/services/auth_service.dart`

## Fuera de alcance (no tocar)

- Providers y pantallas.
- Refresh y logout.

## Resultado esperado

El service devuelve el usuario y deja los tokens guardados.

## Validación

1. Tests de parsing y de `AuthService` con Dio falso (éxito y `ApiException`).

## Seguridad

MASVS-STORAGE/PRIVACY: sin credenciales ni tokens en logs.
