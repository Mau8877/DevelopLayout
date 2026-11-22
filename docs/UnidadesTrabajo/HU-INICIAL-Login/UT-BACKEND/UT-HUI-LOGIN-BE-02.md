# UT-HUI-LOGIN-BE-02 — JwtService: emitir y validar access token

| Campo | Valor |
|-------|-------|
| **Código** | `UT-HUI-LOGIN-BE-02` |
| **Nombre** | JwtService: emitir y validar access token |
| **Historia** | HU-INICIAL-Login-BE |
| **Depende de** | — |

## Antes de implementar

Leer `backend/SEGURIDAD_AUTH_BACKEND.md`, `backend/CONVENCIONES_JAVA_BACKEND.md` y `.claude/rules/SEGURIDAD_OWASP.md`.

## Solicitud de negocio

El access token es lo que prueba quién es el usuario en cada request.

## Objetivo

Existe un servicio que emite y valida JWT firmados de 15 minutos.

## Alcance

- `security/JwtService.java` con `emitir(UUID codigoUsuario, List<String> roles, UUID sesionCodigo)` y `validar(String token)` (devuelve `codigo`, `sid` y roles o lanza `UnauthorizedException` `ERR_AUTH_07`).
- JJWT con HS256 y clave de al menos 256 bits; se rechaza `alg=none` y cualquier algoritmo distinto del configurado.
- Claims: `sub` (codigo), `sid` (codigo de la sesión), `roles`, `iat`, `exp` (15 min), `iss`; sin datos personales.
- `app.jwt.secret` (`JWT_SECRET`, sin default; la app falla al arrancar si falta o es corta) y `app.jwt.access-ttl` (15 min). Variable nueva en `.env.example` raíz/backend y en ambos compose.

**Archivos esperados (crear/tocar):**

- `projects/backend/src/main/java/com/example/backend/security/JwtService.java`
- `projects/backend/src/main/resources/application.properties`
- `projects/backend/pom.xml`
- `.env.example`
- `projects/backend/.env.example`
- `docker-compose.yml`
- `docker-compose.prod.yml`

## Fuera de alcance (no tocar)

- Cookies, filtros o controllers.
- Refresh tokens (son opacos y no pasan por este servicio).

## Resultado esperado

Un token emitido se valida de vuelta; uno vencido, alterado o firmado con otra clave se rechaza.

## Validación

1. Test unitario: emitir→validar devuelve el mismo `sub` y roles.
2. Test: token vencido, firma cambiada y `alg=none` lanzan `UnauthorizedException`.
3. Arrancar sin `JWT_SECRET`: falla con mensaje claro.

## Seguridad

A04/A07: clave fuera del repo, algoritmo fijo, TTL corto.
