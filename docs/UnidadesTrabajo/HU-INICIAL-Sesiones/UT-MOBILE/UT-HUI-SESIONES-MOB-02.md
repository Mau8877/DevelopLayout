# UT-HUI-SESIONES-MOB-02 — Pantalla Mis sesiones

| Campo | Valor |
|-------|-------|
| **Código** | `UT-HUI-SESIONES-MOB-02` |
| **Nombre** | Pantalla Mis sesiones |
| **Historia** | HU-INICIAL-Sesiones-MOB |
| **Depende de** | UT-HUI-SESIONES-MOB-01 |

## Antes de implementar

Leer `mobile/ESTADO_GLOBAL_MOBILE.md`, `mobile/COMPONENTIZACION_MOBILE.md`, `mobile/NAVEGACION_MOBILE.md` y `mobile/ERRORES_LOGGING_MOBILE.md`.

## Solicitud de negocio

El usuario debe poder cortar accesos desde el teléfono.

## Objetivo

La app tiene la pantalla «Mis sesiones», accesible desde ambos shells.

## Alcance

- `providers/mis_sesiones_provider.dart`: `AsyncNotifier` (`misSesionesProvider`) con primera página, «cargar más», cerrar una y cerrar las demás; tras cualquier cierre, con éxito o error, vuelve a cargar. **Se descarta al salir de la pantalla y al cambiar de usuario**: no deben quedar sesiones de otra cuenta en memoria.
- `screens/mis_sesiones_screen.dart`: lista con la actual marcada y sin cierre, «Cerrar todas las demás» deshabilitado si no hay otras, confirmación antes de cerrar, `SnackBar` en fallos (`mostrarError`).
- Widgets genéricos que faltan, en `shared/widgets/`: `estado_error.dart` (mensaje y «Reintentar») y `dialogo_confirmacion.dart`.
- Ruta `/mis-sesiones` en `app_router.dart` (no va en las rutas sin sesión) y un botón en el `AppBar` de `UsuarioShell` y `AdminShell`. `layouts/` solo navega.

**Archivos esperados (crear/tocar):**

- `projects/mobile/lib/features/auth/providers/mis_sesiones_provider.dart`
- `projects/mobile/lib/features/auth/screens/mis_sesiones_screen.dart`
- `projects/mobile/lib/features/auth/widgets/sesion_tile.dart` (fila de sesión: conoce el dominio)
- `projects/mobile/lib/shared/widgets/estado_error.dart`
- `projects/mobile/lib/shared/widgets/dialogo_confirmacion.dart`
- `projects/mobile/lib/core/routing/app_router.dart`
- `projects/mobile/lib/layouts/usuario/usuario_shell.dart`
- `projects/mobile/lib/layouts/admin/admin_shell.dart`

## Seguridad

MASVS-AUTH: la autorización es del servidor. Sin datos de sesiones persistidos ni logueados.

## Fuera de alcance (no tocar)

- Prueba en emulador o dispositivo: se hace al final, con el resto de las pruebas mobile.

## Resultado esperado

El usuario ve y cierra sus sesiones desde la app.

## Validación

1. `flutter analyze` limpio y tests de la UT 03 en verde.
