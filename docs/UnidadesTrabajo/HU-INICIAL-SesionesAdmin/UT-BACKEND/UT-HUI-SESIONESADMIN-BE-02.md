# UT-HUI-SESIONESADMIN-BE-02 — Endpoints de administración de sesiones

| Campo | Valor |
|-------|-------|
| **Código** | `UT-HUI-SESIONESADMIN-BE-02` |
| **Nombre** | Endpoints de administración de sesiones |
| **Historia** | HU-INICIAL-SesionesAdmin-BE |
| **Depende de** | UT-HUI-SESIONESADMIN-BE-01 |

## Antes de implementar

Leer `backend/SEGURIDAD_AUTH_BACKEND.md`, `backend/ENDPOINTS_BACKEND.md` y `.claude/rules/SEGURIDAD_OWASP.md`.

## Solicitud de negocio

Expone la función al panel de administración.

## Objetivo

Existen `POST /api/v1/admin/sesiones/buscar` (body `email`, `page`, `pageSize`), `DELETE /api/v1/admin/sesiones/{codigo}` y `POST /api/v1/admin/usuarios/{codigoUsuario}/sesiones/cerrar`.

## Alcance

- `features/auth/controller/SesionAdminController.java` delgado; listado paginado con `meta` y `pageSize` máximo 100; autenticados, CSRF en web; la autorización real está en el service.

**Archivos esperados (crear/tocar):**

- `projects/backend/src/main/java/com/example/backend/features/auth/controller/SesionAdminController.java`
- `projects/backend/src/main/java/com/example/backend/features/auth/dto/BuscarSesionesRequest.java`
- `projects/backend/src/main/java/com/example/backend/common/util/Paginacion.java`
- `docs/api/openapi.json` y `projects/frontend/src/shared/api/schema.d.ts` (contrato)

## Fuera de alcance (no tocar)

- Lógica de negocio.

## Resultado esperado

Un admin usa los endpoints con cookies y CSRF.

## Validación

1. `curl` con admin: lista, cierra una, cierra todas; con usuario normal → 403.

## Seguridad

A01/API5.
