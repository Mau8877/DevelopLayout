# Unidades de trabajo — HU-INICIAL-Sesiones-BE

**Historia:** El usuario ve sus sesiones y cierra una o todas las demás.
**Fuente HU:** [`HU-INICIAL-Sesiones.md`](../../../HistoriasDeUsuario/HU-INICIAL-Sesiones.md)
**Recurso HTTP / contrato:** `GET /api/v1/auth/sesiones`, `DELETE /api/v1/auth/sesiones/{codigo}`, `POST /api/v1/auth/sesiones/cerrar-otras`.

## Antes de implementar cualquier UT

Leer las rules de [`backend/`](../../../../.claude/rules/backend/) y [`SEGURIDAD_OWASP.md`](../../../../.claude/rules/SEGURIDAD_OWASP.md). Estas fichas **no** sustituyen esas reglas: si hay conflicto, ganan las rules del proyecto.

## Decisiones de diseño

Sin tablas ni migraciones nuevas: la política `sesion_aislamiento` ya limita cada usuario a sus filas y `app_runtime` ya tiene `UPDATE`.

- **Servicio y controller propios**, separados de los de administración: así ningún camino de usuario abre el contexto de administrador.
- **DTO propio** (`MiSesionResponse`): lleva `actual` y no lleva `usuarioCodigo`.
- **La sesión actual se identifica por el `sid`** del access token (`@CurrentSessionId`, ya existe).
- **Se bloquean todas las sesiones activas** antes de cerrar, y la actual debe seguir entre ellas.
- La actual va primero en el listado para que siempre esté en la primera página.

## Seguridad

- **Acceso:** autenticado; solo recursos propios (filtro explícito y RLS).
- **Consumo:** paginación con máximo 100 y límite global por IP.
- **Registro:** `AUTH_SESION_CERRADA` por sesión; sin IP ni user agent en logs.

## Contrato de datos / recurso HTTP

| Método y ruta | Éxito | Errores |
|---|---|---|
| `GET /api/v1/auth/sesiones?page&pageSize` | 200, `data` lista y `meta` | 400, 401 |
| `DELETE /api/v1/auth/sesiones/{codigo}` | 200 (también si ya estaba cerrada) | 400 `ERR_AUTH_10`; 401; 403 CSRF; 404 `ERR_AUTH_09` |
| `POST /api/v1/auth/sesiones/cerrar-otras` | 200, cantidad en `message` | 401; 403 CSRF |

## Orden de ejecución

| Código | Nombre | Depende de |
|--------|--------|------------|
| [UT-HUI-SESIONES-BE-01](./UT-HUI-SESIONES-BE-01.md) | Listar mis sesiones | — |
| [UT-HUI-SESIONES-BE-02](./UT-HUI-SESIONES-BE-02.md) | Cerrar una y cerrar todas las demás | 01 |
| [UT-HUI-SESIONES-BE-03](./UT-HUI-SESIONES-BE-03.md) | Tests | 02 |

**Cadena:** 01 → 02 → 03

## Criterios de aceptación cubiertos

| CA | Descripción | UTs |
|---|---|---|
| CA01 | Listado propio paginado | 01, 03 |
| CA02 | Cerrar una | 02, 03 |
| CA03 | Ajena o inexistente: 404 | 02, 03 |
| CA04 | Cerrar las demás | 02, 03 |
| CA05 | La actual: 400 | 02, 03 |
| CA06 | 401 y CSRF | 01, 02, 03 |
| CA07 | Sin datos internos | 01, 03 |
| CA08 | Sesión que ya no está activa | 02, 03 |
| CA09 | Cierres cruzados | 02, 03 |
| CA10 | Ya cerrada | 02, 03 |

## Follow-ups (no se implementan en estas UT)

- Avisar por correo de un inicio de sesión desde un dispositivo nuevo.

## Qué NO incluyen estas UT

- Cambios en la administración de sesiones.
- Cambio de contraseña con sesión iniciada.
