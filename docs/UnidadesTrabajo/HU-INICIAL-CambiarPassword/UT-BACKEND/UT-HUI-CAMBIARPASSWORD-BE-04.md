# UT-HUI-CAMBIARPASSWORD-BE-04 — Servicio de cambio de contraseña

| Campo | Valor |
|-------|-------|
| **Código** | `UT-HUI-CAMBIARPASSWORD-BE-04` |
| **Nombre** | Servicio de cambio de contraseña |
| **Historia** | HU-INICIAL-CambiarPassword-BE |
| **Depende de** | 01, 02, 03 |

## Antes de implementar

SEGURIDAD_AUTH_BACKEND (conexiones, recuperación y Mis sesiones), BITACORA_BACKEND, LOGGING_BACKEND, EXCEPCIONES_BACKEND y GUIA_ERRORES_BACKEND.

## Solicitud de negocio

Cambiar la contraseña tiene que ser seguro aunque pasen otras cosas al mismo tiempo.

## Objetivo

`CambioPasswordService.cambiar(usuario, sesionActual, actual, nueva)` completo.

## Alcance

El método **no es `@Transactional`**: BCrypt y SMTP no corren con una conexión tomada y nunca se pide una segunda conexión. Pasos:

1. Transacción corta de lectura (`usarUsuario`): la cuenta sigue `ACTIVA` y `sesionActual` sigue activa; si no, 401 `ERR_AUTH_04`.
2. `intentos.reservar`; sin cupo, 423 `ERR_AUTH_13` y log `WARN`.
3. Comparar la contraseña actual. Si no coincide: bitácora `AUTH_PASSWORD_CAMBIO_FALLIDO` (`FALLO`, en su propia transacción), log `WARN` y 400 `ERR_AUTH_12`.
4. Si coincide, `intentos.reiniciar`. Si la nueva es igual a la actual, 400 `ERR_AUTH_06`.
5. Calcular el hash nuevo.
6. Transacción de escritura (`usarUsuario`):
   - `bloquearParaCambio` sobre la fila del usuario, exigiendo `ACTIVO` y **el mismo hash que se verificó**;
   - bloquear **todas** las sesiones `ACTIVA` y exigir que `sesionActual` esté entre ellas (mismo patrón que Mis sesiones). Si algo no se cumple, 401 `ERR_AUTH_04` sin cambiar nada;
   - `cambiarPassword`, `revocarTodas(CAMBIO_PASSWORD)`, bitácora `AUTH_PASSWORD_CAMBIADA`;
   - `flush` y, **antes de confirmar**, anular los enlaces de recuperación y de invitación. Si eso falla, no cambia la contraseña.
7. Ya confirmado: `loginAttempts.reiniciar(correo)` (un fallo se registra y no se propaga) y el aviso por correo en el ejecutor acotado existente, con el idioma **capturado en la petición**. Un fallo o la cola llena se registra sin el mensaje de la excepción.

- Códigos `ERR_AUTH_12` (`ValidationException`) y `ERR_AUTH_13` (`LockedException`), con sus claves de mensaje en ambos idiomas.
- Documentarlos y actualizar la sección de `ERR_AUTH_06`, que ahora también se lanza aquí.

**Archivos esperados (crear/tocar):**

- `projects/backend/src/main/java/com/example/backend/features/auth/service/CambioPasswordService.java`
- `projects/backend/src/main/java/com/example/backend/features/auth/AuthAcciones.java`
- `projects/backend/src/main/java/com/example/backend/features/auth/AuthErrorCodes.java`
- `projects/backend/src/main/resources/i18n/auth/messages.properties`
- `projects/backend/src/main/resources/i18n/auth/messages_en.properties`
- `docs/errors/auth/ERRORES_AUTH.md`
- `docs/errors/README.md`
- `projects/backend/src/test/java/com/example/backend/features/auth/CambioPasswordServiceTest.java`
- `projects/backend/src/test/java/com/example/backend/features/auth/CambioPasswordConcurrenciaTest.java`

## Seguridad

A01: la cuenta y la sesión salen del token; nunca se usa el contexto de administrador y la base impide cambiar la contraseña de otra persona. A07: se exige la actual y se revoca todo. A09: ni contraseñas ni hashes en logs o bitácora. A10: ante un fallo al revocar o anular enlaces, no cambia nada.

## Fuera de alcance (no tocar)

- El endpoint, el DTO y el límite por IP (BE-05).
- El flujo de recuperación (`RecuperacionService`).
- Cambiar la política de contraseñas.

## Resultado esperado

El cambio funciona de punta a punta dentro del backend.

## Validación

1. Con Postgres, Redis y `app_runtime`: cambia el hash, todas las sesiones (web y mobile, incluida la actual) quedan `REVOCADA` con `CAMBIO_PASSWORD`, su `sid` queda en la lista de revocadas y la contraseña anterior ya no entra.
2. Actual incorrecta e igual a la actual: no cambia nada y las sesiones siguen activas.
3. Tras 5 fallos, la contraseña correcta recibe 423 y el login sigue funcionando; un cambio correcto levanta el bloqueo de login.
4. Un enlace de recuperación pedido antes deja de funcionar.
5. Sesión revocada antes de confirmar: 401 y no cambia la contraseña.
6. Dos cambios simultáneos: solo uno tiene éxito. Un login simultáneo con la contraseña anterior no deja una sesión activa.
7. Con un fallo de Redis al anular enlaces o revocar, la contraseña no cambia.
8. Con el correo caído, el cambio se mantiene. El aviso sale en el idioma de la petición.
9. La bitácora tiene el éxito y el fallo, sin contraseñas. Con un pool de 3 y peticiones simultáneas no se agotan las conexiones.
10. Acceso denegado: un usuario no puede cambiar la contraseña de otro.
