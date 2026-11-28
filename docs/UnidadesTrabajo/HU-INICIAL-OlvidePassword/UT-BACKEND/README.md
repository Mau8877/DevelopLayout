# Unidades de trabajo — HU-INICIAL-OlvidePassword-BE

**Historia:** Recuperar la contraseña con un enlace por correo de un solo uso.
**Fuente HU:** [`HU-INICIAL-OlvidePassword.md`](../../../HistoriasDeUsuario/HU-INICIAL-OlvidePassword.md)
**Recurso HTTP / contrato:** `POST /api/v1/auth/password/forgot`, `POST /api/v1/auth/password/reset`.

## Antes de implementar cualquier UT

Leer las rules de [`backend/`](../../../../.claude/rules/backend/) y [`SEGURIDAD_OWASP.md`](../../../../.claude/rules/SEGURIDAD_OWASP.md). Estas fichas **no** sustituyen esas reglas: si hay conflicto, ganan las rules del proyecto.

## Decisiones de diseño

Sin tablas ni migraciones nuevas (los tokens y límites viven en Redis).

- **El token viaja en el fragmento** del enlace (`#token=`), que el navegador no envía al servidor ni en `Referer`; después va por body.
- **Responder antes de mirar la cuenta.** Igualar tiempos con un hash ficticio no compensa un envío SMTP, y un fallo de envío solo en cuentas reales las delataría. La petición solo cuenta el límite; el resto va a un ejecutor acotado.
- **Sin cola persistente:** un envío se puede perder si el backend se reinicia. Se acepta: el usuario pide otro.
- **Límite por correo silencioso** (200 sin envío); el 429 es solo por IP. Evita una excepción 429 nueva en la jerarquía y que un tercero muestre un error a la víctima, aunque puede gastarle el cupo de una hora.
- **El token se consume al final**, dentro de la transacción de cambio, para no gastarlo si la contraseña elegida no sirve.
- **Login y restablecimiento se coordinan** bloqueando la fila del usuario y comparando el hash.
- **Correo obligatorio en producción**, pero fuera de `/health`.
- Las UT 02 y 07 van en el mismo PR.

## Seguridad

- **Acceso:** los dos endpoints son públicos (`permitAll` solo para esos `POST`); en web exigen CSRF. Cambiar la contraseña exige el token vigente.
- **Datos:** sin tablas nuevas. RLS: lectura por correo con `app.login_email`; cambio con `app.current_user`.
- **Consumo:** límite por IP en cada endpoint, 3 solicitudes por correo y hora, cola de envío acotada, timeouts SMTP.
- **Registro:** bitácora de la solicitud y del cambio; nunca token, contraseña ni correo en claro.
- **Conexiones:** ni BCrypt ni SMTP con una conexión tomada.

## Contrato de datos / recurso HTTP

| Método y ruta | Body | Éxito | Errores |
|---|---|---|---|
| `POST /api/v1/auth/password/forgot` | `email` | 200, mensaje genérico, `data: null` | 400 validación, 403 CSRF, 429 por IP |
| `POST /api/v1/auth/password/reset` | `token`, `password` | 200, `data: null` | 400 validación, `ERR_AUTH_05`, `ERR_AUTH_06`; 403 CSRF; 429 por IP |

## Orden de ejecución

| Código | Nombre | Depende de |
|--------|--------|------------|
| [UT-HUI-OLVIDEPASSWORD-BE-01](./UT-HUI-OLVIDEPASSWORD-BE-01.md) | Mailpit en el compose de desarrollo | — |
| [UT-HUI-OLVIDEPASSWORD-BE-02](./UT-HUI-OLVIDEPASSWORD-BE-02.md) | Envío del correo de recuperación | 01 |
| [UT-HUI-OLVIDEPASSWORD-BE-03](./UT-HUI-OLVIDEPASSWORD-BE-03.md) | PasswordResetTokenService | — |
| [UT-HUI-OLVIDEPASSWORD-BE-04](./UT-HUI-OLVIDEPASSWORD-BE-04.md) | Solicitar recuperación | 02, 03 |
| [UT-HUI-OLVIDEPASSWORD-BE-05](./UT-HUI-OLVIDEPASSWORD-BE-05.md) | Restablecer contraseña | 03, 04 |
| [UT-HUI-OLVIDEPASSWORD-BE-06](./UT-HUI-OLVIDEPASSWORD-BE-06.md) | Tests de recuperación | 05 |
| [UT-HUI-OLVIDEPASSWORD-BE-07](./UT-HUI-OLVIDEPASSWORD-BE-07.md) | Correo en producción y prueba de punta a punta | 05 |
| [UT-HUI-OLVIDEPASSWORD-BE-08](./UT-HUI-OLVIDEPASSWORD-BE-08.md) | Aviso de contraseña cambiada | 05 |

**Cadena:** 01 → 02; 02, 03 → 04 → 05 → 06, 07, 08

## Criterios de aceptación cubiertos

| CA | Descripción | UTs |
|---|---|---|
| CA01 | Correo con enlace | 02, 04, 06 |
| CA02 | Respuesta uniforme | 04, 06 |
| CA03 | Contraseña cambiada y sesiones cerradas | 05, 06 |
| CA04 | Enlace inválido | 03, 05, 06 |
| CA05 | Contraseña inválida | 05, 06 |
| CA06 | 429 por IP | 04, 05, 06 |
| CA07 | Enlace anterior anulado | 03, 06 |
| CA09 | Respuesta independiente de la cuenta | 04, 06 |
| CA10 | Evento INFO | 05, 06 |
| CA11 | Bitácora | 04, 05, 06 |
| CA12 | Límite por correo silencioso | 04, 06 |
| CA13 | Login simultáneo | 05, 06 |
| CA14 | Igual a la actual, enlace intacto | 05, 06 |
| CA15 | Correo caído | 02, 04, 06, 07 |
| CA16 | Desbloqueo | 05, 06 |
| CA17 | No arranca sin correo | 02, 07 |
| CA18 | Un solo uso simultáneo | 03, 05, 06 |
| CA19 | Aviso del cambio | 08 |

CA08 es de la capa web.

## Follow-ups (no se implementan en estas UT)

- Plantilla HTML del correo con la marca del proyecto.
- Cola persistente y reintentos de envío.
- Proveedor SMTP real y SPF/DKIM del dominio.

## Qué NO incluyen estas UT

- Cambio de contraseña con sesión iniciada (ver HU-INICIAL-CambiarPassword).
- Gestión de usuarios.
