# UT-HUI-USUARIOS-BE-05 — Listado y búsqueda de usuarios

| Campo | Valor |
|-------|-------|
| **Código** | `UT-HUI-USUARIOS-BE-05` |
| **Nombre** | Listado y búsqueda de usuarios |
| **Historia** | HU-INICIAL-Usuarios-BE |
| **Depende de** | 01 |

## Antes de implementar

ENDPOINTS_BACKEND (dato personal fuera de la URL, paginación), RESPONSES_BACKEND y `BitacoraConsultaRepository`.

## Solicitud de negocio

El administrador necesita encontrar cuentas y ver su situación.

## Objetivo

Endpoint paginado `POST /api/v1/usuarios/buscar`.

## Alcance

- `BuscarUsuariosRequest` (`ignoreUnknown = false`): `page`, `pageSize`, `search` (opcional, máx. 255), `estado`, `rol` (opcionales). Los criterios se combinan con **Y**.
- `search` busca parte del correo sin distinguir mayúsculas; `%` y `_` se tratan como texto.
- `UsuarioResponse`: `codigo`, `email`, `roles`, `estado`, `invitacionPendiente`, `creadoEn`.
- `UsuarioBusquedaRepository` de solo lectura (Criteria; sin `JpaSpecificationExecutor`). Orden: alta más reciente primero y desempate por `id`.
- Los roles de la página se leen en **una** consulta adicional, no una por usuario.
- `UsuarioAdminService.buscar` con `@PreAuthorize("hasRole('SUPER_ADMIN')")` y `usarAdmin()`; paginación con `Paginacion`.
- Actualizar `docs/api/openapi.json` en esta misma UT (`OpenApiContractTest`). Los nombres de los métodos de controller son únicos entre todos los controllers; si no, springdoc renombra operaciones que ya existían.

**Archivos esperados (crear/tocar):**

- `projects/backend/src/main/java/com/example/backend/features/usuarios/controller/UsuarioController.java`
- `projects/backend/src/main/java/com/example/backend/features/usuarios/service/UsuarioAdminService.java`
- `projects/backend/src/main/java/com/example/backend/features/usuarios/repository/UsuarioBusquedaRepository.java`
- `projects/backend/src/main/java/com/example/backend/features/usuarios/dto/BuscarUsuariosRequest.java`
- `projects/backend/src/main/java/com/example/backend/features/usuarios/dto/UsuarioResponse.java`
- `projects/backend/src/test/java/com/example/backend/features/usuarios/UsuarioBusquedaControllerTest.java`
- `docs/api/openapi.json`

## Seguridad

A01/API5: solo `SUPER_ADMIN`. API3: sin hashes ni `id`. API4: `pageSize` máximo 100. El texto buscado no va en la URL ni se loguea.

## Fuera de alcance (no tocar)

- Cualquier cambio de datos.
- Ordenamiento configurable.

## Resultado esperado

El administrador obtiene el listado filtrado y paginado.

## Validación

1. Paginación por defecto y máxima; búsqueda parcial; cada filtro y su combinación; sin resultados, lista vacía; `%` no devuelve todo.
2. Cada fila trae roles, estado, invitación pendiente y fecha, sin campos de más.
3. Acceso denegado: sin sesión 401, rol `USUARIO` 403 (también llamando al service) y web sin CSRF 403.
