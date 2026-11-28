# Unidades de trabajo — HU-INICIAL-Convenciones-MOB

**Historia:** Rutas de la app en inglés.
**Fuente HU:** [`HU-INICIAL-Convenciones.md`](../../../HistoriasDeUsuario/HU-INICIAL-Convenciones.md)
**Recurso HTTP / contrato:** Rutas de la app

## Antes de implementar cualquier UT

Leer las rules de [`mobile/`](../../../../.claude/rules/mobile/) y [`SEGURIDAD_OWASP.md`](../../../../.claude/rules/SEGURIDAD_OWASP.md). Estas fichas **no** sustituyen esas reglas: si hay conflicto, ganan las rules del proyecto.

## Decisiones de diseño

Una sola UT.

## Seguridad

El `redirect` no cambia.

## Contrato de datos / recurso HTTP

Ver la UT 01.

## Orden de ejecución

| Código | Nombre | Depende de |
|--------|--------|------------|
| [UT-HUI-CONVENCIONES-MOB-01](./UT-HUI-CONVENCIONES-MOB-01.md) | Rutas en inglés | — |

**Cadena:** 01

## Criterios de aceptación cubiertos

| CA | Descripción | UTs |
|----|-------------|-----|
| CA01 | Rutas de la app | 01 |
| CA02 | Llamadas a la API | 01 |

## Follow-ups (no se implementan en estas UT)

- Validación en emulador (al final del proyecto).

## Qué NO incluyen estas UT

- Barriles en Dart.
