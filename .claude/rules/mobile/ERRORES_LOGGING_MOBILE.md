---
globs: projects/mobile/**/*
---

# Errores y logging — Mobile

Complementa [SEGURIDAD_OWASP.md](../SEGURIDAD_OWASP.md) (A09/A10, MASVS PRIVACY/
STORAGE) y [CONSUMO_API_MOBILE.md](CONSUMO_API_MOBILE.md). Ninguna pantalla
muestra un error crudo, y ningún `catch` se traga un error en silencio.

## Un solo tipo de error hacia la UI: `ApiException`

- El interceptor de `ApiClient` convierte el sobre `failed` en `ApiException`
  (`code` + `message`). **Fallos sin sobre** (sin conexión, timeout, respuesta
  no JSON, `502/504`) llegan como `ApiException` con códigos propios del
  cliente (`CLIENT_NETWORK`, `CLIENT_TIMEOUT`, `CLIENT_UNKNOWN`) y mensaje
  genérico en el idioma vigente (ver [IDIOMAS_MOBILE.md](IDIOMAS_MOBILE.md)).
- Una función única, `mensajeDeError(Object error)` (`core/network/`),
  traduce cualquier error a un texto para el usuario. Las pantallas la usan; no
  hacen `error.toString()`, no leen `DioException.response` ni muestran el body.
- `features/health/` (pantalla de diagnóstico) es la única excepción a esto, y
  desaparece cuando haya un mejor lugar para ese chequeo.

## Cómo se muestra

| Situación | Dónde |
| --- | --- |
| Validación de un campo | Inline, vía `validator` del `TextFormField` |
| Falla al cargar datos (`AsyncValue.error`) | Widget de error compartido con "Reintentar" (ver [COMPONENTIZACION_MOBILE.md](COMPONENTIZACION_MOBILE.md)) |
| Falla de una mutación | `SnackBar` con el mensaje de `mensajeDeError`, desde un helper compartido |

- Sin diálogos con texto técnico. Los mensajes de login y reset se muestran tal
  cual los devuelve el backend (anti-enumeración, A07).
- `401` se resuelve solo en el interceptor (refresh) y en el `redirect` global de
  `go_router`; ninguna pantalla lo maneja por su cuenta.

## Errores no controlados

En `main.dart`, una sola vez:

- `FlutterError.onError` y `PlatformDispatcher.instance.onError` registran por el
  logger y no dejan caer la app en silencio.
- `ErrorWidget.builder` muestra un widget genérico en release (no el detalle de la
  excepción).
- No hay servicio de crash reporting definido. Si se agrega, es una decisión
  explícita y debe redactar datos personales y tokens.

## Logging

- Prohibido `print`. `avoid_print` ya viene activo con `flutter_lints`; no se
  desactiva. Se usa `debugPrint`/`dart:developer` (`log`) detrás de `kDebugMode`;
  en release no se emite nada.
- El logging de Dio (`LogInterceptor` o propio) existe **solo en debug** y nunca
  con cabeceras ni body de `/auth/**`.
- **Nunca se loguea:** access/refresh token, `Authorization`, passwords, datos
  personales. Ningún dato sensible va en una URL ni en `shared_preferences`
  (tokens solo en `flutter_secure_storage`).
- `.env` se empaqueta dentro de la app (asset), por lo que **nunca contiene
  secretos**: solo valores públicos como la URL base.

## Tests

`mensajeDeError` y la normalización del interceptor (sobre `failed`, sin
conexión, timeout) se prueban: es parsing de respuestas, caso que
[TESTING_MOBILE.md](TESTING_MOBILE.md) sí exige.
