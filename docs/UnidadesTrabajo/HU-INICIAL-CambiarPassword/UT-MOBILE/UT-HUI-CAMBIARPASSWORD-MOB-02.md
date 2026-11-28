# UT-HUI-CAMBIARPASSWORD-MOB-02 — Pantalla y acceso desde la barra

| Campo | Valor |
|-------|-------|
| **Código** | `UT-HUI-CAMBIARPASSWORD-MOB-02` |
| **Nombre** | Pantalla y acceso desde la barra |
| **Historia** | HU-INICIAL-CambiarPassword-MOB |
| **Depende de** | 01 |

## Antes de implementar

COMPONENTIZACION_MOBILE, FORMULARIOS_MOBILE, NAVEGACION_MOBILE e IDIOMAS_MOBILE.

## Solicitud de negocio

La persona tiene que poder cambiar su contraseña desde el teléfono.

## Objetivo

Pantalla en `/change-password`, accesible desde los dos shells, en español e inglés.

## Alcance

- `CambiarPasswordScreen`: contraseña actual, nueva y confirmación con `AppTextField`; los validadores devuelven claves, con las mismas reglas que la web. Aviso previo de que se cerrarán todas las sesiones. Botón con estado de carga.
- Un error muestra el mensaje con `mensajeDeError` y deja a la persona en la pantalla.
- Al éxito, un `SnackBar` avisa que la contraseña cambió; se toma el `ScaffoldMessenger` antes de llamar, porque la pantalla desaparece al cerrarse la sesión.
- Ruta en `app_router.dart` (exige sesión) y botón con `tooltip` en la barra de los dos shells.

**Archivos esperados (crear/tocar):**

- `projects/mobile/lib/features/auth/screens/cambiar_password_screen.dart`
- `projects/mobile/lib/core/routing/app_router.dart`
- `projects/mobile/lib/layouts/admin/admin_shell.dart`
- `projects/mobile/lib/layouts/usuario/usuario_shell.dart`
- `projects/mobile/lib/features/auth/language/es.json`
- `projects/mobile/lib/features/auth/language/en.json`
- `projects/mobile/test/features/auth/cambiar_password_test.dart`

## Seguridad

MASVS-STORAGE y PLATFORM: campos ocultos, sin sugerencias del teclado y sin registrar lo escrito. La ruta exige sesión.

## Fuera de alcance (no tocar)

- El flujo de recuperación.
- La validación en emulador (se hace al final del proyecto).

## Resultado esperado

La app permite cambiar la contraseña.

## Validación

1. Validaciones y envío único.
2. Error del servidor: se muestra su mensaje y la sesión sigue.
3. Éxito: termina en el login con el aviso.
4. La pantalla se prueba en español y en inglés; los botones existen en los dos shells.
5. Acceso denegado: sin sesión, la ruta lleva al login.
6. `flutter analyze` y `flutter test` en verde.
