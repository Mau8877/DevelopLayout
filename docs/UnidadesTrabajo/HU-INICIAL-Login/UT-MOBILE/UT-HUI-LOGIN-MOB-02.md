# UT-HUI-LOGIN-MOB-02 — ApiClient: header mobile y Bearer

| Campo | Valor |
|-------|-------|
| **Código** | `UT-HUI-LOGIN-MOB-02` |
| **Nombre** | ApiClient: header mobile y Bearer |
| **Historia** | HU-INICIAL-Login-MOB |
| **Depende de** | UT-HUI-LOGIN-MOB-01 |

## Antes de implementar

Leer `mobile/CONSUMO_API_MOBILE.md` y `.claude/rules/SEGURIDAD_OWASP.md`.

## Solicitud de negocio

El backend reconoce al cliente mobile por header y lo autentica por `Authorization`.

## Objetivo

`ApiClient` envía `X-Client-Type: mobile` y el access token como Bearer.

## Alcance

- En `core/network/api_client.dart`: header fijo `X-Client-Type: mobile` e interceptor que agrega `Authorization: Bearer` leyendo `SecureTokenStorage`.
- Solo HTTPS en release (URL base desde `.env`); sin cookies.

**Archivos esperados (crear/tocar):**

- `projects/mobile/lib/core/network/api_client.dart`

## Fuera de alcance (no tocar)

- Renovación automática en 401 (HU-INICIAL-Refresh).

## Resultado esperado

Las requests salen con el header y el Bearer cuando hay token.

## Validación

1. Test con adaptador falso de Dio: headers con y sin token.
2. `flutter analyze` limpio.

## Seguridad

MASVS-NETWORK: HTTPS en producción.
