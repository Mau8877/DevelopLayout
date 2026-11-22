# UT-HUI-LOGIN-BE-05 — RefreshTokenService: emitir refresh token

| Campo | Valor |
|-------|-------|
| **Código** | `UT-HUI-LOGIN-BE-05` |
| **Nombre** | RefreshTokenService: emitir refresh token |
| **Historia** | HU-INICIAL-Login-BE |
| **Depende de** | — |

## Antes de implementar

Leer `backend/SEGURIDAD_AUTH_BACKEND.md`, `backend/CONVENCIONES_JAVA_BACKEND.md` y `.claude/rules/SEGURIDAD_OWASP.md`.

## Solicitud de negocio

La sesión necesita un credencial de larga vida que se pueda invalidar.

## Objetivo

Existe un servicio que genera y guarda en Redis un refresh token opaco de 7 días, indexado por usuario.

## Alcance

- `features/auth/service/RefreshTokenService.java` con solo `emitir(UUID codigoUsuario)`: 32 bytes de `SecureRandom` en Base64 URL, guardados como `auth:refresh:{sha256(token)}` → `codigoUsuario` con TTL 7 días, y el hash agregado al set `auth:refresh:user:{codigo}`. Devuelve el token en claro solo al llamador.
- `app.auth.refresh-ttl` (7 días).
- Nunca guarda ni loguea el token en claro.

**Archivos esperados (crear/tocar):**

- `projects/backend/src/main/java/com/example/backend/features/auth/service/RefreshTokenService.java`
- `projects/backend/src/main/resources/application.properties`

## Fuera de alcance (no tocar)

- Rotar o validar el token (UT de HU-INICIAL-Refresh).
- Revocarlo (UT de HU-INICIAL-Logout).
- Cookies o controllers.

## Resultado esperado

`emitir` devuelve un token y deja su hash en Redis con TTL de 7 días.

## Validación

1. Test con Redis de Testcontainers: tras `emitir`, `auth:refresh:{hash}` existe con TTL ≈ 7 días y el set del usuario lo contiene.
2. Dos llamadas devuelven tokens distintos.
3. `redis-cli KEYS "auth:refresh:*"` no muestra el token en claro.

## Seguridad

A07/A04: 256 bits de entropía, solo el hash se persiste.
