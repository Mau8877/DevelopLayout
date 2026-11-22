# UT-HUI-REFRESH-BE-01 — RefreshTokenService: rotar y detectar reutilización

| Campo | Valor |
|-------|-------|
| **Código** | `UT-HUI-REFRESH-BE-01` |
| **Nombre** | RefreshTokenService: rotar y detectar reutilización |
| **Historia** | HU-INICIAL-Refresh-BE |
| **Depende de** | Login: RefreshTokenService.emitir (HU-INICIAL-Login) |

## Antes de implementar

Leer `backend/SEGURIDAD_AUTH_BACKEND.md`, `backend/CONVENCIONES_JAVA_BACKEND.md` y `.claude/rules/SEGURIDAD_OWASP.md`.

## Solicitud de negocio

Un refresh token robado no debe servir indefinidamente.

## Objetivo

`RefreshTokenService` valida, rota y revoca la familia ante reutilización.

## Alcance

- Editar `features/auth/service/RefreshTokenService.java` agregando `rotar(String tokenEnClaro)` y `revocarTodas(UUID codigoUsuario)`.
- `rotar`: busca `auth:refresh:{hash}`; si existe lo borra (uso único), emite uno nuevo con `emitir` y devuelve `codigoUsuario` + token nuevo.
- Si el hash no existe pero hay un registro marcador de "usado" (`auth:refresh:used:{hash}`, TTL 7 días) → reutilización: `revocarTodas(codigo)` y `UnauthorizedException` `ERR_AUTH_04`. Si no existe ni marcador, `ERR_AUTH_04`.
- Si Redis falla la excepción se propaga (falla cerrado).
- `ERR_AUTH_04` en `AuthErrorCodes` y su sección en `docs/errors/auth/ERRORES_AUTH.md` + índice.

**Archivos esperados (crear/tocar):**

- `projects/backend/src/main/java/com/example/backend/features/auth/service/RefreshTokenService.java`
- `projects/backend/src/main/java/com/example/backend/features/auth/AuthErrorCodes.java`
- `docs/errors/auth/ERRORES_AUTH.md`
- `docs/errors/README.md`

## Fuera de alcance (no tocar)

- Revocar un solo token por logout (UT de HU-INICIAL-Logout).
- Controller y cookies.
- Leer roles del usuario (lo hace `AuthService.refresh`).

## Resultado esperado

Un token válido se canjea una vez; reusarlo revoca todas las sesiones del usuario.

## Validación

1. Test con Redis: rotar invalida el anterior; reusar el anterior → `ERR_AUTH_04` y el set del usuario queda vacío.
2. Token inexistente → `ERR_AUTH_04`.

## Seguridad

A07: rotación obligatoria y respuesta ante robo.
