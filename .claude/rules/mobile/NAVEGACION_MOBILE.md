---
globs: projects/mobile/**/*
---

# Navegación — Mobile

## `go_router` con redirect global

Un único `GoRouter`, en `lib/core/routing/app_router.dart` (`routerProvider`).
El chequeo de sesión **no se repite pantalla por pantalla**: se resuelve una
sola vez en el `redirect` raíz, que delega en una función pura y testeable:

```dart
String? redirectSegunSesion({
  required AsyncValue<UsuarioSesion?> auth,
  required String location,
});
```

Cargando la sesión → pantalla de carga; sin sesión → `/login`, salvo en las
rutas sin sesión (`/login`, `/forgot-password`; una nueva se agrega a ese
conjunto); con sesión en una de ellas o en carga → inicio. El router escucha `authProvider`
(`refreshListenable`), así que al cerrar sesión o al expirar redirige solo:
ninguna pantalla navega a `/login` a mano.

Mismo concepto que el guard de rutas del frontend web (ver
[RUTAS_NAVEGACION_FRONTEND.md](../frontend/RUTAS_NAVEGACION_FRONTEND.md)):
la lógica de "¿puede entrar acá?" vive en **un solo lugar**, no se
reimplementa un `if` de sesión al principio de cada pantalla.

## Fuente del estado de sesión

El `redirect` necesita leer si hay sesión válida — esto se resuelve contra
el provider de autenticación de `features/auth/providers/` (ver
[ESTADO_GLOBAL_MOBILE.md](ESTADO_GLOBAL_MOBILE.md)), no leyendo
`flutter_secure_storage` directo desde el router. El router depende del
provider de auth, no al revés.

## Estructura

- Definición de rutas y del `GoRouter` en `lib/core/routing/`.
- Cada feature expone sus rutas (o al menos las pantallas a las que el
  router apunta) desde `features/{feature}/screens/`, pero el árbol de
  rutas completo se ensambla en un solo lugar dentro de `core/routing/` —
  no queda esparcido un `GoRoute` por archivo suelto en cada feature sin un
  punto central que las junte.
