---
globs: projects/backend/**/*
---

# Bitácora (auditoría) — Backend

Registro de solo inserción de eventos de seguridad y de cambios de negocio.
Complementa a [LOGGING_BACKEND.md](LOGGING_BACKEND.md) (los logs son para
operar el sistema y rotan; la bitácora es para auditar y se consulta desde la
aplicación) y a [SEGURIDAD_OWASP.md](../SEGURIDAD_OWASP.md) (A09).

## Tabla y servicio

- Tabla `bitacora` (Model extiende `BaseModel`): `codigo`, `usuario_codigo`
  (actor, `NULL` si no hay sesión), `accion`, `modulo`, `entidad`,
  `entidad_codigo`, `resultado` (`EXITO`/`FALLO`), `ip`, `user_agent`
  (truncado), `detalle` (`jsonb`), `created_at`.
- **Un solo punto de escritura:** `common/bitacora/BitacoraService.registrar(...)`.
  Ningún módulo inserta en `bitacora` por su cuenta ni crea su propio repositorio.
- IP y user agent se toman de la request actual cuando existe.
- `app_runtime` solo tiene `INSERT` (y `SELECT` bajo RLS para `SUPER_ADMIN`). Sin
  `UPDATE` ni `DELETE`. La purga usa una función de base de datos con
  privilegios del dueño.

## Cuándo registrar

- Todo evento de seguridad: login exitoso/fallido, bloqueo, logout,
  reutilización de refresh token, recuperación de contraseña, cierre de sesiones,
  cambios de rol.
- Todo cambio de negocio **que crea, edita o elimina** una entidad
  (`{MODULO}_{ENTIDAD}_{CREADA|EDITADA|ELIMINADA}`), desde el `service`, en la
  misma transacción que el cambio (si el cambio falla, no queda evento de éxito).
- No se registran lecturas (`GET`) ni cada renovación normal de sesión.

## Nombre de la acción

`{MODULO}_{ACCION}` en mayúsculas con guion bajo (`AUTH_LOGIN_OK`,
`PEDIDOS_PEDIDO_APROBADO`). Se definen como constantes del módulo, junto a sus
`ErrorCodes`.

## Qué NO va en `detalle`

Contraseñas, tokens (access o refresh), hashes de tokens, datos de pago,
documentos de identidad, ni el correo en claro de un intento fallido (se usa un
hash corto). El `detalle` lleva solo identificadores (`codigo`) y los campos
que cambiaron con sus nombres, nunca valores sensibles.

## Consulta y retención

- Solo `SUPER_ADMIN` consulta (`@PreAuthorize` en el service), paginada, con
  `pageSize` máximo 100.
- Retención configurable (`app.bitacora.retention-months`, 12 por defecto).
  **Pendiente:** existe la función `authentication.purgar_bitacora(meses)`, pero
  todavía no hay propiedad ni tarea programada que la invoque. La consulta
  llega con la HU de Bitácora.

## Tests

Todo flujo que registre un evento verifica que el evento existe, que su
resultado es el esperado y que `detalle` no contiene secretos.
