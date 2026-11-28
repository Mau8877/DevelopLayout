---
globs: projects/mobile/**/*
---

# Idiomas — Mobile

Complementa a [IDIOMAS.md](../IDIOMAS.md), que manda sobre **qué** se traduce y
cómo se escribe. Misma convención que la web
([IDIOMAS_FRONTEND.md](../frontend/IDIOMAS_FRONTEND.md)): archivos JSON por
feature.

## Dónde viven los textos

```
lib/
├── features/
│   ├── auth/language/       es.json  en.json
│   └── health/language/     es.json  en.json
└── shared/
    └── language/            es.json  en.json     <- textos comunes
        ├── app_texts.dart   <- carga, búsqueda y plurales
        ├── idiomas.dart     <- idiomas soportados y detección
        └── idioma_provider.dart
```

- Se cargan como **assets**. Cada carpeta `language/` se declara en
  `pubspec.yaml` y su espacio de nombres se registra en `app_texts.dart` (una
  lista de nombres: `shared/` **no importa código** de ninguna feature).
- Una feature no usa las claves de otra. Lo común va en `shared/language/`
  (layouts, `core/`, widgets genéricos).
- No se usa `gen-l10n`/`.arb`: la convención del proyecto es el mismo JSON que en
  la web. `flutter_localizations` (del SDK) sí se usa, para los textos propios de
  los widgets de Material.

## Uso

```dart
Text(context.t('auth:login.titulo'))
Text(context.t('auth:sesiones.cantidad', count: total))
```

- Plurales: claves `_one` y `_other` con `count`, igual que en la web.
- **Una clave que no existe falla en las pruebas** y en modo debug; en release
  se muestra la clave, nunca una pantalla rota.
- Providers, services y `core/` no devuelven frases: devuelven **claves** o el
  mensaje que ya vino del servidor, y la pantalla traduce.
- Las reglas de validación devuelven claves y se pasan al campo con
  `context.validador(...)`. Un mensaje que puede ser clave o texto del servidor
  se muestra con `context.traducir(...)`.
- Los widgets de `shared/widgets/` reciben textos por parámetro; sus valores por
  defecto salen de `shared`.

## Selección y API

- El idioma se detecta del sistema y se puede cambiar con el selector, también en
  las pantallas sin sesión. La preferencia se guarda con el almacenamiento que ya
  tiene la app (`core/storage/`), sin dependencias nuevas para esto.
- `ApiClient` manda `Accept-Language` en cada petición, también en el cliente
  auxiliar de renovación. Los mensajes de `ApiException` llegan traducidos; los
  propios del cliente salen de `shared`.
- Fechas y números usan el idioma vigente.

## Control automático (pruebas)

- Mismas claves y marcadores en todos los idiomas.
- Cada clave usada en `lib/` existe, y cada carpeta `language/` está declarada en
  `pubspec.yaml` y en `app_texts.dart`.
- Sin texto fijo en `Text(...)`, `labelText`, `hintText`, `tooltip`,
  `semanticLabel`, `SnackBar` ni `title`.
- Sin voseo en los `es.json`.
