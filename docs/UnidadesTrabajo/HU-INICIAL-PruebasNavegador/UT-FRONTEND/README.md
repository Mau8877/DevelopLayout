# Unidades de trabajo — HU-INICIAL-PruebasNavegador-FE

**Historia:** Pruebas de los flujos clave en un navegador real.
**Fuente HU:** [`HU-INICIAL-PruebasNavegador.md`](../../../HistoriasDeUsuario/HU-INICIAL-PruebasNavegador.md)
**Recurso HTTP / contrato:** `pnpm test:e2e`

## Antes de implementar cualquier UT

Leer las rules de [`frontend/`](../../../../.claude/rules/frontend/) y [`SEGURIDAD_OWASP.md`](../../../../.claude/rules/SEGURIDAD_OWASP.md). Estas fichas **no** sustituyen esas reglas: si hay conflicto, ganan las rules del proyecto.

## Decisiones de diseño

1. **Playwright, solo Chromium.**
2. **Entorno propio y desechable**, separado del de desarrollo y del de la prueba de producción.
3. Web en modo desarrollo y backend real; la imagen de producción ya la comprueba `test:prod`.

## Seguridad

Todo en `127.0.0.1`, secretos generados al ejecutar y protecciones para no borrar nada ajeno.

## Contrato de datos / recurso HTTP

No cambia ningún contrato.

## Orden de ejecución

| Código | Nombre | Depende de |
|--------|--------|------------|
| [UT-HUI-PRUEBASNAVEGADOR-FE-01](./UT-HUI-PRUEBASNAVEGADOR-FE-01.md) | Entorno y Playwright | — |
| [UT-HUI-PRUEBASNAVEGADOR-FE-02](./UT-HUI-PRUEBASNAVEGADOR-FE-02.md) | Sesiones | 01 |
| [UT-HUI-PRUEBASNAVEGADOR-FE-03](./UT-HUI-PRUEBASNAVEGADOR-FE-03.md) | Cuentas y administración | 01 |
| [UT-HUI-PRUEBASNAVEGADOR-FE-04](./UT-HUI-PRUEBASNAVEGADOR-FE-04.md) | Idiomas | 01 |
| [UT-HUI-PRUEBASNAVEGADOR-FE-05](./UT-HUI-PRUEBASNAVEGADOR-FE-05.md) | CI y estabilidad | 02, 03, 04 |

**Cadena:** 01 → 02, 03, 04 → 05

## Criterios de aceptación cubiertos

| CA | Descripción | UTs |
|----|-------------|-----|
| CA01 | Un comando | 01 |
| CA02 | No toca nada ajeno | 01 |
| CA03 | Sesión | 02 |
| CA04 | Correos | 03 |
| CA05 | Administración | 03 |
| CA06 | Permisos | 03 |
| CA07 | Idiomas | 04 |
| CA08 | CI | 05 |
| CA09 | Estabilidad | 05 |
| CA10 | Proyecto nuevo | 05 |

## Follow-ups (no se implementan en estas UT)

- Pruebas de carga.
- Pruebas visuales y de accesibilidad automáticas.

## Qué NO incluyen estas UT

- Mobile.
- Pruebas contra un servidor desplegado.
