# UT-HUI-LOGOUT-BE-01 — SesionService.cerrar, AuthService.logout y endpoint POST /logout

| Campo | Valor |
|-------|-------|
| **Código** | `UT-HUI-LOGOUT-BE-01` |
| **Nombre** | SesionService.cerrar, AuthService.logout y endpoint POST /logout |
| **Historia** | HU-INICIAL-Logout-BE |
| **Depende de** | Refresh: SesionService.revocar (HU-INICIAL-Refresh) |

## Antes de implementar

Leer `backend/SEGURIDAD_AUTH_BACKEND.md`, `backend/ENDPOINTS_BACKEND.md`, `backend/BITACORA_BACKEND.md` y `.claude/rules/SEGURIDAD_OWASP.md`.

## Solicitud de negocio

El usuario debe poder invalidar su sesión y que deje de funcionar al instante.

## Objetivo

Existe `POST /api/v1/auth/logout` que revoca la sesión y borra las cookies.

## Alcance

- Editar `SesionService` agregando solo `cerrar(String tokenEnClaro)`: fija `RlsContext.usarRefreshHash`, busca la sesión por hash y, si está `ACTIVA`, llama a `revocar(sesion, LOGOUT)`; si no existe o ya está cerrada, no hace nada (idempotente).
- Editar `AuthService` agregando solo `logout(String refreshToken)` con el evento `AUTH_LOGOUT` en la bitácora (solo si había sesión activa).
- Editar `AuthController` agregando `POST /logout`: web lee la cookie y responde borrando ambas (`Max-Age=0`); mobile lee el body. Siempre 200.
- `SecurityConfig`: `permitAll` SOLO para `POST /api/v1/auth/logout`, para que funcione con el access token vencido (comentario de justificación); el CSRF sigue activo en web.

**Archivos esperados (crear/tocar):**

- `projects/backend/src/main/java/com/example/backend/features/auth/service/SesionService.java`
- `projects/backend/src/main/java/com/example/backend/features/auth/service/AuthService.java`
- `projects/backend/src/main/java/com/example/backend/features/auth/controller/AuthController.java`
- `projects/backend/src/main/java/com/example/backend/features/auth/AuthAcciones.java`
- `projects/backend/src/main/java/com/example/backend/config/SecurityConfig.java`

## Fuera de alcance (no tocar)

- Cerrar otras sesiones del mismo usuario (HU futura, pospuesta).
- Cambios en el cliente.

## Resultado esperado

Tras logout la sesión queda `REVOCADA`, su access token se rechaza al instante y las cookies se borran.

## Validación

1. `curl` logout → 200 con `Set-Cookie` de `Max-Age=0`; luego `/me` con el access token anterior → 401 inmediato y `/refresh` con el token anterior → 401; logout repetido → 200.
2. Sin `X-XSRF-TOKEN` → 403.
3. Fila de `sesion` en `REVOCADA` con motivo `LOGOUT` y evento `AUTH_LOGOUT`.

## Seguridad

A07: invalidación del lado servidor e inmediata; CSRF activo.
