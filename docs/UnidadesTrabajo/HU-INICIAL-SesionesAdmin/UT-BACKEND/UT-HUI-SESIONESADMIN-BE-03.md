# UT-HUI-SESIONESADMIN-BE-03 — Tests de administración de sesiones

| Campo | Valor |
|-------|-------|
| **Código** | `UT-HUI-SESIONESADMIN-BE-03` |
| **Nombre** | Tests de administración de sesiones |
| **Historia** | HU-INICIAL-SesionesAdmin-BE |
| **Depende de** | UT-HUI-SESIONESADMIN-BE-02 |

## Antes de implementar

Leer `backend/TESTING_BACKEND.md` y `.claude/rules/SEGURIDAD_OWASP.md`.

## Solicitud de negocio

Probar la autorización por función.

## Objetivo

Tests de CA01–CA07.

## Alcance

- `SesionAdminControllerTest`: admin permitido, `USUARIO` → 403 en todas, correo inexistente → vacío, 404 de sesión inexistente, revocación inmediata, eventos con actor y afectado, CSRF, sesiones vencidas fuera del listado, respuesta sin hashes, `pageSize` mayor a 100 → 400, y que el contexto de administrador no puede modificar usuarios.

**Archivos esperados (crear/tocar):**

- `projects/backend/src/test/java/com/example/backend/features/auth/SesionAdminControllerTest.java`

## Fuera de alcance (no tocar)

- Código de producción.

## Resultado esperado

Pasan.

## Validación

1. `./mvnw test` en verde.

## Seguridad

Evidencia de API5.
