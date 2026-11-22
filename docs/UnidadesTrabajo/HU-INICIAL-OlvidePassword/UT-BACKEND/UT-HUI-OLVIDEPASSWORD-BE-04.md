# UT-HUI-OLVIDEPASSWORD-BE-04 — Solicitar recuperación: POST /auth/password/forgot

| Campo | Valor |
|-------|-------|
| **Código** | `UT-HUI-OLVIDEPASSWORD-BE-04` |
| **Nombre** | Solicitar recuperación: POST /auth/password/forgot |
| **Historia** | HU-INICIAL-OlvidePassword-BE |
| **Depende de** | UT-HUI-OLVIDEPASSWORD-BE-02, UT-HUI-OLVIDEPASSWORD-BE-03 |

## Antes de implementar

Leer `backend/SEGURIDAD_AUTH_BACKEND.md`, `backend/ENDPOINTS_BACKEND.md`, `SEGURIDAD_OWASP.md` (API6) y `.claude/rules/SEGURIDAD_OWASP.md`.

## Solicitud de negocio

Permite recuperar el acceso sin revelar qué correos existen.

## Objetivo

Existe `POST /api/v1/auth/password/forgot` con respuesta uniforme.

## Alcance

- Editar `AuthService` agregando solo `solicitarRecuperacion(String email)`: si el usuario existe y está activo, emite el token y envía el correo; si no, hace trabajo equivalente (misma duración aproximada, p. ej. ejecuta un hash ficticio) y no envía nada. Siempre retorna lo mismo.
- Editar `AuthController`: `POST /password/forgot` (`ForgotPasswordRequest` con `@Email`) → 200 con mensaje genérico en `message` y `data: null`.
- Límite propio: por IP y por correo (clave `auth:forgot:{hash}`), p. ej. 3 por hora por correo; excedido → 429 con el sobre estándar y `ERR_SYS_02`.
- `SecurityConfig`: `permitAll` SOLO para esta ruta, con comentario (el usuario no tiene sesión); CSRF activo en web.
- Logs: `INFO` sin token; `WARN` al exceder límite.
- Registra `AUTH_RECUPERACION_SOLICITADA` en la bitácora solo cuando el usuario existe, con el correo como hash corto.

**Archivos esperados (crear/tocar):**

- `projects/backend/src/main/java/com/example/backend/features/auth/service/AuthService.java`
- `projects/backend/src/main/java/com/example/backend/features/auth/controller/AuthController.java`
- `projects/backend/src/main/java/com/example/backend/features/auth/dto/ForgotPasswordRequest.java`
- `projects/backend/src/main/java/com/example/backend/config/SecurityConfig.java`
- `projects/backend/src/main/java/com/example/backend/security/RateLimitFilter.java`
- `projects/backend/src/main/java/com/example/backend/features/auth/AuthAcciones.java`

## Fuera de alcance (no tocar)

- Restablecer la contraseña (UT 05).
- Cambios en el frontend o mobile.

## Resultado esperado

Cualquier correo recibe la misma respuesta; solo los activos reciben el correo.

## Validación

1. Con Mailpit: correo existente recibe mensaje; inexistente no, y ambas respuestas son idénticas.
2. Medir tiempos: equivalentes.
3. Cuarta solicitud en una hora para el mismo correo → 429.

## Seguridad

A07/API6: anti-enumeración por contenido y tiempo, límite específico del flujo.
