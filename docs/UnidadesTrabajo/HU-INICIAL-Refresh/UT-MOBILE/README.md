# Unidades de trabajo — HU-INICIAL-Refresh-MOB

**Historia:** Renovación automática de sesión en la app.
**Fuente HU:** [`HU-INICIAL-Refresh.md`](../../../HistoriasDeUsuario/HU-INICIAL-Refresh.md)
**Recurso HTTP / contrato:** Interceptor de Dio.

## Antes de implementar cualquier UT

Leer las rules de [`mobile/`](../../../../.claude/rules/mobile/) y [`SEGURIDAD_OWASP.md`](../../../../.claude/rules/SEGURIDAD_OWASP.md). Estas fichas **no** sustituyen esas reglas: si hay conflicto, ganan las rules del proyecto.

## Orden de ejecución

| Código | Nombre | Depende de |
|--------|--------|------------|
| [UT-HUI-REFRESH-MOB-01](./UT-HUI-REFRESH-MOB-01.md) | Interceptor Dio de refresh (QueuedInterceptor) | — |
| [UT-HUI-REFRESH-MOB-02](./UT-HUI-REFRESH-MOB-02.md) | Tests del refresh mobile | UT-HUI-REFRESH-MOB-01 |

**Cadena:** 01 → 02

## Criterios de aceptación cubiertos

| CA (HU-INICIAL-Refresh) | Descripción | UTs |
|---|---|---|
| CA01 | Rotación | 01, 02 |
| CA02 | Fallo vuelve a login | 01, 02 |
| CA04 | Un solo refresh concurrente | 01, 02 |

## Follow-ups (no se implementan en estas UT)

- Ninguno por ahora.

## Qué NO incluyen estas UT

- Logout y recuperación.
