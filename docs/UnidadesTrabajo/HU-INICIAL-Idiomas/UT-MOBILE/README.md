# Unidades de trabajo — HU-INICIAL-Idiomas-MOB

**Historia:** La app completa en español o inglés, con selector.
**Fuente HU:** [`HU-INICIAL-Idiomas.md`](../../../HistoriasDeUsuario/HU-INICIAL-Idiomas.md)
**Recurso HTTP / contrato:** Toda la app

## Antes de implementar cualquier UT

Leer las rules de [`mobile/`](../../../../.claude/rules/mobile/) y [`SEGURIDAD_OWASP.md`](../../../../.claude/rules/SEGURIDAD_OWASP.md). Estas fichas **no** sustituyen esas reglas: si hay conflicto, ganan las rules del proyecto.

## Decisiones de diseño

1. **JSON por feature**, como en la web, con un cargador propio pequeño; no se usa `gen-l10n` (`.arb`). A cambio, las claves no se comprueban al compilar: lo hacen las pruebas.
2. Sin dependencias nuevas fuera del SDK.
3. Las pruebas en emulador quedan para el final, junto con las demás de mobile.

## Seguridad

La preferencia no es sensible y no se mezcla con los tokens.

## Contrato de datos / recurso HTTP

Cabecera `Accept-Language`.

## Orden de ejecución

| Código | Nombre | Depende de |
|--------|--------|------------|
| [UT-HUI-IDIOMAS-MOB-01](./UT-HUI-IDIOMAS-MOB-01.md) | Base | — |
| [UT-HUI-IDIOMAS-MOB-02](./UT-HUI-IDIOMAS-MOB-02.md) | Selector y comunes | 01 |
| [UT-HUI-IDIOMAS-MOB-03](./UT-HUI-IDIOMAS-MOB-03.md) | Autenticación y diagnóstico | 02 |
| [UT-HUI-IDIOMAS-MOB-04](./UT-HUI-IDIOMAS-MOB-04.md) | Control automático | 03 |

**Cadena:** 01 → 02 → 03 → 04

## Criterios de aceptación cubiertos

| CA | Descripción | UTs |
|----|-------------|-----|
| CA01 | Detección | 01 |
| CA02 | Selector | 02, 03 |
| CA03 | Preferencia | 01 |
| CA04 | Textos | 02, 03 |
| CA05 | Mensajes del servidor | 01, 03 |
| CA09 | Fechas y plurales | 01, 02 |
| CA10 | Idioma neutro | 03, 04 |
| CA11 | Comprobaciones | 04 |
| CA12 | Igual que la web | 01, 02, 03 |

## Follow-ups (no se implementan en estas UT)

- Validación en emulador o teléfono (al final del proyecto).

## Qué NO incluyen estas UT

- Traducir el nombre de la app en las tiendas.
