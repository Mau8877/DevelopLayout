# Unidades de trabajo — HU-INICIAL-SesionesAdmin-FE

**Historia:** Pantalla de administración de sesiones.
**Fuente HU:** [`HU-INICIAL-SesionesAdmin.md`](../../../HistoriasDeUsuario/HU-INICIAL-SesionesAdmin.md)
**Recurso HTTP / contrato:** Ruta `_authenticated/_admin/sesiones-usuarios`.

## Antes de implementar cualquier UT

Leer las rules de [`frontend/`](../../../../.claude/rules/frontend/) y [`SEGURIDAD_OWASP.md`](../../../../.claude/rules/SEGURIDAD_OWASP.md). Estas fichas **no** sustituyen esas reglas: si hay conflicto, ganan las rules del proyecto.

## Orden de ejecución

| Código | Nombre | Depende de |
|--------|--------|------------|
| [UT-HUI-SESIONESADMIN-FE-01](./UT-HUI-SESIONESADMIN-FE-01.md) | Hooks de administración de sesiones | — |
| [UT-HUI-SESIONESADMIN-FE-02](./UT-HUI-SESIONESADMIN-FE-02.md) | Pantalla Sesiones de usuarios (admin) | UT-HUI-SESIONESADMIN-FE-01 |
| [UT-HUI-SESIONESADMIN-FE-03](./UT-HUI-SESIONESADMIN-FE-03.md) | Tests de la pantalla de admin | UT-HUI-SESIONESADMIN-FE-02 |

**Cadena:** 01 → 02 → 03

## Criterios de aceptación cubiertos

| CA (HU-INICIAL-SesionesAdmin) | Descripción | UTs |
|---|---|---|
| CA01 | Listar | 01, 02, 03 |
| CA03 | Cerrar una | 01, 02, 03 |
| CA04 | Cerrar todas | 01, 02, 03 |

## Follow-ups (no se implementan en estas UT)

- Ninguno por ahora.

## Qué NO incluyen estas UT

- Mobile (el panel de administración es web).
