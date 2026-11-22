# Unidades de trabajo — HU-INICIAL-Base-BE

**Historia:** Tablas de usuarios y roles, rol de runtime con RLS y administrador inicial.
**Fuente HU:** [`HU-INICIAL-Base.md`](../../../HistoriasDeUsuario/HU-INICIAL-Base.md)
**Recurso HTTP / contrato:** Sin endpoints; esquema de base de datos y arranque.

## Antes de implementar cualquier UT

Leer las rules de [`backend/`](../../../../.claude/rules/backend/) y [`SEGURIDAD_OWASP.md`](../../../../.claude/rules/SEGURIDAD_OWASP.md). Estas fichas **no** sustituyen esas reglas: si hay conflicto, ganan las rules del proyecto.

## Decisiones de diseño

- El administrador inicial se crea con la conexión dueña del esquema: en el arranque no hay usuario autenticado y RLS está forzado. Es la única escritura fuera de `app_runtime` y queda documentada como excepción.
- El correo se resuelve en las políticas de RLS con `app.login_email`, que fija el flujo de login/recuperación y expone una sola fila; así `usuarios` no necesita `RLS-OPT-OUT` aunque el login ocurra antes de conocer al usuario.

## Orden de ejecución

| Código | Nombre | Depende de |
|--------|--------|------------|
| [UT-HUI-BASE-BE-01](./UT-HUI-BASE-BE-01.md) | Migración de tablas de autenticación con RLS | — |
| [UT-HUI-BASE-BE-02](./UT-HUI-BASE-BE-02.md) | Rol de base de datos app_runtime y configuración del datasource | UT-HUI-BASE-BE-01 |
| [UT-HUI-BASE-BE-03](./UT-HUI-BASE-BE-03.md) | Models, enums y repositorios de usuarios y roles | UT-HUI-BASE-BE-01 |
| [UT-HUI-BASE-BE-04](./UT-HUI-BASE-BE-04.md) | Administrador inicial en el primer arranque | UT-HUI-BASE-BE-03 |
| [UT-HUI-BASE-BE-05](./UT-HUI-BASE-BE-05.md) | Test de integración de RLS como app_runtime | UT-HUI-BASE-BE-02, UT-HUI-BASE-BE-04 |

**Cadena:** 01 → 02 → 03 → 04 → 05

## Criterios de aceptación cubiertos

| CA (HU-INICIAL-Base) | Descripción | UTs |
|---|---|---|
| CA01 | Tablas y roles sembrados | 01, 03 |
| CA02 | Admin inicial creado | 04 |
| CA03 | Idempotencia | 04 |
| CA04 | Fallo de arranque sin variables | 04 |
| CA05 | Tablas con BaseModel | 01, 03 |
| CA06 | RLS sin contexto y con correo de login | 01, 05 |
| CA07 | Conexión con app_runtime | 02 |

## Follow-ups (no se implementan en estas UT)

- Alta, edición y baja de usuarios por el administrador (HU futura).
- Rotación de la contraseña del administrador inicial.

## Qué NO incluyen estas UT

- Login, refresh, logout y recuperación (HU siguientes).
- Interfaz web o mobile: esta HU solo toca backend e infraestructura.
