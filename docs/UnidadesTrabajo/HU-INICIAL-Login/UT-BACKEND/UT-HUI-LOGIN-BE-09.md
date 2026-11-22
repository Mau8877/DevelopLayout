# UT-HUI-LOGIN-BE-09 — Tests de autenticación (login, lockout, autorización)

| Campo | Valor |
|-------|-------|
| **Código** | `UT-HUI-LOGIN-BE-09` |
| **Nombre** | Tests de autenticación (login, lockout, autorización) |
| **Historia** | HU-INICIAL-Login-BE |
| **Depende de** | UT-HUI-LOGIN-BE-08 |

## Antes de implementar

Leer `backend/TESTING_BACKEND.md`, `SEGURIDAD_OWASP.md` ("Antes de dar algo por terminado") y `.claude/rules/SEGURIDAD_OWASP.md`.

## Solicitud de negocio

La seguridad de login solo está garantizada si hay pruebas que la rompan.

## Objetivo

Quedan tests de controller que cubren los CA de la HU.

## Alcance

- `features/auth/AuthControllerTest` (`MockMvc`) verificando el sobre `ApiResponse` completo, no solo el status.
- Casos: éxito web (cookies y atributos), éxito mobile (body, sin cookies), 401 genérico igual para ambos fallos, 423, 403 inactivo, 400 con campos inválidos o extra, CSRF ausente → 403, `/me` con y sin sesión, header mobile con cookie → no autenticado.
- Endpoint privado con y sin token (permitido y denegado).
- Verifica la fila creada en `sesiones` y los eventos de bitácora (`AUTH_LOGIN_OK`, `AUTH_LOGIN_FALLO`, `AUTH_CUENTA_BLOQUEADA`) sin datos sensibles.

**Archivos esperados (crear/tocar):**

- `projects/backend/src/test/java/com/example/backend/features/auth/AuthControllerTest.java`

## Fuera de alcance (no tocar)

- Modificar código de producción (si falla, se corrige en su UT).

## Resultado esperado

Todos los casos pasan en verde.

## Validación

1. `./mvnw test` pasa.
2. Cada CA de la HU tiene al menos un test (ver tabla del README).

## Seguridad

Evidencia de A01/A07/A10 y de que el CSRF no se salta desde web.
