# Unidades de trabajo — HU-INICIAL-Convenciones-FE

**Historia:** URL de la web en inglés y barriles.
**Fuente HU:** [`HU-INICIAL-Convenciones.md`](../../../HistoriasDeUsuario/HU-INICIAL-Convenciones.md)
**Recurso HTTP / contrato:** Rutas de la web

## Antes de implementar cualquier UT

Leer las rules de [`frontend/`](../../../../.claude/rules/frontend/) y [`SEGURIDAD_OWASP.md`](../../../../.claude/rules/SEGURIDAD_OWASP.md). Estas fichas **no** sustituyen esas reglas: si hay conflicto, ganan las rules del proyecto.

## Decisiones de diseño

1. Primero las rutas y después los barriles, para no tocar dos veces los mismos imports.
2. `components/ui/` queda sin barril por el CLI de shadcn.

## Seguridad

Los guards no cambian.

## Contrato de datos / recurso HTTP

Ver la UT 01.

## Orden de ejecución

| Código | Nombre | Depende de |
|--------|--------|------------|
| [UT-HUI-CONVENCIONES-FE-01](./UT-HUI-CONVENCIONES-FE-01.md) | Rutas en inglés | — |
| [UT-HUI-CONVENCIONES-FE-02](./UT-HUI-CONVENCIONES-FE-02.md) | Barriles | 01 |

**Cadena:** 01 → 02

## Criterios de aceptación cubiertos

| CA | Descripción | UTs |
|----|-------------|-----|
| CA01 | URL de la web | 01 |
| CA03 | URL anteriores | 01 |
| CA04 | Misma protección | 01 |
| CA05 | Menú | 01 |
| CA06 | Barriles | 02 |
| CA07 | Comprobaciones | 02 |
| CA08 | Sin regresiones | 02 |

## Follow-ups (no se implementan en estas UT)

- Barriles en la app móvil, si se decide esa convención.

## Qué NO incluyen estas UT

- Textos en otros idiomas.
