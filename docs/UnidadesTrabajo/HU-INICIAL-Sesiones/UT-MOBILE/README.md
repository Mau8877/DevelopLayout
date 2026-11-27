# Unidades de trabajo — HU-INICIAL-Sesiones-MOB

**Historia:** Pantalla «Mis sesiones» en la app.
**Fuente HU:** [`HU-INICIAL-Sesiones.md`](../../../HistoriasDeUsuario/HU-INICIAL-Sesiones.md)
**Recurso HTTP / contrato:** Ruta privada `/mis-sesiones`.

## Antes de implementar cualquier UT

Leer las rules de [`mobile/`](../../../../.claude/rules/mobile/) y [`SEGURIDAD_OWASP.md`](../../../../.claude/rules/SEGURIDAD_OWASP.md). Estas fichas **no** sustituyen esas reglas: si hay conflicto, ganan las rules del proyecto.

## Decisiones de diseño

- Acceso desde el `AppBar` de los dos shells.
- La prueba en emulador queda para el final de todo; no condiciona estas UT.

## Seguridad

- Sin persistencia ni logs de IP o user agent.
- El estado se descarta al salir y al cambiar de usuario.

## Orden de ejecución

| Código | Nombre | Depende de |
|--------|--------|------------|
| [UT-HUI-SESIONES-MOB-01](./UT-HUI-SESIONES-MOB-01.md) | Modelo y servicio | BE-02 |
| [UT-HUI-SESIONES-MOB-02](./UT-HUI-SESIONES-MOB-02.md) | Provider, pantalla y navegación | 01 |
| [UT-HUI-SESIONES-MOB-03](./UT-HUI-SESIONES-MOB-03.md) | Tests | 02 |

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

- Validación en emulador (al final).

## Qué NO incluyen estas UT

- Pantallas de administración en mobile.
