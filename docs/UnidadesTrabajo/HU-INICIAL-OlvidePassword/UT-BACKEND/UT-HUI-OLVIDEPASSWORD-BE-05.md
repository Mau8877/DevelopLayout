# UT-HUI-OLVIDEPASSWORD-BE-05 — Restablecer contraseña: POST /auth/password/reset

| Campo | Valor |
|-------|-------|
| **Código** | `UT-HUI-OLVIDEPASSWORD-BE-05` |
| **Nombre** | Restablecer contraseña: POST /auth/password/reset |
| **Historia** | HU-INICIAL-OlvidePassword-BE |
| **Depende de** | UT-HUI-OLVIDEPASSWORD-BE-03, UT-HUI-OLVIDEPASSWORD-BE-04 |

## Antes de implementar

Leer `backend/SEGURIDAD_AUTH_BACKEND.md`, `backend/BASE_DE_DATOS_PRIVADA_RLS.md`, `backend/GUIA_ERRORES_BACKEND.md` y `.claude/rules/SEGURIDAD_OWASP.md`.

## Solicitud de negocio

El usuario define una contraseña nueva y nadie que tuviera la anterior conserva el acceso.

## Objetivo

Existe `POST /api/v1/auth/password/reset`, que cambia la contraseña, cierra todas las sesiones y levanta el bloqueo.

## Alcance

### `RecuperacionService.restablecer(token, nuevaPassword)` — sin `@Transactional`, en este orden

1. `consultar(token)`; si no existe, 400 `ERR_AUTH_05`.
2. Transacción corta de lectura (`usarUsuario`): la cuenta debe estar activa (si no, `ERR_AUTH_05`); tomar el hash actual.
3. **Sin conexión tomada:** si la nueva coincide con la actual, 400 `ERR_AUTH_06` (el enlace sigue valiendo); calcular el hash nuevo.
4. Transacción de escritura: bloquear la fila del usuario (`PESSIMISTIC_WRITE`); si el hash ya no es el leído o la cuenta dejó de estar activa, `ERR_AUTH_05`; cambiar la contraseña; `SesionService.revocarTodas(codigo, RESET_PASSWORD)`; bitácora `AUTH_PASSWORD_RESTABLECIDA`; `flush`; y **por último** `consumir(token, codigo)`: si da `false`, lanzar `ERR_AUTH_05` y deshacer todo.
5. Confirmada la transacción: `LoginAttemptService.reiniciar(email)`. Si falla, se registra `ERROR` y se responde éxito (la contraseña ya cambió).

Qué pasa si algo falla: antes de consumir, el enlace sigue valiendo; si falla la confirmación después de consumir, la contraseña **no** cambia y hay que pedir otro enlace. Si se deshace después de revocar, las sesiones pueden quedar anotadas como revocadas en Redis: el efecto es denegar acceso (falla cerrado).

### El login no debe sobrevivir al cambio (código existente)

`AuthService.abrirSesion` vuelve a leer el usuario, pero **no comprueba que la contraseña siga siendo la verificada**: un login que comparó la anterior puede crear una sesión después de la revocación. Cambiarlo para leer con bloqueo compartido (`PESSIMISTIC_READ`) y exigir que el hash sea el mismo que se comparó; si no, `ERR_AUTH_01`. Con el bloqueo exclusivo del paso 4, o el login confirma antes (y su sesión se revoca) o espera y ve el hash nuevo.

### Resto

- `SesionService.revocarTodas(UUID, MotivoRevocacion)`: reutiliza `SesionRepository.bloquearPorUsuarioYEstado` (ya existe, con orden fijo) y `revocar`. No cambia `SesionAdminService`.
- `LoginAttemptService.reiniciar(email)` no existe y las claves por origen llevan el hash de la IP. El script de reserva anota cada clave de origen en un conjunto por cuenta (`...:{cuenta}:origenes`, mismo TTL); `reiniciar` borra, en un script, esas claves, el conjunto y el contador de la cuenta. No usar `SCAN` ni `KEYS`.
- `Usuario`: método `cambiarPassword(String hash)` (sin setters genéricos). `UsuarioRepository`: las dos lecturas con bloqueo.
- `ResetPasswordRequest`: `token` (`@NotBlank @Size(max=100)`), `password` (`@Size(min=8)` y **máximo 72 bytes** en UTF-8, validado en el DTO → 400: BCrypt rechaza lo que pase y daría 500).
- `RateLimitFilter`: bucket propio por IP (`reset-capacity=10`, `reset-refill-per-minute=5`), subido en `application-test.properties`. `SecurityConfig`: `permitAll` solo para este `POST`.
- `ERR_AUTH_05` (enlace inválido, vencido o usado) y `ERR_AUTH_06` (igual a la actual), con `ValidationException`, documentados en `docs/errors/`.
- Al terminar los dos endpoints: `pnpm openapi:sync` (si no, `OpenApiContractTest` falla).

**Archivos esperados (crear/tocar):**

- `projects/backend/src/main/java/com/example/backend/features/auth/service/RecuperacionService.java`
- `projects/backend/src/main/java/com/example/backend/features/auth/service/AuthService.java`
- `projects/backend/src/main/java/com/example/backend/features/auth/service/SesionService.java`
- `projects/backend/src/main/java/com/example/backend/features/auth/service/LoginAttemptService.java`
- `projects/backend/src/main/java/com/example/backend/features/auth/controller/AuthController.java`
- `projects/backend/src/main/java/com/example/backend/features/auth/dto/ResetPasswordRequest.java`
- `projects/backend/src/main/java/com/example/backend/features/auth/AuthErrorCodes.java`
- `projects/backend/src/main/java/com/example/backend/features/auth/AuthAcciones.java`
- `projects/backend/src/main/java/com/example/backend/features/usuarios/model/Usuario.java`
- `projects/backend/src/main/java/com/example/backend/features/usuarios/repository/UsuarioRepository.java`
- `projects/backend/src/main/java/com/example/backend/config/SecurityConfig.java`
- `projects/backend/src/main/java/com/example/backend/security/RateLimitFilter.java`
- `projects/backend/src/test/java/com/example/backend/security/RateLimitFilterTest.java`
- `projects/backend/src/main/resources/application.properties`
- `projects/backend/src/test/resources/application-test.properties`
- `docs/errors/auth/ERRORES_AUTH.md`
- `docs/errors/README.md`
- `docs/api/openapi.json`

## Seguridad

A01: RLS limita el cambio a la cuenta del token. A07: un solo uso, sesiones revocadas, sin ventana para un login con la contraseña anterior. A09: nada sensible en logs ni bitácora. Sin BCrypt con una conexión tomada y sin pedir una segunda. No hace falta migración (`RESET_PASSWORD` ya está en el `CHECK`).

## Fuera de alcance (no tocar)

- Enviar el correo (UT 04).
- Cambio de contraseña con sesión iniciada.
- Gestión de usuarios.

## Resultado esperado

Con un enlace válido se cambia la contraseña, las sesiones anteriores dejan de funcionar y la cuenta queda desbloqueada.

## Validación

1. Con Mailpit y `curl`: pedir, restablecer, login con la nueva (200) y con la anterior (401); refresh anterior (401).
2. Reutilizar el enlace: 400 `ERR_AUTH_05`. Contraseña de 7 caracteres o de más de 72 bytes: 400. Igual a la actual: 400 `ERR_AUTH_06`, y después el mismo enlace funciona.
3. Cuenta bloqueada desde dos IP: tras restablecer puede entrar desde ambas.
4. `./mvnw test` en verde.
