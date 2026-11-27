# Errores — Usuarios (`US`)

## `ERR_US_01`

- **HTTP:** 404
- **Excepción:** `NotFoundException`
- **Significado:** el código no corresponde a ninguna cuenta.
- **Dónde se lanza:** `UsuarioAdminService.reenviarInvitacion()`, `desactivar()`, `reactivar()` y `cambiarRoles()`

## `ERR_US_02`

- **HTTP:** 409
- **Excepción:** `ConflictException`
- **Significado:** el correo ya pertenece a una cuenta, activa o inactiva, sin distinguir mayúsculas.
- **Dónde se lanza:** `UsuarioAdminService.crear()`

## `ERR_US_03`

- **HTTP:** 400
- **Excepción:** `ValidationException`
- **Significado:** se pidió un rol que no existe o está desactivado.
- **Dónde se lanza:** `UsuarioAdminService.crear()` y `cambiarRoles()`

## `ERR_US_04`

- **HTTP:** 409
- **Excepción:** `ConflictException`
- **Significado:** un administrador intentó desactivar su propia cuenta.
- **Dónde se lanza:** `UsuarioAdminService.desactivar()`

## `ERR_US_05`

- **HTTP:** 409
- **Excepción:** `ConflictException`
- **Significado:** no corresponde reenviar la invitación: la cuenta está inactiva o ya la aceptó.
- **Dónde se lanza:** `UsuarioAdminService.reenviarInvitacion()`

## `ERR_US_06`

- **HTTP:** 409
- **Excepción:** `ConflictException`
- **Significado:** se superó el límite de reenvíos de una cuenta en una hora.
- **Dónde se lanza:** `UsuarioAdminService.reenviarInvitacion()`

## `ERR_US_07`

- **HTTP:** 409
- **Excepción:** `ConflictException`
- **Significado:** un administrador intentó quitarse su propio rol `SUPER_ADMIN`.
- **Dónde se lanza:** `UsuarioAdminService.cambiarRoles()`
