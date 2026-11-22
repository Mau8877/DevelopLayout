# Unidades de trabajo — HU-INICIAL-Bitacora-FE

**Historia:** Pantalla de bitácora para el administrador.
**Fuente HU:** [`HU-INICIAL-Bitacora.md`](../../../HistoriasDeUsuario/HU-INICIAL-Bitacora.md)
**Recurso HTTP / contrato:** Ruta `_authenticated/_admin/bitacora`.

## Antes de implementar cualquier UT

Leer las rules de [`frontend/`](../../../../.claude/rules/frontend/) y [`SEGURIDAD_OWASP.md`](../../../../.claude/rules/SEGURIDAD_OWASP.md). Estas fichas **no** sustituyen esas reglas: si hay conflicto, ganan las rules del proyecto.

## Orden de ejecución

| Código | Nombre | Depende de |
|--------|--------|------------|
| [UT-HUI-BITACORA-FE-01](./UT-HUI-BITACORA-FE-01.md) | Hooks y filtros de bitácora | — |
| [UT-HUI-BITACORA-FE-02](./UT-HUI-BITACORA-FE-02.md) | Pantalla Bitácora (admin) | UT-HUI-BITACORA-FE-01 |
| [UT-HUI-BITACORA-FE-03](./UT-HUI-BITACORA-FE-03.md) | Tests de la pantalla de bitácora | UT-HUI-BITACORA-FE-02 |

**Cadena:** 01 → 02 → 03

## Criterios de aceptación cubiertos

| CA (HU-INICIAL-Bitacora) | Descripción | UTs |
|---|---|---|
| CA07 | Filtrar y paginar sin recargar | 01, 02, 03 |

## Follow-ups (no se implementan en estas UT)

- Ninguno por ahora.

## Qué NO incluyen estas UT

- Mobile (el panel de administración es web).
