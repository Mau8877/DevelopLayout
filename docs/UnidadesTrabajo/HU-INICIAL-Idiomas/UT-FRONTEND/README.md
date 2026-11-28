# Unidades de trabajo — HU-INICIAL-Idiomas-FE

**Historia:** La web completa en español o inglés, con selector.
**Fuente HU:** [`HU-INICIAL-Idiomas.md`](../../../HistoriasDeUsuario/HU-INICIAL-Idiomas.md)
**Recurso HTTP / contrato:** Toda la web

## Antes de implementar cualquier UT

Leer las rules de [`frontend/`](../../../../.claude/rules/frontend/) y [`SEGURIDAD_OWASP.md`](../../../../.claude/rules/SEGURIDAD_OWASP.md). Estas fichas **no** sustituyen esas reglas: si hay conflicto, ganan las rules del proyecto.

## Decisiones de diseño

1. **i18next + react-i18next**: maduro, con claves tipadas y plurales.
2. **Textos por feature** en `language/`; los comunes en `shared/language/`. Se descubren con `import.meta.glob`, sin romper la regla de dependencia.
3. **El servidor traduce sus mensajes**; la web solo envía el idioma.

## Seguridad

La preferencia no es sensible. Sin `dangerouslySetInnerHTML` con traducciones.

## Contrato de datos / recurso HTTP

Cabecera `Accept-Language` en cada petición.

## Orden de ejecución

| Código | Nombre | Depende de |
|--------|--------|------------|
| [UT-HUI-IDIOMAS-FE-01](./UT-HUI-IDIOMAS-FE-01.md) | Base | — |
| [UT-HUI-IDIOMAS-FE-02](./UT-HUI-IDIOMAS-FE-02.md) | Selector | 01 |
| [UT-HUI-IDIOMAS-FE-03](./UT-HUI-IDIOMAS-FE-03.md) | Textos comunes | 01 |
| [UT-HUI-IDIOMAS-FE-04](./UT-HUI-IDIOMAS-FE-04.md) | Autenticación | 03 |
| [UT-HUI-IDIOMAS-FE-05](./UT-HUI-IDIOMAS-FE-05.md) | Resto de la web | 03, 04 |
| [UT-HUI-IDIOMAS-FE-06](./UT-HUI-IDIOMAS-FE-06.md) | Control automático | 02, 04, 05 |

**Cadena:** 01 → 02, 03 → 04 → 05 → 06

## Criterios de aceptación cubiertos

| CA | Descripción | UTs |
|----|-------------|-----|
| CA01 | Detección | 01 |
| CA02 | Selector | 02, 04 |
| CA03 | Preferencia | 01, 02 |
| CA04 | Textos | 03, 04, 05 |
| CA05 | Mensajes del servidor | 01, 04 |
| CA09 | Fechas y plurales | 03, 05 |
| CA10 | Idioma neutro | 03, 04, 05, 06 |
| CA11 | Comprobaciones | 01, 06 |

## Follow-ups (no se implementan en estas UT)

- Cargar cada idioma por separado para reducir el tamaño.

## Qué NO incluyen estas UT

- Mobile.
- Rutas traducidas en la URL.
