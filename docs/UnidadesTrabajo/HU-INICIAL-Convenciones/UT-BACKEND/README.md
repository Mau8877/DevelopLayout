# Unidades de trabajo — HU-INICIAL-Convenciones-BE

**Historia:** La API con todas sus URL en inglés.
**Fuente HU:** [`HU-INICIAL-Convenciones.md`](../../../HistoriasDeUsuario/HU-INICIAL-Convenciones.md)
**Recurso HTTP / contrato:** `/api/v1/**`

## Antes de implementar cualquier UT

Leer las rules de [`backend/`](../../../../.claude/rules/backend/) y [`SEGURIDAD_OWASP.md`](../../../../.claude/rules/SEGURIDAD_OWASP.md). Estas fichas **no** sustituyen esas reglas: si hay conflicto, ganan las rules del proyecto.

## Decisiones de diseño

Schema: no crea ni cambia tablas.

1. Cambian solo las URL; el contrato de datos, los códigos y la base siguen igual.
2. Sin redirecciones ni versión nueva: la plantilla no está desplegada.

## Seguridad

Las mismas autorizaciones, CSRF y límites en las rutas nuevas.

## Contrato de datos / recurso HTTP

La tabla de rutas está en la UT 01.

## Orden de ejecución

| Código | Nombre | Depende de |
|--------|--------|------------|
| [UT-HUI-CONVENCIONES-BE-01](./UT-HUI-CONVENCIONES-BE-01.md) | URL en inglés | — |

**Cadena:** 01

## Criterios de aceptación cubiertos

| CA | Descripción | UTs |
|----|-------------|-----|
| CA02 | API en inglés | 01 |
| CA03 | Rutas anteriores | 01 |
| CA04 | Misma protección | 01 |

## Follow-ups (no se implementan en estas UT)

- Nada previsto.

## Qué NO incluyen estas UT

- Traducir campos del JSON o la base de datos.
