# Unidades de trabajo — HU-INICIAL-Usuarios-BE

**Historia:** API para listar, invitar, desactivar y reactivar usuarios, y para aceptar la invitación.
**Fuente HU:** [`HU-INICIAL-Usuarios.md`](../../../HistoriasDeUsuario/HU-INICIAL-Usuarios.md)
**Recurso HTTP / contrato:** `/api/v1/usuarios/**` y `POST /api/v1/auth/invitation/accept`

## Antes de implementar cualquier UT

Leer las rules de [`backend/`](../../../../.claude/rules/backend/) y [`SEGURIDAD_OWASP.md`](../../../../.claude/rules/SEGURIDAD_OWASP.md). Estas fichas **no** sustituyen esas reglas: si hay conflicto, ganan las rules del proyecto.

## Decisiones de diseño

Schema: authentication — existente — son datos de identidad y acceso.

1. **Invitación pendiente = columna `invitacion_pendiente`**, aparte del estado. La cuenta nace con el hash de una contraseña aleatoria que se descarta: no hay que permitir `password_hash` nulo ni tocar el login para cumplir el CA09.
2. **La invitación no reutiliza el endpoint de restablecimiento.** Tiene su ruta, su prefijo en Redis y sus mensajes; comparte la lógica de tokens y el servicio de correo.
3. **La base se ajusta más de lo que pide la función:** dejan de poder escribirse usuarios con el contexto del correo de login y roles con el contexto propio, y un administrador no puede cambiar correos ni contraseñas ajenas.
4. **Un solo bloqueo para administrar** (UT 04). Serializa estos cambios, que son poco frecuentes, y evita tener que contar administradores.
5. **La invitación vive en `features/auth`** (token, envío y aceptación); la administración en `features/usuarios`, que usa esos servicios y `SesionService`. `auth` no usa los servicios de administración, así que no hay ciclo.
6. **El envío es sincrónico** (a diferencia de `forgot`): aquí no hay nada que ocultar y el administrador necesita saber si salió.

## Seguridad

Solo `SUPER_ADMIN` (`@PreAuthorize`, guardia y RLS), salvo la aceptación, que es pública con token, CSRF y límite por IP. 3 reenvíos por usuario y hora; `pageSize` máximo 100. Sin RLS opt-out nuevo. Bitácora y logs sin correos, enlaces ni contraseñas.

## Contrato de datos / recurso HTTP

| Método y ruta | Cuerpo | Éxito | Errores propios |
|---|---|---|---|
| `POST /api/v1/usuarios/buscar` | `page`, `pageSize`, `search?`, `estado?`, `rol?` | 200, lista + `meta` | — |
| `POST /api/v1/usuarios` | `email`, `roles` | 201, `{usuario, invitacionEnviada}` | `ERR_US_02` 409, `ERR_US_03` 400 |
| `POST /api/v1/usuarios/{codigo}/reenviar-invitacion` | — | 200, `{invitacionEnviada}` | `ERR_US_01` 404, `ERR_US_05` y `ERR_US_06` 409 |
| `POST /api/v1/usuarios/{codigo}/desactivar` | — | 200, usuario | `ERR_US_01` 404, `ERR_US_04` 409 |
| `POST /api/v1/usuarios/{codigo}/reactivar` | — | 200, usuario | `ERR_US_01` 404 |
| `POST /api/v1/auth/invitation/accept` (público) | `token`, `password` | 200 | `ERR_AUTH_11` 400 |

`UsuarioResponse`: `codigo`, `email`, `roles`, `estado`, `invitacionPendiente`, `creadoEn`.

## Orden de ejecución

| Código | Nombre | Depende de |
|--------|--------|------------|
| [UT-HUI-USUARIOS-BE-01](./UT-HUI-USUARIOS-BE-01.md) | Base de datos y permisos | — |
| [UT-HUI-USUARIOS-BE-02](./UT-HUI-USUARIOS-BE-02.md) | Tokens de invitación | — |
| [UT-HUI-USUARIOS-BE-03](./UT-HUI-USUARIOS-BE-03.md) | Correo de invitación | — |
| [UT-HUI-USUARIOS-BE-04](./UT-HUI-USUARIOS-BE-04.md) | Guardia de administradores | 01 |
| [UT-HUI-USUARIOS-BE-05](./UT-HUI-USUARIOS-BE-05.md) | Listado y búsqueda | 01 |
| [UT-HUI-USUARIOS-BE-06](./UT-HUI-USUARIOS-BE-06.md) | Alta con invitación | 02, 03, 04, 05 |
| [UT-HUI-USUARIOS-BE-07](./UT-HUI-USUARIOS-BE-07.md) | Reenvío | 06 |
| [UT-HUI-USUARIOS-BE-08](./UT-HUI-USUARIOS-BE-08.md) | Aceptar invitación | 01, 02 |
| [UT-HUI-USUARIOS-BE-09](./UT-HUI-USUARIOS-BE-09.md) | Recuperación acepta la invitación | 08 |
| [UT-HUI-USUARIOS-BE-10](./UT-HUI-USUARIOS-BE-10.md) | Desactivar y reactivar | 02, 04, 05 |
| [UT-HUI-USUARIOS-BE-11](./UT-HUI-USUARIOS-BE-11.md) | Prueba de producción y documentación | 07, 09, 10 |

**Cadena:** 01, 02, 03 → 04, 05, 08 → 06, 09, 10 → 07 → 11

## Criterios de aceptación cubiertos

| CA | Descripción | UTs |
|----|-------------|-----|
| CA01 | Listado paginado | 05 |
| CA02 | Búsqueda y filtros | 05 |
| CA03 | Alta con invitación | 03, 06 |
| CA04 | Datos inválidos | 06 |
| CA05 | Correo repetido | 06 |
| CA06 | Correo que falla | 06, 07, 11 |
| CA07 | Aceptación | 08, 09 |
| CA08 | Enlace inservible | 02, 08 |
| CA09 | Login de cuenta pendiente | 06 |
| CA10 | Reenvío | 07 |
| CA11 | Reenvío no permitido | 07 |
| CA12 | Desactivar | 02, 10 |
| CA13 | Reactivar | 10 |
| CA14 | Operación repetida | 10 |
| CA15 | Propia cuenta | 10 |
| CA16 | Cruce entre administradores | 04, 10 |
| CA17 | Actor que ya no es administrador | 04, 06, 10 |
| CA18 | 401, 403 y 404 | 05, 06, 07, 10 |
| CA19 | Bitácora | 06, 07, 08, 09, 10 |
| CA21 | Respuestas sin datos sensibles | 05, 06, 10 |

## Follow-ups (no se implementan en estas UT)

- Resuelto fuera de estas UT: con un dueño de base **sin** superusuario, `AdminInicialInitializer` contaba cero administradores y el segundo arranque fallaba. Lo corrige la política `usuario_rol_lectura_alta_inicial` (`V20261127090000`), que la UT 01 debe conservar.
- Una sesión que pasó a `EXPIRADA` con un access token aún vigente (como máximo 15 minutos, solo justo al vencer) no entra en la revocación.
- Envío de correo con cola persistente y reintentos.

## Qué NO incluyen estas UT

- Cambiar roles (HU Roles).
- Cambiar el correo, eliminar usuarios, crear roles.
- Nada de mobile.
