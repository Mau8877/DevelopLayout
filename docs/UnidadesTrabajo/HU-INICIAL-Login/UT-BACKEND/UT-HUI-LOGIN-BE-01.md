# UT-HUI-LOGIN-BE-01 — Configuración de seguridad stateless, CSRF y manejo de 401/403

| Campo | Valor |
|-------|-------|
| **Código** | `UT-HUI-LOGIN-BE-01` |
| **Nombre** | Configuración de seguridad stateless, CSRF y manejo de 401/403 |
| **Historia** | HU-INICIAL-Login-BE |
| **Depende de** | — |

## Antes de implementar

Leer `backend/SEGURIDAD_AUTH_BACKEND.md`, `SEGURIDAD_OWASP.md`, `backend/RESPONSES_BACKEND.md` y `.claude/rules/SEGURIDAD_OWASP.md`.

## Solicitud de negocio

Hoy Spring crea sesiones y responde 403 vacío; la API necesita responder siempre con el sobre estándar y sin estado de servidor.

## Objetivo

`SecurityConfig` queda stateless, con CSRF double-submit y con 401/403 en formato `ApiResponse`.

## Alcance

- `SessionCreationPolicy.STATELESS` (sin `JSESSIONID`).
- CSRF con `CookieCsrfTokenRepository.withHttpOnlyFalse()` y `CsrfTokenRequestAttributeHandler`; se ignora CSRF solo para requests con `Authorization: Bearer` o con el header `X-Client-Type: mobile` (un header propio obliga a preflight CORS, que falla para orígenes no permitidos).
- `security/RestAuthenticationEntryPoint` (401, `ERR_AUTH_07`) y `security/RestAccessDeniedHandler` (403, `ERR_AUTH_08`) escribiendo el sobre de `RESPONSES_BACKEND.md`, registrados en `SecurityConfig`.
- `AuthErrorCodes` solo con `ERR_AUTH_07` y `ERR_AUTH_08`, y sus secciones en `docs/errors/auth/ERRORES_AUTH.md` + índice en `docs/errors/README.md`.
- CORS: se mantiene la lista explícita de orígenes y se agregan `X-XSRF-TOKEN` y `X-Client-Type` a las cabeceras permitidas.
- `server.error.include-*=never` en `application.properties`.

**Archivos esperados (crear/tocar):**

- `projects/backend/src/main/java/com/example/backend/config/SecurityConfig.java`
- `projects/backend/src/main/java/com/example/backend/security/RestAuthenticationEntryPoint.java`
- `projects/backend/src/main/java/com/example/backend/security/RestAccessDeniedHandler.java`
- `projects/backend/src/main/java/com/example/backend/features/auth/AuthErrorCodes.java`
- `docs/errors/auth/ERRORES_AUTH.md`
- `docs/errors/README.md`

## Fuera de alcance (no tocar)

- Filtro JWT (UT-HUI-LOGIN-BE-06).
- Reglas `permitAll` de los endpoints de auth (UT-HUI-LOGIN-BE-08, con su justificación).
- Endpoints `/csrf`, `/login`, `/me`.

## Resultado esperado

Una request sin credenciales a un endpoint privado responde 401 con sobre JSON, sin cookie de sesión.

## Validación

1. `curl -i http://localhost:8080/api/v1/x` devuelve 401 con `status:"failed"` y `error:"ERR_AUTH_07"`, sin `Set-Cookie: JSESSIONID`.
2. Un `POST` web sin `X-XSRF-TOKEN` a un endpoint privado devuelve 403 con `ERR_AUTH_08`.
3. `./mvnw test` pasa.

## Seguridad

A01/A02/A10: falla cerrada, sin sesiones de servidor, sin detalles internos en el error.
