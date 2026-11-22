# Unidades de trabajo — HU-INICIAL-Logout-MOB

**Historia:** Acción de cerrar sesión en la app.
**Fuente HU:** [`HU-INICIAL-Logout.md`](../../../HistoriasDeUsuario/HU-INICIAL-Logout.md)
**Recurso HTTP / contrato:** Botón en los shells.

## Antes de implementar cualquier UT

Leer las rules de [`mobile/`](../../../../.claude/rules/mobile/) y [`SEGURIDAD_OWASP.md`](../../../../.claude/rules/SEGURIDAD_OWASP.md). Estas fichas **no** sustituyen esas reglas: si hay conflicto, ganan las rules del proyecto.

## Orden de ejecución

| Código | Nombre | Depende de |
|--------|--------|------------|
| [UT-HUI-LOGOUT-MOB-01](./UT-HUI-LOGOUT-MOB-01.md) | logout en AuthService y authProvider | — |
| [UT-HUI-LOGOUT-MOB-02](./UT-HUI-LOGOUT-MOB-02.md) | Acción cerrar sesión en los shells | UT-HUI-LOGOUT-MOB-01 |
| [UT-HUI-LOGOUT-MOB-03](./UT-HUI-LOGOUT-MOB-03.md) | Tests de logout mobile | UT-HUI-LOGOUT-MOB-02 |

**Cadena:** 01 → 02 → 03

## Criterios de aceptación cubiertos

| CA (HU-INICIAL-Logout) | Descripción | UTs |
|---|---|---|
| CA03 | Borrado de almacenamiento seguro | 01, 02, 03 |
| CA04 | Cierre local aunque falle el servidor | 01, 03 |

## Follow-ups (no se implementan en estas UT)

- Ninguno por ahora.

## Qué NO incluyen estas UT

- Recuperación de contraseña.
