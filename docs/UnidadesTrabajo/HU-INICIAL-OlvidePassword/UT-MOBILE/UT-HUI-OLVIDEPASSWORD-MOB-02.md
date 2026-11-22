# UT-HUI-OLVIDEPASSWORD-MOB-02 — Pantalla Olvidé mi contraseña

| Campo | Valor |
|-------|-------|
| **Código** | `UT-HUI-OLVIDEPASSWORD-MOB-02` |
| **Nombre** | Pantalla Olvidé mi contraseña |
| **Historia** | HU-INICIAL-OlvidePassword-MOB |
| **Depende de** | UT-HUI-OLVIDEPASSWORD-MOB-01 |

## Antes de implementar

Leer `mobile/FORMULARIOS_MOBILE.md`, `mobile/NAVEGACION_MOBILE.md` y `.claude/rules/SEGURIDAD_OWASP.md`.

## Solicitud de negocio

El usuario debe poder pedir el enlace desde la app.

## Objetivo

Existe `ForgotPasswordScreen` con enlace desde el login y ruta en el router.

## Alcance

- `screens/forgot_password_screen.dart` (correo con validación, mensaje uniforme, aviso en 429); editar `login_screen.dart` para el enlace y `app_router.dart` para la ruta `/forgot-password` (pública en el `redirect`).
- Texto que indica que el enlace se abre en el navegador (sin deep links).

**Archivos esperados (crear/tocar):**

- `projects/mobile/lib/features/auth/screens/forgot_password_screen.dart`
- `projects/mobile/lib/features/auth/screens/login_screen.dart`
- `projects/mobile/lib/core/routing/app_router.dart`

## Fuera de alcance (no tocar)

- Pantalla de restablecer (es web).
- Deep links.

## Resultado esperado

El usuario pide el enlace desde la app.

## Validación

1. Manual con Mailpit; `flutter analyze` limpio.

## Seguridad

MASVS-PLATFORM: sin deep links que ejecuten acciones.
