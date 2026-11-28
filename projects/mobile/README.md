# Mobile

App en Flutter/Dart con Riverpod (estado), `go_router` (navegación) y `dio`
(cliente HTTP).

## Comandos

```bash
flutter pub get     # instalar dependencias
flutter run          # correr en un emulador/dispositivo conectado
flutter test         # tests
flutter analyze      # análisis estático
```

## Configuración

`pubspec.yaml` declara `.env` como asset y el archivo real no se commitea.
Antes de correr o analizar, creá el tuyo:

```bash
cp .env.example .env
```

Sin ese archivo, `flutter analyze` falla con `asset_does_not_exist`.
`pnpm setup:env` (desde la raíz) también lo crea. `API_BASE_URL` trae la
dirección para el emulador de Android (`10.0.2.2` es tu computadora); en un
dispositivo físico va la IP de tu máquina en la red. En una compilación de
publicación la URL tiene que ser `https://`.

La versión de Flutter es la que fija el CI en `.github/workflows/ci.yml`.

> La app tiene pruebas automáticas, pero **todavía no se ha validado en un
> emulador o dispositivo** dentro de las historias que lo requieren.

## Estructura

Feature-based: `lib/features/{feature}/` por funcionalidad de negocio y
`lib/core/` para lo transversal (`network/` con el cliente `dio` y la
renovación de sesión, `routing/`, `storage/`, `logging/` y `theme/`),
`lib/shared/` (widgets genéricos y textos comunes) y `lib/layouts/{rol}/`, un
shell por rol. El detalle está en
[`ARQUITECTURA_MOBILE.md`](../../.claude/rules/ARQUITECTURA_MOBILE.md) y las
reglas de `.claude/rules/mobile/`.

## Textos e idiomas

Cada feature tiene `language/es.json` y `en.json`; lo común vive en
`lib/shared/language/`. Una carpeta nueva se declara en `pubspec.yaml` y en
`carpetasDeTextos` (`shared/language/app_texts.dart`). Las pruebas rechazan
texto fijo y claves que no existen. Ver
[`IDIOMAS_MOBILE.md`](../../.claude/rules/mobile/IDIOMAS_MOBILE.md).

`pubspec.lock` se commitea para que el build sea reproducible.
