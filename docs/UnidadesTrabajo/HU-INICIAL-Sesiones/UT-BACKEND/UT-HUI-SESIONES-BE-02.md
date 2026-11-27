# UT-HUI-SESIONES-BE-02 — Cerrar una sesión y cerrar todas las demás

| Campo | Valor |
|-------|-------|
| **Código** | `UT-HUI-SESIONES-BE-02` |
| **Nombre** | Cerrar una sesión y cerrar todas las demás |
| **Historia** | HU-INICIAL-Sesiones-BE |
| **Depende de** | UT-HUI-SESIONES-BE-01 |

## Antes de implementar

Leer `backend/SEGURIDAD_AUTH_BACKEND.md`, `backend/BITACORA_BACKEND.md`, `backend/GUIA_ERRORES_BACKEND.md` y `.claude/rules/SEGURIDAD_OWASP.md`.

## Solicitud de negocio

El usuario debe poder cortar un acceso que no reconoce sin quedarse él fuera.

## Objetivo

Existen `DELETE /api/v1/auth/sesiones/{codigo}` y `POST /api/v1/auth/sesiones/cerrar-otras`.

## Alcance

Ambos en `MisSesionesService`, cada uno en **una** transacción con `usarUsuario`:

1. Bloquear las sesiones `ACTIVA` del usuario con `SesionRepository.bloquearPorUsuarioYEstado` (ya existe; su orden fijo evita interbloqueos).
2. **La sesión actual tiene que estar en ese resultado**; si no, 401 `ERR_AUTH_04` sin cerrar nada. Bloquear solo la sesión objetivo no alcanza: dos cierres cruzados pasarían ambos la autenticación y se cerrarían los dos. Al esperar el bloqueo, PostgreSQL (`READ COMMITTED`) deja fuera las filas que otra transacción acaba de revocar.
3. `cerrarUna`: si el `codigo` es el actual, 400 `ERR_AUTH_10` (se puede comprobar antes de tocar la base). Si está entre las bloqueadas, `SesionService.revocar(sesion, USUARIO)`. Si no, buscarla **del usuario**: existe → 200 sin evento (ya estaba cerrada); no existe o es ajena → 404 `ERR_AUTH_09`, el mismo para ambos casos.
4. `cerrarOtras`: revoca todas las bloqueadas salvo la actual (incluidas las vencidas) y devuelve cuántas.
5. Por cada sesión revocada, en la misma transacción: bitácora `AUTH_SESION_CERRADA` (entidad `sesion`, `detalle` con `motivo: USUARIO`) y log `INFO` con códigos, sin IP ni user agent.

- `ERR_AUTH_10` (la sesión actual se cierra con «Cerrar sesión») en `AuthErrorCodes` y `docs/errors/`. Ampliar el significado documentado de `ERR_AUTH_09` y sus «Dónde se lanza».
- El mensaje de `cerrar-otras` indica cuántas se cerraron; `data` va vacío.
- Terminados los endpoints: actualizar `docs/api/openapi.json` (si no, `OpenApiContractTest` falla).

**Archivos esperados (crear/tocar):**

- `projects/backend/src/main/java/com/example/backend/features/auth/service/MisSesionesService.java`
- `projects/backend/src/main/java/com/example/backend/features/auth/controller/MisSesionesController.java`
- `projects/backend/src/main/java/com/example/backend/features/auth/AuthErrorCodes.java`
- `projects/backend/src/main/java/com/example/backend/features/auth/AuthAcciones.java`
- `docs/errors/auth/ERRORES_AUTH.md`
- `docs/errors/README.md`
- `docs/api/openapi.json`

## Seguridad

API1: propiedad comprobada y 404 uniforme. API5: sin contexto de administrador. A07: solo una sesión vigente cierra otras. CSRF activo en web. `revocar` corta el access token al instante y, si Redis falla, no confirma (falla cerrado). Sin migración: `USUARIO` ya está en el `CHECK`.

## Fuera de alcance (no tocar)

- Cerrar la sesión actual (HU Logout).
- `SesionAdminService`.
- Límite propio de peticiones: exige sesión y ya aplica el global.

## Resultado esperado

El usuario cierra otras sesiones y la suya sigue funcionando.

## Validación

1. `curl`: cerrar otra → su access token da 401; cerrar la actual → 400; una ajena → 404.
2. `./mvnw test` en verde.
