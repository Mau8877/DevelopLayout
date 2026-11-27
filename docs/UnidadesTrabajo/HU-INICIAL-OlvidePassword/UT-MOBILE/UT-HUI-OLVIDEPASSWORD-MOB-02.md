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

- `screens/forgot_password_screen.dart`: correo con validación, botón deshabilitado mientras envía, mensaje uniforme tras un 200 («si la cuenta existe, te enviamos un enlace»), aviso en 429 y mensaje de `mensajeDeError` en los demás fallos. Usa los widgets de `shared/widgets/`.
- Texto que indica que el enlace se abre en el navegador (sin deep links).
- Editar `login_screen.dart` para el enlace y `app_router.dart` para la ruta `/forgot-password`. **`redirectSegunSesion` hoy manda a `/login` cualquier otra ruta sin sesión:** debe permitir también `/forgot-password` sin sesión; con sesión redirige a inicio y, mientras carga, a la pantalla de carga.

**Archivos esperados (crear/tocar):**

- `projects/mobile/lib/features/auth/screens/forgot_password_screen.dart`
- `projects/mobile/lib/features/auth/screens/login_screen.dart`
- `projects/mobile/lib/core/routing/app_router.dart`
- `projects/mobile/lib/shared/widgets/error_snack_bar.dart` (aviso de error compartido con el login)

## Fuera de alcance (no tocar)

- Pantalla de restablecer (es web).
- Deep links.

## Resultado esperado

El usuario pide el enlace desde la app.

## Validación

1. Manual con Mailpit; `flutter analyze` limpio.

## Seguridad

MASVS-PLATFORM: sin deep links que ejecuten acciones.
