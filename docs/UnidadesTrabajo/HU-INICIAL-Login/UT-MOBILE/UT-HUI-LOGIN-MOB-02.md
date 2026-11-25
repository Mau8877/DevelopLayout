# UT-HUI-LOGIN-MOB-02 — ApiClient: header mobile y Bearer

| Campo | Valor |
|-------|-------|
| **Código** | `UT-HUI-LOGIN-MOB-02` |
| **Nombre** | ApiClient: header mobile y Bearer |
| **Historia** | HU-INICIAL-Login-MOB |
| **Depende de** | UT-HUI-LOGIN-MOB-01 |

## Antes de implementar

Leer `mobile/ERRORES_LOGGING_MOBILE.md`, `mobile/CONSUMO_API_MOBILE.md` y `.claude/rules/SEGURIDAD_OWASP.md`.

## Solicitud de negocio

El backend reconoce al cliente mobile por header y lo autentica por `Authorization`.

## Objetivo

`ApiClient` envía `X-Client-Type: mobile` y el access token como Bearer.

## Alcance

- En `core/network/api_client.dart`: header fijo `X-Client-Type: mobile` e interceptor que agrega `Authorization: Bearer` leyendo `SecureTokenStorage`.
- Solo HTTPS en release (URL base desde `.env`); sin cookies.

- Los fallos sin sobre (sin conexión, timeout, respuesta no JSON, 502/504) llegan como `ApiException` con códigos `CLIENT_NETWORK`, `CLIENT_TIMEOUT`, `CLIENT_UNKNOWN`.
- `core/network/error_message.dart`: `mensajeDeError(Object error)` convierte cualquier error en un texto para el usuario (nunca `toString()` ni el body).
- Sin logging de cabeceras ni body de `/auth/**`; cualquier log de red existe solo en debug.

**Archivos esperados (crear/tocar):**

- `projects/mobile/lib/core/network/api_client.dart`
- `projects/mobile/lib/core/network/error_message.dart`

## Fuera de alcance (no tocar)

- Renovación automática en 401 (HU-INICIAL-Refresh).

## Resultado esperado

Las requests salen con el header y el Bearer cuando hay token.

## Validación

1. Test con adaptador falso de Dio: headers con y sin token, y errores sin sobre (`CLIENT_NETWORK`, `CLIENT_TIMEOUT`) y `mensajeDeError`.
2. `flutter analyze` limpio.

## Seguridad

MASVS-NETWORK: HTTPS en producción.
