# UT-HUI-SESIONES-BE-01 — Listar mis sesiones

| Campo | Valor |
|-------|-------|
| **Código** | `UT-HUI-SESIONES-BE-01` |
| **Nombre** | Listar mis sesiones |
| **Historia** | HU-INICIAL-Sesiones-BE |
| **Depende de** | — |

## Antes de implementar

Leer `backend/ENDPOINTS_BACKEND.md` (paginación), `backend/RESPONSES_BACKEND.md` y `backend/BASE_DE_DATOS_PRIVADA_RLS.md`.

## Solicitud de negocio

El usuario necesita ver desde dónde está abierta su cuenta.

## Objetivo

Existe `GET /api/v1/auth/sesiones`, paginado, con solo las sesiones del usuario autenticado.

## Alcance

- `features/auth/service/MisSesionesService.java` (nuevo): `listar(UUID usuario, UUID sesionActual, Pageable)`, `@Transactional(readOnly = true)`, con `RlsContext.usarUsuario`. **Nunca** `usarAdmin`.
- `SesionRepository`: consulta de las sesiones `ACTIVA` y no vencidas de **ese** usuario (filtro explícito además de RLS), ordenadas con la actual primero y después por `createdAt` e `id` descendentes, con su consulta de conteo.
- `features/auth/dto/MiSesionResponse.java`: `codigo`, `tipoCliente`, `ip`, `userAgent`, `creadaEn`, `ultimoUsoEn`, `expiraEn`, `actual`. Sin `usuarioCodigo`, `id` ni hashes.
- `features/auth/controller/MisSesionesController.java` en `/api/v1/auth/sesiones`: usa `@CurrentUserId` y `@CurrentSessionId`; paginación con `common/util/Paginacion` y `PageMeta`.
- `SecurityConfig` no cambia: la ruta es privada por defecto.

**Archivos esperados (crear/tocar):**

- `projects/backend/src/main/java/com/example/backend/features/auth/service/MisSesionesService.java`
- `projects/backend/src/main/java/com/example/backend/features/auth/controller/MisSesionesController.java`
- `projects/backend/src/main/java/com/example/backend/features/auth/dto/MiSesionResponse.java`
- `projects/backend/src/main/java/com/example/backend/features/auth/repository/SesionRepository.java`

## Seguridad

A01/API1: el usuario sale del token, no de un parámetro. API3: el DTO expone solo lo necesario. API4: `pageSize` máximo 100.

## Fuera de alcance (no tocar)

- Cerrar sesiones (UT 02).
- Endpoints y servicio de administración.

## Resultado esperado

Cada usuario ve sus sesiones, con la actual marcada y primero.

## Validación

1. Con dos usuarios, cada uno ve solo las suyas; sin token, 401.
2. `./mvnw test` y `checkstyle:check` en verde.
