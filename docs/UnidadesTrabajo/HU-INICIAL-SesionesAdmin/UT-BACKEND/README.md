# Unidades de trabajo — HU-INICIAL-SesionesAdmin-BE

**Historia:** Administrador lista y cierra sesiones de cualquier usuario.
**Fuente HU:** [`HU-INICIAL-SesionesAdmin.md`](../../../HistoriasDeUsuario/HU-INICIAL-SesionesAdmin.md)
**Recurso HTTP / contrato:** `POST /api/v1/admin/sesiones/buscar`, `DELETE /api/v1/admin/sesiones/{codigo}`, `POST /api/v1/admin/usuarios/{codigoUsuario}/sesiones/cerrar`.

## Antes de implementar cualquier UT

Leer las rules de [`backend/`](../../../../.claude/rules/backend/) y [`SEGURIDAD_OWASP.md`](../../../../.claude/rules/SEGURIDAD_OWASP.md). Estas fichas **no** sustituyen esas reglas: si hay conflicto, ganan las rules del proyecto.

## Decisiones de diseño

- Schema: `authentication` — existente — solo agrega una política RLS.
- No hay listado general de usuarios: se busca por correo exacto (evita enumerar usuarios).
- La búsqueda es `POST` con el correo en el body: no queda en URLs ni en logs de acceso.
- Política `usuario_lectura_admin` (`FOR SELECT` con `app.is_admin`). No se reutiliza `app.login_email`, que además permite modificar la fila.
- El correo se resuelve a través de un service de `features/usuarios` (dueño del dato).
- Primer listado paginado del proyecto: la validación de `page`/`pageSize` (máximo 100) va en `common/util`.
- `RlsContext.usarAdmin()` solo se llama desde servicios con `@PreAuthorize` ya verificado.

## Orden de ejecución

| Código | Nombre | Depende de |
|--------|--------|------------|
| [UT-HUI-SESIONESADMIN-BE-01](./UT-HUI-SESIONESADMIN-BE-01.md) | SesionAdminService: listar y cerrar sesiones de usuarios | Refresh: SesionService.revocar (HU-INICIAL-Refresh) |
| [UT-HUI-SESIONESADMIN-BE-02](./UT-HUI-SESIONESADMIN-BE-02.md) | Endpoints de administración de sesiones | UT-HUI-SESIONESADMIN-BE-01 |
| [UT-HUI-SESIONESADMIN-BE-03](./UT-HUI-SESIONESADMIN-BE-03.md) | Tests de administración de sesiones | UT-HUI-SESIONESADMIN-BE-02 |

**Cadena:** 01 → 02 → 03

## Criterios de aceptación cubiertos

| CA (HU-INICIAL-SesionesAdmin) | Descripción | UTs |
|---|---|---|
| CA01 | Listar | 01, 02, 03 |
| CA02 | Correo inexistente | 01, 03 |
| CA03 | Cerrar una | 01, 02, 03 |
| CA04 | Cerrar todas | 01, 02, 03 |
| CA05 | 403 sin rol SUPER_ADMIN | 01, 02, 03 |
| CA06 | Bitácora actor y afectado | 01, 03 |
| CA07 | CSRF | 02, 03 |

## Follow-ups (no se implementan en estas UT)

- Módulo de gestión de usuarios con listado general (HU futura).

## Qué NO incluyen estas UT

- Mobile.
