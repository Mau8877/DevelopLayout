# UT-HUI-LOGIN-BE-05 — SesionService: crear la sesión y emitir el refresh token

| Campo | Valor |
|-------|-------|
| **Código** | `UT-HUI-LOGIN-BE-05` |
| **Nombre** | SesionService: crear la sesión y emitir el refresh token |
| **Historia** | HU-INICIAL-Login-BE |
| **Depende de** | Models y repositorios de sesiones (HU-INICIAL-Base) |

## Antes de implementar

Leer `backend/SEGURIDAD_AUTH_BACKEND.md`, `backend/CONVENCIONES_JAVA_BACKEND.md`, `backend/BASE_DE_DATOS_PRIVADA_RLS.md` y `.claude/rules/SEGURIDAD_OWASP.md`.

## Solicitud de negocio

Cada inicio de sesión necesita quedar registrado y con un credencial de larga vida que se pueda invalidar.

## Objetivo

Existe un servicio que crea la fila de `sesiones` y genera un refresh token opaco de 7 días.

## Alcance

- `features/auth/service/SesionService.java` con solo `crear(Usuario usuario, TipoCliente tipo, String ip, String userAgent)`: genera 32 bytes de `SecureRandom` (Base64 URL), guarda en `sesiones` solo el `sha256` del token, `estado = ACTIVA`, `expira_en = now + 7 días`, `ultimo_uso_en = now`, IP, user agent truncado y tipo de cliente. Devuelve la sesión y el token en claro solo al llamador.
- Parámetros `app.auth.refresh-ttl` (7 días) y `app.auth.session-max-lifetime` (30 días).
- Corre dentro de la transacción del login con el contexto RLS ya fijado (`app.current_user`).
- Nunca guarda ni loguea el token en claro.

**Archivos esperados (crear/tocar):**

- `projects/backend/src/main/java/com/example/backend/features/auth/service/SesionService.java`
- `projects/backend/src/main/resources/application.properties`

## Fuera de alcance (no tocar)

- Rotar o validar el token (UT de HU-INICIAL-Refresh).
- Revocar o cerrar sesiones (UT de Refresh, Logout y siguientes).
- Cookies o controllers.
- Registrar el evento en la bitácora (lo hace `AuthService`).

## Resultado esperado

`crear` deja una sesión `ACTIVA` en la base y devuelve un token cuyo hash coincide con la fila.

## Validación

1. Test de integración (Testcontainers, rol `app_runtime`): tras `crear`, existe la fila con `estado = ACTIVA` y `refresh_hash = sha256(token)`; el token en claro no está en la base.
2. Dos llamadas devuelven tokens distintos.
3. `SELECT refresh_hash FROM sesiones` no contiene el token en claro.

## Seguridad

A07/A04: 256 bits de entropía, solo el hash se persiste, RLS por usuario.
