# UT-HUI-REFRESH-MOB-01 — Interceptor Dio de refresh (QueuedInterceptor)

| Campo | Valor |
|-------|-------|
| **Código** | `UT-HUI-REFRESH-MOB-01` |
| **Nombre** | Interceptor Dio de refresh (QueuedInterceptor) |
| **Historia** | HU-INICIAL-Refresh-MOB |
| **Depende de** | — |

## Antes de implementar

Leer `mobile/CONSUMO_API_MOBILE.md` y `.claude/rules/SEGURIDAD_OWASP.md`.

## Solicitud de negocio

La app debe renovar sin pedir contraseña y sin disparar refresh duplicados.

## Objetivo

Ante 401 el cliente renueva con el refresh guardado y reintenta una vez.

## Alcance

- `core/network/refresh_interceptor.dart` (`QueuedInterceptor`) que llama a `POST /auth/refresh` con el refresh de `SecureTokenStorage`, guarda los tokens nuevos y reintenta una vez, usando un `Dio` auxiliar (reintentar por el mismo bloquearía la cola). Solo si el refresh responde **401 o 403** borra el almacenamiento y avisa a `authProvider` para ir a login (vía callback inyectado, `core` no importa `features`); sin conexión, 429 o 500 conserva los tokens.
- Si la sesión se cerró mientras el refresh estaba en curso, los tokens nuevos no se guardan y se cierran en el servidor.
- Registrarlo en `ApiClient`.

**Archivos esperados (crear/tocar):**

- `projects/mobile/lib/core/network/refresh_interceptor.dart`
- `projects/mobile/lib/core/network/api_client.dart`
- `projects/mobile/lib/features/auth/providers/auth_provider.dart`

## Fuera de alcance (no tocar)

- Pantallas.
- Logout.

## Resultado esperado

Varios 401 seguidos producen un solo refresh y la sesión se renueva sin intervención.

## Validación

1. Test con Dio falso: concurrencia, reintento, fallo que limpia el storage.

## Seguridad

MASVS-STORAGE/AUTH: tokens rotados en almacenamiento seguro.
