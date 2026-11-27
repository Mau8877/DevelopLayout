# Unidades de trabajo — HU-INICIAL-Roles-FE

**Historia:** Cambio de roles desde la pantalla de usuarios.
**Fuente HU:** [`HU-INICIAL-Roles.md`](../../../HistoriasDeUsuario/HU-INICIAL-Roles.md)
**Recurso HTTP / contrato:** Ruta `/usuarios` (modal)

## Antes de implementar cualquier UT

Leer las rules de [`frontend/`](../../../../.claude/rules/frontend/) y [`SEGURIDAD_OWASP.md`](../../../../.claude/rules/SEGURIDAD_OWASP.md). Estas fichas **no** sustituyen esas reglas: si hay conflicto, ganan las rules del proyecto.

## Decisiones de diseño

Una sola UT: reutiliza la pantalla, el grupo de casillas y la confirmación de HU-INICIAL-Usuarios.

## Seguridad

Solo la ve un administrador; la autorización real es del backend.

## Contrato de datos / recurso HTTP

`PUT /api/v1/usuarios/{codigo}/roles` con `{ roles }` (ver el [README de backend](../UT-BACKEND/README.md)).

## Orden de ejecución

| Código | Nombre | Depende de |
|--------|--------|------------|
| [UT-HUI-ROLES-FE-01](./UT-HUI-ROLES-FE-01.md) | Cambiar roles | — |

**Cadena:** 01

## Criterios de aceptación cubiertos

| CA | Descripción | UTs |
|----|-------------|-----|
| CA01 | Conjunto exacto | 01 |
| CA03 | Datos inválidos | 01 |
| CA05 | Rol propio | 01 |
| CA11 | Confirmación y refresco | 01 |

## Follow-ups (no se implementan en estas UT)

- Nada previsto.

## Qué NO incluyen estas UT

- Mobile.
- Administrar el catálogo de roles.
