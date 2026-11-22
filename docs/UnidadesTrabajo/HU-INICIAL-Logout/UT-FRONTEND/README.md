# Unidades de trabajo — HU-INICIAL-Logout-FE

**Historia:** Acción de cerrar sesión en la web.
**Fuente HU:** [`HU-INICIAL-Logout.md`](../../../HistoriasDeUsuario/HU-INICIAL-Logout.md)
**Recurso HTTP / contrato:** Botón del `AppShell`.

## Antes de implementar cualquier UT

Leer las rules de [`frontend/`](../../../../.claude/rules/frontend/) y [`SEGURIDAD_OWASP.md`](../../../../.claude/rules/SEGURIDAD_OWASP.md). Estas fichas **no** sustituyen esas reglas: si hay conflicto, ganan las rules del proyecto.

## Orden de ejecución

| Código | Nombre | Depende de |
|--------|--------|------------|
| [UT-HUI-LOGOUT-FE-01](./UT-HUI-LOGOUT-FE-01.md) | Hook useLogout y limpieza de caché | — |
| [UT-HUI-LOGOUT-FE-02](./UT-HUI-LOGOUT-FE-02.md) | Acción cerrar sesión en el AppShell | UT-HUI-LOGOUT-FE-01 |
| [UT-HUI-LOGOUT-FE-03](./UT-HUI-LOGOUT-FE-03.md) | Tests de logout web | UT-HUI-LOGOUT-FE-02 |

**Cadena:** 01 → 02 → 03

## Criterios de aceptación cubiertos

| CA (HU-INICIAL-Logout) | Descripción | UTs |
|---|---|---|
| CA03 | Limpieza del cliente | 01, 02, 03 |
| CA04 | Cierre local aunque falle el servidor | 01, 03 |

## Follow-ups (no se implementan en estas UT)

- Ninguno por ahora.

## Qué NO incluyen estas UT

- Recuperación de contraseña.
