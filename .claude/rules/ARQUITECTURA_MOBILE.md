---
globs: projects/mobile/**/*
---

# Arquitectura Mobile

**Feature-based**, mismo criterio conceptual que el frontend web (ver
[ARQUITECTURA_FRONTEND.md](ARQUITECTURA_FRONTEND.md)): el código se
organiza por funcionalidad de negocio (`features/{feature}/`), y lo
transversal a toda la app vive en `core/`.

## Estructura de `lib/`

La estructura real que existe hoy en el repo (`core/`, `layouts/`,
`shared/widgets/` y `features/auth/` implementados; `features/health/` es de
diagnóstico):

```
lib/
├── core/
│   ├── network/     # ApiClient (Dio + sobre), refresh_interceptor y error_message (ver CONSUMO_API_MOBILE.md)
│   ├── routing/     # GoRouter único con el redirect global según la sesión (ver NAVEGACION_MOBILE.md)
│   ├── storage/     # SecureTokenStorage: tokens en el almacenamiento seguro del sistema
│   ├── logging/     # AppLogger: único punto de log, solo en debug (ver ERRORES_LOGGING_MOBILE.md)
│   ├── theme/       # AppColors/AppTheme, con paleta neutra de placeholder
│   └── marca.dart   # El nombre del producto (lo cambia project:init)
├── layouts/
│   └── {rol}/       # Shell por rol (AdminShell, UsuarioShell), con "Cerrar sesión"
├── shared/
│   ├── language/    # Carga de textos y textos comunes (ver IDIOMAS_MOBILE.md)
│   └── widgets/     # Widgets genéricos, sin dominio (AppButton, EstadoError, pedirConfirmacion...)
├── features/
│   ├── auth/        # Referencia real: models, providers, screens, services
│   ├── health/      # Feature de diagnóstico contra el backend real
│   └── {feature}/   # Un feature de negocio (ver estructura abajo)
└── main.dart         # Entry point: ProviderScope + MaterialApp.router
```

`core/` es la única carpeta de nivel superior con código transversal real
además de `features/` — no hay una carpeta `common/`/`utils/` genérica
todavía; si aparece una necesidad concreta de utilidades puramente
transversales (no ligadas a red, routing ni tema), se define esa carpeta
en su momento, no se anticipa vacía.

`features/health/` no es una feature de negocio real — existe para probar
de punta a punta que `ApiClient` (Dio + interceptor) y Riverpod
(provider + `AsyncValue`) funcionan contra el backend real antes de que
exista la primera feature real. Se borra o se reemplaza el día que haya un
mejor lugar para un chequeo de conectividad, mismo criterio que
`features/home/` en el frontend web.

`layouts/{rol}/` sigue el mismo patrón que
[ARQUITECTURA_FRONTEND.md](ARQUITECTURA_FRONTEND.md#estructura-de-src) del
lado web: un shell delgado por rol, que `core/routing/app_router.dart` monta
según el rol de la sesión. Su única acción sobre una feature es "Cerrar sesión"
(`authProvider`).

## Estructura de un feature (`lib/features/{feature}/`)

`lib/features/auth/` es la referencia real — cualquier feature nueva sigue
esta misma forma:

```
lib/features/usuarios/
├── language/     # Textos de ESTA feature: es.json y en.json (ver IDIOMAS_MOBILE.md)
├── models/       # DTOs Dart (fromJson) del JSON de request/response de la API
├── providers/    # Riverpod: AsyncNotifier/FutureProvider (datos) + *UiProvider (estado de UI)
├── screens/      # Pantallas completas que apunta el GoRouter de core/routing/
├── services/     # Llamadas a la API de ESTE feature, vía el ApiClient de core/network/
└── widgets/      # Solo widgets que conocen el dominio de esta feature -- se agrega cuando hace falta uno
```

Referencia de cada subcarpeta, en el resto de las rules:
[ESTADO_GLOBAL_MOBILE.md](mobile/ESTADO_GLOBAL_MOBILE.md) (`providers/`,
convención de nombres), [CONSUMO_API_MOBILE.md](mobile/CONSUMO_API_MOBILE.md)
(`services/`), [NAVEGACION_MOBILE.md](mobile/NAVEGACION_MOBILE.md) (cómo
`screens/` se conecta con el router central), [WIDGETS_UI_MOBILE.md](mobile/WIDGETS_UI_MOBILE.md)
(`widgets/`; el criterio de qué va en `shared/widgets/` está en COMPONENTIZACION_MOBILE.md).

> **Modelos de datos (DTOs Dart):** viven en `lib/features/{feature}/models/`, uno por
> archivo, con un `fromJson` (ver [CONSUMO_API_MOBILE.md](mobile/CONSUMO_API_MOBILE.md)).

## Regla de dependencia

`features/{feature}/` puede depender de `core/` y `shared/` — nunca al
revés (`core/`/`shared/` no importan nada de `features/`). **Única excepción:**
`core/routing/`, que ensambla el árbol de rutas y por eso importa las pantallas,
el provider de sesión y los shells (ver
[NAVEGACION_MOBILE.md](mobile/NAVEGACION_MOBILE.md)). Una feature no
depende de otra feature directo salvo que la relación de negocio lo
justifique explícitamente, mismo criterio que
[ARQUITECTURA_BACKEND.md](ARQUITECTURA_BACKEND.md#regla-de-dependencia).
