# UT-HUI-LOGOUT-MOB-02 — Acción cerrar sesión en los shells

| Campo | Valor |
|-------|-------|
| **Código** | `UT-HUI-LOGOUT-MOB-02` |
| **Nombre** | Acción cerrar sesión en los shells |
| **Historia** | HU-INICIAL-Logout-MOB |
| **Depende de** | UT-HUI-LOGOUT-MOB-01 |

## Antes de implementar

Leer `mobile/WIDGETS_UI_MOBILE.md` y `.claude/rules/SEGURIDAD_OWASP.md`.

## Solicitud de negocio

Debe haber una forma visible de salir.

## Objetivo

`AdminShell` y `UsuarioShell` ofrecen "Cerrar sesión".

## Alcance

- Agregar la acción en ambos shells llamando a `authProvider.logout()`; el redirect global lleva a `/login`.

**Archivos esperados (crear/tocar):**

- `projects/mobile/lib/layouts/admin/admin_shell.dart`
- `projects/mobile/lib/layouts/usuario/usuario_shell.dart`
- `projects/mobile/test/widget_test.dart`

## Fuera de alcance (no tocar)

- Otras opciones de menú.

## Resultado esperado

Al pulsar se vuelve a `/login`.

## Validación

1. Manual en emulador; `flutter analyze` limpio.

## Seguridad

MASVS-AUTH.
