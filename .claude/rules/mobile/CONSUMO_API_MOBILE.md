---
globs: projects/mobile/**/*
---

# Consumo de API — Mobile

## Cliente `dio` centralizado (implementado)

- `lib/core/network/api_client.dart`: la única instancia de `Dio` de la app, con
  `baseUrl` leída de `.env` (`flutter_dotenv`). En release exige HTTPS.
- Su interceptor desempaqueta el sobre estándar del backend
  (`{status, data, message, error, timestamp, meta?}`, mismo contrato que
  [RESPONSES_BACKEND.md](../backend/RESPONSES_BACKEND.md)):
  - En éxito deja pasar `data` (y `meta` cuando exista); los `services` no
    desestructuran el sobre.
  - En error llega una `ApiException` (`code`, `message`) envuelta en
    `DioException`; `core/network/error_message.dart` la extrae.
- `lib/core/network/refresh_interceptor.dart` (`QueuedInterceptor`): ante un
  401 renueva una sola vez y reintenta con un `Dio` auxiliar (reintentar por el
  principal bloquearía la cola). Solo un 401 o 403 del refresh borra los tokens
  y avisa por `ApiClient.onSesionExpirada`, que registra el provider de sesión
  (`core` no importa `features`). Sin red, 429 o 500 los conserva. Si la sesión
  se cierra durante un refresh, no se la revive.

## Servicios por feature

Los servicios que llaman a la API viven en `services/` dentro de cada
feature, siguiendo el scaffold ya creado (`lib/features/auth/services/`):

```
lib/features/usuarios/
├── services/
│   └── usuarios_service.dart   # usa el ApiClient de core/network/
├── providers/
└── screens/
```

Un `service` recibe el `ApiClient` compartido (no crea su propia instancia
de `Dio`), y solo conoce endpoints de **su** feature.

### Endpoints compartidos entre features

Mismo criterio que en el frontend web (ver
[TANSTACK_QUERY_FRONTEND.md](../frontend/TANSTACK_QUERY_FRONTEND.md)): si un
endpoint se consume desde más de una feature, su lógica va en
`lib/core/network/` (no en `features/{feature}/services/` de ninguna de las
dos, y no duplicado en ambas). Se mueve ahí recién cuando aparece el segundo
consumidor real, no de forma anticipada.

## Modelos (DTOs) y tokens

- **DTOs:** `lib/features/{feature}/models/{nombre}.dart`, una clase por archivo,
  inmutable (`final`), con `factory X.fromJson(Map<String, dynamic>)`. Los
  `services` los devuelven; la UI nunca recibe un `Map` crudo.
- **Tokens:** `lib/core/storage/secure_token_storage.dart` (`SecureTokenStorage`)
  guarda access y refresh en el almacenamiento seguro del sistema
  (`flutter_secure_storage`). Nunca `shared_preferences`, nunca en logs.
- **Cabeceras:** `ApiClient` manda siempre `X-Client-Type: mobile` y, si hay
  token, `Authorization: Bearer`. Sin cookies.
