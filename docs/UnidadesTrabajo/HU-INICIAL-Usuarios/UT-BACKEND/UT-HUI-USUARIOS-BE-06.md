# UT-HUI-USUARIOS-BE-06 — Alta de usuario con invitación

| Campo | Valor |
|-------|-------|
| **Código** | `UT-HUI-USUARIOS-BE-06` |
| **Nombre** | Alta de usuario con invitación |
| **Historia** | HU-INICIAL-Usuarios-BE |
| **Depende de** | 02, 03, 04, 05 |

## Antes de implementar

EXCEPCIONES_BACKEND, GUIA_ERRORES_BACKEND, BITACORA_BACKEND, LOGGING_BACKEND y SEGURIDAD_AUTH_BACKEND (conexiones).

## Solicitud de negocio

El administrador da de alta a una persona sin conocer su contraseña.

## Objetivo

`POST /api/v1/usuarios` crea la cuenta pendiente y envía la invitación.

## Alcance

- `CrearUsuarioRequest` (`ignoreUnknown = false`): `email` y `roles` (al menos uno). El correo se normaliza (sin espacios, minúsculas).
- Antes de la transacción, se calcula el BCrypt de 32 bytes aleatorios que **se descartan**: la cuenta pendiente no tiene ninguna contraseña utilizable y el login falla igual que con credenciales incorrectas, sin tocar `AuthService`.
- En una transacción: guardia (UT 04), correo no usado, roles existentes y activos, cuenta `ACTIVO` con `invitacionPendiente`, asignaciones y bitácora `USUARIOS_USUARIO_CREADO` (detalle: nombres de roles). El índice único es el respaldo: su violación también es 409.
- **Después de confirmar**, sin conexión tomada: `InvitacionEnvio.enviar(usuario, correo)` emite el token y envía. Cualquier fallo (Redis o SMTP) devuelve `false`, se registra en el log sin el mensaje de la excepción y **no** deshace la cuenta.
- Respuesta **201** con `data`: `{ usuario, invitacionEnviada }`. El `message` dice si el correo no se pudo enviar.
- Errores: `ERR_US_02` (409, correo usado), `ERR_US_03` (400, rol inexistente); formato inválido o sin roles, 400 indicando el campo. Documentarlos en `docs/errors/usuarios/`.
- El envío es sincrónico para poder informar el resultado; con un SMTP caído la respuesta tarda lo que marcan sus tiempos límite.
- Actualizar `docs/api/openapi.json` en esta misma UT (`OpenApiContractTest`). Los nombres de los métodos de controller son únicos entre todos los controllers; si no, springdoc renombra operaciones que ya existían.

**Archivos esperados (crear/tocar):**

- `projects/backend/src/main/java/com/example/backend/features/usuarios/controller/UsuarioController.java`
- `projects/backend/src/main/java/com/example/backend/features/usuarios/service/UsuarioAdminService.java`
- `projects/backend/src/main/java/com/example/backend/features/usuarios/dto/CrearUsuarioRequest.java`
- `projects/backend/src/main/java/com/example/backend/features/usuarios/dto/UsuarioCreadoResponse.java`
- `projects/backend/src/main/java/com/example/backend/features/usuarios/UsuarioErrorCodes.java`
- `projects/backend/src/main/java/com/example/backend/features/usuarios/UsuarioAcciones.java`
- `projects/backend/src/main/java/com/example/backend/features/usuarios/model/Usuario.java`
- `projects/backend/src/main/java/com/example/backend/features/usuarios/model/UsuarioRol.java`
- `projects/backend/src/main/java/com/example/backend/features/auth/service/InvitacionEnvio.java`
- `projects/backend/src/test/java/com/example/backend/features/usuarios/CrearUsuarioControllerTest.java`
- `docs/errors/usuarios/ERRORES_USUARIOS.md`
- `docs/errors/README.md`
- `docs/api/openapi.json`

## Seguridad

API3: no se aceptan estado, contraseña ni código. A07: contraseña inutilizable. A09: bitácora sin correos ni enlaces; los logs usan `codigo`. Ni BCrypt ni SMTP corren con una conexión tomada.

## Fuera de alcance (no tocar)

- Reenvío (UT 07) y aceptación (UT 08).
- Cambiar roles de una cuenta existente (HU Roles).

## Resultado esperado

Un alta válida deja la cuenta pendiente y el correo enviado, o avisa que no salió.

## Validación

1. Alta válida: 201, correo con enlace, `invitacionPendiente`, roles y evento sin datos sensibles.
2. Correo inválido, sin roles, rol inexistente o campos de más: 400 y nada creado.
3. Correo ya usado (otras mayúsculas, cuenta inactiva): 409. Dos altas simultáneas: una 201 y otra 409.
4. Con el SMTP caído: 201, `invitacionEnviada=false` y la cuenta existe.
5. La cuenta pendiente no puede iniciar sesión y recibe el mismo error que unas credenciales incorrectas.
6. Acceso denegado: 401, 403 por rol, 403 sin CSRF y 401 con la sesión del administrador revocada.
