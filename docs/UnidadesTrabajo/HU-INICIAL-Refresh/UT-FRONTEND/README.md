# Unidades de trabajo — HU-INICIAL-Refresh-FE

**Historia:** Renovación automática de sesión en el cliente web.
**Fuente HU:** [`HU-INICIAL-Refresh.md`](../../../HistoriasDeUsuario/HU-INICIAL-Refresh.md)
**Recurso HTTP / contrato:** Interceptor de `apiClient`.

## Antes de implementar cualquier UT

Leer las rules de [`frontend/`](../../../../.claude/rules/frontend/) y [`SEGURIDAD_OWASP.md`](../../../../.claude/rules/SEGURIDAD_OWASP.md). Estas fichas **no** sustituyen esas reglas: si hay conflicto, ganan las rules del proyecto.

## Orden de ejecución

| Código | Nombre | Depende de |
|--------|--------|------------|
| [UT-HUI-REFRESH-FE-01](./UT-HUI-REFRESH-FE-01.md) | Interceptor de refresh con una sola llamada concurrente | — |
| [UT-HUI-REFRESH-FE-02](./UT-HUI-REFRESH-FE-02.md) | Tests del interceptor | UT-HUI-REFRESH-FE-01 |

**Cadena:** 01 → 02

## Criterios de aceptación cubiertos

| CA (HU-INICIAL-Refresh) | Descripción | UTs |
|---|---|---|
| CA02 | Refresh fallido vuelve a login | 01, 02 |
| CA04 | Un solo refresh concurrente | 01, 02 |

## Follow-ups (no se implementan en estas UT)

- Ninguno por ahora.

## Qué NO incluyen estas UT

- Logout y recuperación.
