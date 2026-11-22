# UT-HUI-LOGIN-BE-06 — JwtAuthenticationFilter: autenticar por cookie o Bearer

| Campo | Valor |
|-------|-------|
| **Código** | `UT-HUI-LOGIN-BE-06` |
| **Nombre** | JwtAuthenticationFilter: autenticar por cookie o Bearer |
| **Historia** | HU-INICIAL-Login-BE |
| **Depende de** | UT-HUI-LOGIN-BE-01, UT-HUI-LOGIN-BE-02 |

## Antes de implementar

Leer `backend/SEGURIDAD_AUTH_BACKEND.md`, `SEGURIDAD_OWASP.md` y `.claude/rules/SEGURIDAD_OWASP.md`.

## Solicitud de negocio

Cada request privada debe identificar al usuario sin sesión de servidor.

## Objetivo

Un filtro lee el access token y puebla el `SecurityContext`.

## Alcance

- `security/JwtAuthenticationFilter.java` (`OncePerRequestFilter`): con `X-Client-Type: mobile` lee solo `Authorization: Bearer` (ignora cookies); sin ese header lee solo la cookie `accessToken`.
- Token válido → `Authentication` con `ROLE_*`; inválido o ausente → no autentica (el entry point responde 401). Nunca lanza excepciones sin controlar.
- Se registra en `SecurityConfig` después de `RateLimitFilter` y antes de `UsernamePasswordAuthenticationFilter`.
- `security/CurrentUserId.java` y su `HandlerMethodArgumentResolver` (devuelve el `UUID` del `sub`), registrado en `config/WebConfig.java`.

**Archivos esperados (crear/tocar):**

- `projects/backend/src/main/java/com/example/backend/security/JwtAuthenticationFilter.java`
- `projects/backend/src/main/java/com/example/backend/security/CurrentUserId.java`
- `projects/backend/src/main/java/com/example/backend/security/CurrentUserIdArgumentResolver.java`
- `projects/backend/src/main/java/com/example/backend/config/SecurityConfig.java`
- `projects/backend/src/main/java/com/example/backend/config/WebConfig.java`

## Fuera de alcance (no tocar)

- Emitir tokens (UT-HUI-LOGIN-BE-02/07).
- Endpoints de auth.
- Leer el refresh token.

## Resultado esperado

Con un access token válido la request llega autenticada; sin token recibe 401.

## Validación

1. `MockMvc`: cookie válida autentica; Bearer con header mobile autentica; cookie con header mobile NO autentica; token manipulado → 401.
2. `@CurrentUserId` inyecta el `UUID` correcto.

## Seguridad

A01/A07: sin mezclar canales de credencial, fallo cerrado ante token inválido.
