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

## Estructura

Feature-based: `lib/features/{feature}/` por funcionalidad de negocio y
`lib/core/` para lo transversal (`network/` con el cliente `dio` y la
renovación de sesión, `routing/`, `storage/`, `logging/` y `theme/`). `lib/layouts/{rol}/` tiene un shell por
rol. El detalle está en
[`ARQUITECTURA_MOBILE.md`](../../.claude/rules/ARQUITECTURA_MOBILE.md) y las
reglas de `.claude/rules/mobile/`.

`pubspec.lock` se commitea para que el build sea reproducible.
