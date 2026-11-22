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

- `core/network/refresh_interceptor.dart` (`QueuedInterceptor`) que llama a `POST /auth/refresh` con el refresh de `SecureTokenStorage`, guarda los tokens nuevos y reintenta; si falla, borra el almacenamiento y avisa a `authProvider` para ir a login (vía callback inyectado, `core` no importa `features`).
- Registrarlo en `ApiClient`.

**Archivos esperados (crear/tocar):**

- `projects/mobile/lib/core/network/refresh_interceptor.dart`
- `projects/mobile/lib/core/network/api_client.dart`

## Fuera de alcance (no tocar)

- Pantallas.
- Logout.

## Resultado esperado

Varios 401 seguidos producen un solo refresh y la sesión se renueva sin intervención.

## Validación

1. Test con Dio falso: concurrencia, reintento, fallo que limpia el storage.

## Seguridad

MASVS-STORAGE/AUTH: tokens rotados en almacenamiento seguro.
