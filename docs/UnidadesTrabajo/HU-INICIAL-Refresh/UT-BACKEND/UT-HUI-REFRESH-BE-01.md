# UT-HUI-REFRESH-BE-01 — SesionService.rotar, revocación y detección de reutilización

| Campo | Valor |
|-------|-------|
| **Código** | `UT-HUI-REFRESH-BE-01` |
| **Nombre** | SesionService.rotar, revocación y detección de reutilización |
| **Historia** | HU-INICIAL-Refresh-BE |
| **Depende de** | Login: SesionService.crear (HU-INICIAL-Login) |

## Antes de implementar

Leer `backend/SEGURIDAD_AUTH_BACKEND.md`, `backend/BASE_DE_DATOS_PRIVADA_RLS.md`, `backend/BITACORA_BACKEND.md` y `.claude/rules/SEGURIDAD_OWASP.md`.

## Solicitud de negocio

Un refresh token robado no debe servir indefinidamente y una sesión revocada debe dejar de valer de inmediato.

## Objetivo

`SesionService` rota el refresh token, expira sesiones y revoca ante reutilización; `SesionRevocadaRegistry` puede revocar.

## Alcance

- Editar `SesionService` agregando `rotar(String tokenEnClaro, String ip, String userAgent)` y `revocar(Sesion sesion, MotivoRevocacion motivo)`.
- `rotar`: fija `RlsContext.usarRefreshHash(sha256)`, busca la sesión por `refresh_hash`; si está `ACTIVA` y no vencida, guarda el hash actual en `refresh_hash_anterior`, pone el nuevo hash, actualiza `ultimo_uso_en`, `ip` y `expira_en = min(now + 7 días, creada + 30 días)` y devuelve sesión y token nuevo.
- Si el hash coincide con `refresh_hash_anterior` (token ya rotado) es reutilización: `revocar(sesion, REUSO)`, evento `AUTH_REFRESH_REUSO` con `registrarEnTransaccionPropia` y `UnauthorizedException` `ERR_AUTH_04`.
- Si la sesión superó su vida máxima o `expira_en`, se marca `EXPIRADA` y responde `ERR_AUTH_04`. Hash desconocido o sesión no `ACTIVA` también `ERR_AUTH_04`.
- `revocar` marca `REVOCADA` con `revocada_en` y motivo, y llama a `SesionRevocadaRegistry.revocar(sid, ttl = vida del access token)`. Idempotente.
- La revocación y la expiración **se confirman aunque se responda 401**: las transacciones usan `noRollbackFor = UnauthorizedException`.
- La fila se lee **con bloqueo** (`PESSIMISTIC_WRITE`): dos solicitudes no pueden canjear el mismo token.
- Tras encontrar la sesión por su hash se fija además `RlsContext.usarUsuario(sesion.usuarioCodigo)`: la política RLS no permite actualizarla solo con el hash.
- La reutilización de una sesión que ya no está `ACTIVA` solo responde `ERR_AUTH_04`, sin repetir el evento.
- `Sesion.ip` pasa a ser actualizable.
- Editar `SesionRevocadaRegistry` agregando solo `revocar(UUID sid, Duration ttl)`; si Redis falla se propaga el error.
- Agregar `RlsContext.usarRefreshHash(String hash)` (`app.refresh_hash`).
- `ERR_AUTH_04` en `AuthErrorCodes` y su sección en `docs/errors/auth/ERRORES_AUTH.md` + índice.

**Archivos esperados (crear/tocar):**

- `projects/backend/src/main/java/com/example/backend/features/auth/service/SesionService.java`
- `projects/backend/src/main/java/com/example/backend/security/SesionRevocadaRegistry.java`
- `projects/backend/src/main/java/com/example/backend/security/RlsContext.java`
- `projects/backend/src/main/java/com/example/backend/features/auth/model/Sesion.java`
- `projects/backend/src/main/java/com/example/backend/features/auth/repository/SesionRepository.java`
- `projects/backend/src/test/java/com/example/backend/features/auth/service/SesionServiceTest.java`
- `projects/backend/src/main/java/com/example/backend/features/auth/AuthAcciones.java`
- `projects/backend/src/main/java/com/example/backend/features/auth/AuthErrorCodes.java`
- `docs/errors/auth/ERRORES_AUTH.md`
- `docs/errors/README.md`

## Fuera de alcance (no tocar)

- Cerrar sesión por el usuario (UT de HU-INICIAL-Logout).
- Revocar todas las sesiones de un usuario (UT de HU-INICIAL-OlvidePassword).
- Controller y cookies.
- Leer roles del usuario (lo hace `AuthService.refresh`).

## Resultado esperado

Un token válido se canjea una vez; reusarlo revoca la sesión; una sesión revocada o expirada no vuelve a emitir tokens.

## Validación

1. Test de integración (Testcontainers, `app_runtime`, Redis): rotar cambia `refresh_hash` y guarda el anterior; reusar el anterior → `ERR_AUTH_04`, sesión `REVOCADA` con motivo `REUSO`, clave `auth:revoked:{sid}` presente y evento `AUTH_REFRESH_REUSO`.
2. Sesión con más de 30 días → `EXPIRADA` y `ERR_AUTH_04`.
3. Hash desconocido → `ERR_AUTH_04`.

## Seguridad

A07: rotación obligatoria, respuesta ante robo, revocación inmediata y falla cerrada.
