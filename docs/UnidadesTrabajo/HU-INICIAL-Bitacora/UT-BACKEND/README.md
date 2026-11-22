# Unidades de trabajo — HU-INICIAL-Bitacora-BE

**Historia:** Consultar la bitácora y purgar por retención.
**Fuente HU:** [`HU-INICIAL-Bitacora.md`](../../../HistoriasDeUsuario/HU-INICIAL-Bitacora.md)
**Recurso HTTP / contrato:** `GET /api/v1/bitacora`.

## Antes de implementar cualquier UT

Leer las rules de [`backend/`](../../../../.claude/rules/backend/) y [`SEGURIDAD_OWASP.md`](../../../../.claude/rules/SEGURIDAD_OWASP.md). Estas fichas **no** sustituyen esas reglas: si hay conflicto, ganan las rules del proyecto.

## Decisiones de diseño

- La purga usa una función `SECURITY DEFINER`, así `app_runtime` nunca tiene `DELETE`.
- Instancia única: la tarea programada no usa bloqueo distribuido.

## Orden de ejecución

| Código | Nombre | Depende de |
|--------|--------|------------|
| [UT-HUI-BITACORA-BE-01](./UT-HUI-BITACORA-BE-01.md) | Consulta de la bitácora (service y endpoint) | Base: BitacoraRepository y BitacoraService (HU-INICIAL-Base) |
| [UT-HUI-BITACORA-BE-02](./UT-HUI-BITACORA-BE-02.md) | Purga diaria por retención | UT-HUI-BITACORA-BE-01 |
| [UT-HUI-BITACORA-BE-03](./UT-HUI-BITACORA-BE-03.md) | Tests de consulta de bitácora | UT-HUI-BITACORA-BE-02 |

**Cadena:** 01 → 02 → 03

## Criterios de aceptación cubiertos

| CA (HU-INICIAL-Bitacora) | Descripción | UTs |
|---|---|---|
| CA01 | Listado | 01, 03 |
| CA02 | Filtros | 01, 03 |
| CA03 | Rango inválido | 01, 03 |
| CA04 | 403 | 01, 03 |
| CA05 | Sin secretos | 03 |
| CA06 | Retención | 02 |

## Follow-ups (no se implementan en estas UT)

- Exportar la bitácora a CSV.
- Alertas en Grafana sobre eventos de fallo.

## Qué NO incluyen estas UT

- Escritura de eventos (Base) y mobile.
