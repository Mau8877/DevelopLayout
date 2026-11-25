# UT-HUI-LOGIN-MOB-05 — LoginScreen

| Campo | Valor |
|-------|-------|
| **Código** | `UT-HUI-LOGIN-MOB-05` |
| **Nombre** | LoginScreen |
| **Historia** | HU-INICIAL-Login-MOB |
| **Depende de** | UT-HUI-LOGIN-MOB-04 |

## Antes de implementar

Leer `mobile/COMPONENTIZACION_MOBILE.md`, `mobile/ERRORES_LOGGING_MOBILE.md`, `mobile/FORMULARIOS_MOBILE.md`, `mobile/WIDGETS_UI_MOBILE.md` y `.claude/rules/SEGURIDAD_OWASP.md`.

## Solicitud de negocio

Es la puerta de entrada en la app.

## Objetivo

Existe `LoginScreen` con validación y manejo de errores.

## Alcance

- `screens/login_screen.dart`: `Form` + `TextFormField`; correo válido y contraseña mínimo 8 (misma regla que el web); `obscureText`; botón deshabilitado mientras envía.
- Error genérico igual para ambos fallos; aviso de bloqueo en 423.
- Sin datos sensibles en logs.

- Widgets genéricos nuevos en `lib/shared/widgets/` (carpeta nueva): `app_text_field.dart` (campo con label y error del tema) y `app_button.dart` (botón con estado de carga). Sin conocimiento de auth. `LoginScreen` solo los compone.
- Errores de la mutación mostrados con `SnackBar` usando `mensajeDeError`.

**Archivos esperados (crear/tocar):**

- `projects/mobile/lib/features/auth/screens/login_screen.dart`
- `projects/mobile/lib/shared/widgets/app_text_field.dart`
- `projects/mobile/lib/shared/widgets/app_button.dart`

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
