# UT-HUI-REFRESH-BE-04 — Refresh token firmado y detección de reutilización sin límite

| Campo | Valor |
|-------|-------|
| **Código** | `UT-HUI-REFRESH-BE-04` |
| **Nombre** | Refresh token firmado y detección de reutilización sin límite |
| **Historia** | HU-INICIAL-Refresh-BE |
| **Depende de** | UT-HUI-REFRESH-BE-03 |

## Antes de implementar

`backend/SEGURIDAD_AUTH_BACKEND.md`, `backend/BASE_DE_DATOS_PRIVADA_RLS.md`, `backend/SCHEMAS_BD_BACKEND.md`.

## Solicitud de negocio

Un token robado no debe poder seguir vivo porque el ladrón lo rotó varias veces.

## Objetivo

Cualquier token anterior de una sesión activa revoca la sesión.

## Alcance

- `RefreshTokens`: `{sesion}.{aleatorio}.{HMAC-SHA256}`; `app.auth.refresh-token-secret` (`REFRESH_TOKEN_SECRET`, mínimo 32 caracteres, distinta de `JWT_SECRET`).
- La sesión se busca por su `codigo` solo después de validar la firma (`app.refresh_sid`).
- Migración: expira las sesiones activas, cambia la política RLS de `sesion` y elimina `refresh_hash_anterior`. Debe funcionar con un dueño sin superusuario.
- Variable en `.env.example`, compose y `setup-env`.

**Archivos esperados (crear/tocar):**

- `projects/backend/src/main/java/com/example/backend/features/auth/service/RefreshTokens.java`, `SesionService.java`
- `projects/backend/src/main/java/com/example/backend/features/auth/model/Sesion.java`, `repository/SesionRepository.java`, `security/RlsContext.java`, `common/model/BaseModel.java`
- `projects/backend/src/main/resources/db/migration/V20261126120000__refresh_token_firmado.sql`
- `docs/BASE_DE_DATOS.mermaid`, `.env.example`, `docker-compose.yml`, `docker-compose.prod.yml`, `scripts/setup-env.mjs`

## Seguridad

A07/A04: la firma se compara en tiempo constante; un token alterado no toca la base.

## Fuera de alcance (no tocar)

Cambios en web o mobile: el token sigue siendo un texto que no interpretan.

## Resultado esperado

Reutilizar un token de cualquier antigüedad revoca la sesión.

## Validación

1. `AuthRefreshTest`, `RefreshTokensTest` y `MigracionRefreshFirmadoTest` en verde.
