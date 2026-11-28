# UT-HUI-IDIOMAS-MOB-01 — Base de idiomas en la app

| Campo | Valor |
|-------|-------|
| **Código** | `UT-HUI-IDIOMAS-MOB-01` |
| **Nombre** | Base de idiomas en la app |
| **Historia** | HU-INICIAL-Idiomas-MOB |
| **Depende de** | — |

## Antes de implementar

IDIOMAS_MOBILE, ESTADO_GLOBAL_MOBILE y CONSUMO_API_MOBILE.

## Solicitud de negocio

La app sigue la misma convención que la web.

## Objetivo

Carga de JSON, detección, preferencia y envío del idioma.

## Alcance

- `shared/language/`: `app_texts.dart` (carga los JSON de cada espacio, `t(clave, args, count)` con plurales y la extensión `context.t`), `idiomas.dart`, `idioma_provider.dart`, `es.json`, `en.json`.
- `flutter_localizations` (SDK) en `MaterialApp`; `locale` del provider.
- La preferencia se guarda con el almacenamiento de `core/storage/`, sin dependencias nuevas.
- `ApiClient` envía `Accept-Language` en el cliente principal y en el de renovación.

**Archivos esperados (crear/tocar):**

- `projects/mobile/lib/shared/language/app_texts.dart`
- `projects/mobile/lib/shared/language/idiomas.dart`
- `projects/mobile/lib/shared/language/idioma_provider.dart`
- `projects/mobile/lib/shared/language/es.json`
- `projects/mobile/lib/shared/language/en.json`
- `projects/mobile/lib/main.dart`
- `projects/mobile/lib/core/network/api_client.dart`
- `projects/mobile/lib/core/network/refresh_interceptor.dart`
- `projects/mobile/lib/core/storage/secure_token_storage.dart`
- `projects/mobile/pubspec.yaml`
- `projects/mobile/test/shared/language/app_texts_test.dart`

## Seguridad

La preferencia se guarda aparte de los tokens: borrar la sesión no la borra y nunca se mezclan sus claves.

## Fuera de alcance (no tocar)

- Migrar pantallas.

## Resultado esperado

La app sabe en qué idioma está.

## Validación

1. Detección, plurales, marcadores, clave inexistente y preferencia guardada.
2. `Accept-Language` presente en las peticiones y en la renovación.
3. `flutter analyze` y `flutter test` en verde.
