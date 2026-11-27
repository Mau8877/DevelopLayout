# Unidades de trabajo — HU-INICIAL-Sesiones-FE

**Historia:** Pantalla «Mis sesiones» para todos los roles.
**Fuente HU:** [`HU-INICIAL-Sesiones.md`](../../../HistoriasDeUsuario/HU-INICIAL-Sesiones.md)
**Recurso HTTP / contrato:** Ruta privada `/mis-sesiones`.

## Antes de implementar cualquier UT

Leer las rules de [`frontend/`](../../../../.claude/rules/frontend/) y [`SEGURIDAD_OWASP.md`](../../../../.claude/rules/SEGURIDAD_OWASP.md). Estas fichas **no** sustituyen esas reglas: si hay conflicto, ganan las rules del proyecto.

## Decisiones de diseño

- Una sola pantalla para todos los roles; el layout lo elige `LayoutPorRol`.
- Columnas compartidas con la administración.
- El resumen del dispositivo se arma en el cliente.

## Seguridad

- Ruta privada; el backend autoriza cada operación.
- El user agent se muestra como texto.
- Confirmación antes de cualquier cierre.

## Orden de ejecución

| Código | Nombre | Depende de |
|--------|--------|------------|
| [UT-HUI-SESIONES-FE-01](./UT-HUI-SESIONES-FE-01.md) | Tipos, hooks y resumen de dispositivo | BE-02 |
| [UT-HUI-SESIONES-FE-02](./UT-HUI-SESIONES-FE-02.md) | Pantalla, ruta y menú | 01 |
| [UT-HUI-SESIONES-FE-03](./UT-HUI-SESIONES-FE-03.md) | Tests | 02 |

**Cadena:** 01 → 02 → 03

## Criterios de aceptación cubiertos

| CA | Descripción | UTs |
|---|---|---|
| CA01 | Listado con la actual marcada | 01, 02, 03 |
| CA02 | Cerrar una | 01, 02, 03 |
| CA04 | Cerrar las demás | 01, 02, 03 |
| CA05 | La actual no se cierra desde acá | 02, 03 |
| CA11 | Confirmar y actualizar | 02, 03 |

## Follow-ups (no se implementan en estas UT)

- Ubicación aproximada a partir de la IP.

## Qué NO incluyen estas UT

- Cambios funcionales en la administración.
