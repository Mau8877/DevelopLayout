# UT-HUI-LOGIN-MOB-05 — LoginScreen

| Campo | Valor |
|-------|-------|
| **Código** | `UT-HUI-LOGIN-MOB-05` |
| **Nombre** | LoginScreen |
| **Historia** | HU-INICIAL-Login-MOB |
| **Depende de** | UT-HUI-LOGIN-MOB-04 |

## Antes de implementar

Leer `mobile/FORMULARIOS_MOBILE.md`, `mobile/WIDGETS_UI_MOBILE.md` y `.claude/rules/SEGURIDAD_OWASP.md`.

## Solicitud de negocio

Es la puerta de entrada en la app.

## Objetivo

Existe `LoginScreen` con validación y manejo de errores.

## Alcance

- `screens/login_screen.dart`: `Form` + `TextFormField`; correo válido y contraseña mínimo 8 (misma regla que el web); `obscureText`; botón deshabilitado mientras envía.
- Error genérico igual para ambos fallos; aviso de bloqueo en 423.
- Sin datos sensibles en logs.

**Archivos esperados (crear/tocar):**

- `projects/mobile/lib/features/auth/screens/login_screen.dart`

## Fuera de alcance (no tocar)

- Router y redirect (UT 06).
- Enlace de olvidé contraseña (otra HU).

## Resultado esperado

El usuario envía credenciales y ve los errores.

## Validación

1. `flutter run` contra el backend: login correcto y fallido.
2. `flutter analyze` limpio.

## Seguridad

MASVS-PRIVACY: sin PII en logs; MASVS-PLATFORM: sin deep links.
