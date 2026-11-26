# UT-HUI-BITACORA-BE-01 — Consulta de la bitácora (service y endpoint)

| Campo | Valor |
|-------|-------|
| **Código** | `UT-HUI-BITACORA-BE-01` |
| **Nombre** | Consulta de la bitácora (service y endpoint) |
| **Historia** | HU-INICIAL-Bitacora-BE |
| **Depende de** | Base: BitacoraRepository y BitacoraService (HU-INICIAL-Base) |

## Antes de implementar

Leer `backend/BITACORA_BACKEND.md`, `backend/ENDPOINTS_BACKEND.md`, `backend/RESPONSES_BACKEND.md` y `.claude/rules/SEGURIDAD_OWASP.md`.

## Solicitud de negocio

Auditar exige poder buscar eventos con filtros y sin exponer datos sensibles.

## Objetivo

Existe `GET /api/v1/bitacora` paginado y filtrable, solo para `SUPER_ADMIN`.

## Alcance

- `features/bitacora/service/BitacoraQueryService.java` con `@PreAuthorize("hasRole('SUPER_ADMIN')")`: fija `RlsContext.usarAdmin()` y consulta con `Specification` (acción, módulo, resultado, usuario, `desde`, `hasta`), orden `created_at` descendente; `desde > hasta` → `ValidationException` `ERR_BIT_01`.
- `common/bitacora/repository/BitacoraConsultaRepository.java`: consulta paginada con Criteria; **solo lectura** (sin `JpaSpecificationExecutor`). Paginación validada con `common/util/Paginacion`.
- `features/bitacora/dto/BitacoraResponse.java` (`codigo`, `fecha`, `usuarioCodigo`, `accion`, `modulo`, `entidad`, `entidadCodigo`, `resultado`, `ip`, `detalle`; sin `id`) y `controller/BitacoraController.java` delgado con `page`, `pageSize` (máximo 100) y `filter[...]`.
- `ERR_BIT_01` en `BitacoraErrorCodes` y su sección en `docs/errors/bitacora/ERRORES_BITACORA.md` + índice.

**Archivos esperados (crear/tocar):**

- `projects/backend/src/main/java/com/example/backend/features/bitacora/service/BitacoraQueryService.java`
- `projects/backend/src/main/java/com/example/backend/features/bitacora/dto/BitacoraResponse.java`
- `projects/backend/src/main/java/com/example/backend/features/bitacora/controller/BitacoraController.java`
- `projects/backend/src/main/java/com/example/backend/features/bitacora/BitacoraErrorCodes.java`
- `projects/backend/src/main/java/com/example/backend/common/bitacora/repository/BitacoraConsultaRepository.java`
- `docs/api/openapi.json` y `projects/frontend/src/shared/api/schema.d.ts` (contrato)
- `docs/errors/bitacora/ERRORES_BITACORA.md`
- `docs/errors/README.md`

## Fuera de alcance (no tocar)

- Purga (UT 02).
- Endpoints de escritura o borrado (no existen).

## Resultado esperado

Un admin consulta eventos con filtros; un usuario normal recibe 403.

## Validación

1. `curl` con admin: lista paginada descendente; filtros por acción y fechas; `desde > hasta` → 400 `ERR_BIT_01`; con usuario normal → 403.
2. La respuesta no incluye `id`.

## Seguridad

A09/API4/API5: solo `SUPER_ADMIN`, paginación acotada, filtros validados.
