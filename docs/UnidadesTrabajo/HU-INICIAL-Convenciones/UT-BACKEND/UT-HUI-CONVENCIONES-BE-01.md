# UT-HUI-CONVENCIONES-BE-01 — URL de la API en inglés

| Campo | Valor |
|-------|-------|
| **Código** | `UT-HUI-CONVENCIONES-BE-01` |
| **Nombre** | URL de la API en inglés |
| **Historia** | HU-INICIAL-Convenciones-BE |
| **Depende de** | — |

## Antes de implementar

ENDPOINTS_BACKEND, SEGURIDAD_AUTH_BACKEND (endpoints públicos y límites) y CI_CD (prueba de producción).

## Solicitud de negocio

Las direcciones de la plantilla tienen que ser uniformes antes de que haya clientes.

## Objetivo

Todas las rutas de la API en inglés, con la misma protección.

## Alcance

| Antes (bajo `/api/v1`) | Después |
|---|---|
| `/auth/sesiones` y `/auth/sesiones/{codigo}` | `/auth/sessions` y `/auth/sessions/{codigo}` |
| `/auth/sesiones/cerrar-otras` | `/auth/sessions/revoke-others` |
| `/admin/sesiones/buscar` y `/admin/sesiones/{codigo}` | `/admin/sessions/search` y `/admin/sessions/{codigo}` |
| `/admin/usuarios/{codigoUsuario}/sesiones/cerrar` | `/admin/users/{codigoUsuario}/sessions/revoke` |
| `/bitacora` | `/audit-log` |
| `/usuarios` y `/usuarios/buscar` | `/users` y `/users/search` |
| `/usuarios/{codigo}/desactivar` y `/reactivar` | `/users/{codigo}/deactivate` y `/reactivate` |
| `/usuarios/{codigo}/reenviar-invitacion` | `/users/{codigo}/resend-invitation` |
| `/usuarios/{codigo}/roles` | `/users/{codigo}/roles` |

Filtros de `/audit-log`: `filter[accion]` → `filter[action]`, `filter[modulo]` → `filter[module]`, `filter[resultado]` → `filter[result]`, `filter[usuarioCodigo]` → `filter[userCode]`, `desde` → `from`, `hasta` → `to`. El resto de `/auth/*` ya está en inglés.

- Solo cambian las URL: ni los métodos de Java ni los `operationId`, DTO, campos, códigos o valores. Los nombres entre llaves no forman parte de la URL y se mantienen.
- Revisar todo lo que nombra rutas: `SecurityConfig`, `RateLimitFilter`, cookies, nginx, `prod-smoke.mjs` y pruebas.
- Actualizar `docs/api/openapi.json`, `docs/errors/` y las reglas que describen rutas actuales (SEGURIDAD_AUTH_BACKEND, BITACORA_BACKEND y las que aparezcan al buscar).

**Archivos esperados (crear/tocar):**

- `projects/backend/src/main/java/com/example/backend/features/`
- `projects/backend/src/main/java/com/example/backend/config/SecurityConfig.java`
- `projects/backend/src/main/java/com/example/backend/security/RateLimitFilter.java`
- `projects/backend/src/test/java/com/example/backend/`
- `docs/api/openapi.json`
- `docs/errors/`
- `scripts/prod-smoke.mjs`
- `.claude/rules/`

## Seguridad

A01/API9: ninguna ruta cambia de permiso y las anteriores dejan de existir. Los datos personales siguen fuera de la URL.

## Fuera de alcance (no tocar)

- Cambiar los clientes (UT FE-01 y MOB-01).
- Reescribir historias y tareas ya cerradas: son registro histórico.
- Redirecciones.

## Resultado esperado

La API responde solo en las rutas nuevas.

## Validación

1. Cada ruta nueva mantiene sus pruebas de acceso permitido y denegado (401, 403 por rol y sin CSRF).
2. Una ruta anterior responde como inexistente.
3. El contrato solo cambia en rutas y nombres de filtros.
4. Ninguna ruta anterior queda en el código, la configuración ni los scripts.
5. `./mvnw test` en verde. La prueba de producción se corre al terminar FE-01.
