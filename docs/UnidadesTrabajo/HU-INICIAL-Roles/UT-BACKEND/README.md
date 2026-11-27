# Unidades de trabajo — HU-INICIAL-Roles-BE

**Historia:** API para reemplazar el conjunto de roles de un usuario.
**Fuente HU:** [`HU-INICIAL-Roles.md`](../../../HistoriasDeUsuario/HU-INICIAL-Roles.md)
**Recurso HTTP / contrato:** `PUT /api/v1/usuarios/{codigo}/roles`

## Antes de implementar cualquier UT

Leer las rules de [`backend/`](../../../../.claude/rules/backend/) y [`SEGURIDAD_OWASP.md`](../../../../.claude/rules/SEGURIDAD_OWASP.md). Estas fichas **no** sustituyen esas reglas: si hay conflicto, ganan las rules del proyecto.

## Decisiones de diseño

Schema: authentication — existente — son asignaciones de roles.

1. Quitar un rol desactiva la asignación; no se borra.
2. La invariante de administradores la garantiza la guardia creada en HU-INICIAL-Usuarios; aquí no se agrega otro mecanismo.
3. El permiso para modificar asignaciones se agrega en esta historia, no antes.

## Seguridad

Solo `SUPER_ADMIN` con sesión vigente. RLS permite al administrador activar o desactivar asignaciones, no reapuntarlas ni borrarlas. Las sesiones de la persona se cierran y queda en la bitácora.

## Contrato de datos / recurso HTTP

| Método y ruta | Cuerpo | Éxito | Errores propios |
|---|---|---|---|
| `PUT /api/v1/usuarios/{codigo}/roles` | `roles` | 200, usuario | `ERR_US_01` 404, `ERR_US_03` 400, `ERR_US_07` 409 |

## Orden de ejecución

| Código | Nombre | Depende de |
|--------|--------|------------|
| [UT-HUI-ROLES-BE-01](./UT-HUI-ROLES-BE-01.md) | Permiso de base de datos | — |
| [UT-HUI-ROLES-BE-02](./UT-HUI-ROLES-BE-02.md) | Cambiar roles | 01 |
| [UT-HUI-ROLES-BE-03](./UT-HUI-ROLES-BE-03.md) | Cruces y documentación | 02 |

**Cadena:** 01 → 02 → 03

## Criterios de aceptación cubiertos

| CA | Descripción | UTs |
|----|-------------|-----|
| CA01 | Conjunto exacto | 02 |
| CA02 | Cierre de sesiones | 02, 03 |
| CA03 | Datos inválidos | 02 |
| CA04 | Mismo conjunto | 02 |
| CA05 | Rol propio | 02 |
| CA06 | Cruces | 03 |
| CA07 | Actor que ya no es administrador | 02 |
| CA08 | Reasignación | 01, 02 |
| CA09 | 401, 403 y 404 | 01, 02 |
| CA10 | Bitácora | 02 |

## Follow-ups (no se implementan en estas UT)

- Administrar el catálogo de roles.

## Qué NO incluyen estas UT

- Alta, estado o invitaciones.
- Permisos más finos que los roles.
- Mobile.
