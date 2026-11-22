# UT-HUI-LOGIN-MOB-06 — GoRouter con redirect global y arranque

| Campo | Valor |
|-------|-------|
| **Código** | `UT-HUI-LOGIN-MOB-06` |
| **Nombre** | GoRouter con redirect global y arranque |
| **Historia** | HU-INICIAL-Login-MOB |
| **Depende de** | UT-HUI-LOGIN-MOB-05 |

## Antes de implementar

Leer `mobile/NAVEGACION_MOBILE.md` y `.claude/rules/SEGURIDAD_OWASP.md`.

## Solicitud de negocio

El chequeo de sesión debe vivir en un solo lugar.

## Objetivo

Existe el `GoRouter` central con `redirect` según `authProvider` y `main.dart` lo usa.

## Alcance

- `core/routing/app_router.dart` con rutas `/login` y `/` y `redirect` (sin sesión → `/login`; con sesión en `/login` → `/`). El router depende del provider, no al revés.
- `main.dart`: `MaterialApp.router` con ese router.
- Pantalla inicial mínima por rol usando los shells existentes.

**Archivos esperados (crear/tocar):**

- `projects/mobile/lib/core/routing/app_router.dart`
- `projects/mobile/lib/main.dart`

## Fuera de alcance (no tocar)

- Rutas de otras features.
- Logout.

## Resultado esperado

Sin sesión la app muestra login; con sesión muestra su pantalla.

## Validación

1. Sin token → login; tras login → home; reiniciar la app mantiene la sesión.
2. `flutter analyze` limpio.

## Seguridad

MASVS-AUTH: el estado de sesión viene del backend.
