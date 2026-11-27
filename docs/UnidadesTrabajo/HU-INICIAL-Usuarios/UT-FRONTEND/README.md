# Unidades de trabajo — HU-INICIAL-Usuarios-FE

**Historia:** Pantalla de administración de usuarios y pantalla pública para aceptar la invitación.
**Fuente HU:** [`HU-INICIAL-Usuarios.md`](../../../HistoriasDeUsuario/HU-INICIAL-Usuarios.md)
**Recurso HTTP / contrato:** Rutas `/usuarios` y `/accept-invitation`

## Antes de implementar cualquier UT

Leer las rules de [`frontend/`](../../../../.claude/rules/frontend/) y [`SEGURIDAD_OWASP.md`](../../../../.claude/rules/SEGURIDAD_OWASP.md). Estas fichas **no** sustituyen esas reglas: si hay conflicto, ganan las rules del proyecto.

## Decisiones de diseño

1. La administración es una feature nueva (`usuarios`); aceptar la invitación pertenece a `auth`, junto al restablecimiento, cuyo formulario y lectura del token reutiliza.
2. Desactivar usa una confirmación nueva y no `ModalDestructive`, porque se puede deshacer.
3. La cabecera de nginx va en la UT que crea la ruta que protege.

## Seguridad

La ruta de usuarios está bajo el guard de administrador y la de invitación es pública con token en el fragmento. El correo buscado no va en la URL, la `queryKey` ni los logs. La autorización real es del backend.

## Contrato de datos / recurso HTTP

Los endpoints están en el [README de backend](../UT-BACKEND/README.md#contrato-de-datos--recurso-http). Los tipos salen del contrato (`pnpm openapi:types`).

## Orden de ejecución

| Código | Nombre | Depende de |
|--------|--------|------------|
| [UT-HUI-USUARIOS-FE-01](./UT-HUI-USUARIOS-FE-01.md) | Tipos y llamadas | — |
| [UT-HUI-USUARIOS-FE-02](./UT-HUI-USUARIOS-FE-02.md) | Componentes genéricos | — |
| [UT-HUI-USUARIOS-FE-03](./UT-HUI-USUARIOS-FE-03.md) | Pantalla de usuarios | 01 |
| [UT-HUI-USUARIOS-FE-04](./UT-HUI-USUARIOS-FE-04.md) | Crear usuario | 01, 02, 03 |
| [UT-HUI-USUARIOS-FE-05](./UT-HUI-USUARIOS-FE-05.md) | Acciones por fila | 01, 02, 03 |
| [UT-HUI-USUARIOS-FE-06](./UT-HUI-USUARIOS-FE-06.md) | Aceptar invitación | — |

**Cadena:** 01, 02, 06 → 03 → 04, 05

## Criterios de aceptación cubiertos

| CA | Descripción | UTs |
|----|-------------|-----|
| CA01 | Listado | 03 |
| CA02 | Búsqueda y filtros | 01, 03 |
| CA03 | Alta | 04 |
| CA04 | Datos inválidos | 04 |
| CA05 | Correo repetido | 04 |
| CA06 | Correo que falla | 04, 05 |
| CA07 | Aceptación | 06 |
| CA08 | Enlace inservible | 06 |
| CA10 | Reenvío | 05 |
| CA11 | Reenvío no permitido | 05 |
| CA12 | Desactivar | 05 |
| CA13 | Reactivar | 05 |
| CA18 | Acceso denegado | 01, 03 |
| CA20 | Confirmación y refresco | 02, 05 |

## Follow-ups (no se implementan en estas UT)

- Ordenar la tabla por columnas.

## Qué NO incluyen estas UT

- Cambiar roles (HU Roles).
- Pantallas mobile.
