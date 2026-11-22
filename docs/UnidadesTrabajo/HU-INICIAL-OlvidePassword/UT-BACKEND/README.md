# Unidades de trabajo — HU-INICIAL-OlvidePassword-BE

**Historia:** Recuperar la contraseña con enlace por correo de un solo uso.
**Fuente HU:** [`HU-INICIAL-OlvidePassword.md`](../../../HistoriasDeUsuario/HU-INICIAL-OlvidePassword.md)
**Recurso HTTP / contrato:** `POST /api/v1/auth/password/forgot`, `POST /api/v1/auth/password/reset`.

## Antes de implementar cualquier UT

Leer las rules de [`backend/`](../../../../.claude/rules/backend/) y [`SEGURIDAD_OWASP.md`](../../../../.claude/rules/SEGURIDAD_OWASP.md). Estas fichas **no** sustituyen esas reglas: si hay conflicto, ganan las rules del proyecto.

## Decisiones de diseño

- El token viaja en el fragmento del enlace (`#token=`), que el navegador no envía al servidor ni en `Referer`; luego va por body al endpoint.
- Se implementa después de Login y Refresh: reutiliza `LoginAttemptService.reiniciar` y `RefreshTokenService.revocarTodas`.

## Orden de ejecución

| Código | Nombre | Depende de |
|--------|--------|------------|
| [UT-HUI-OLVIDEPASSWORD-BE-01](./UT-HUI-OLVIDEPASSWORD-BE-01.md) | Mailpit en el compose de desarrollo | — |
| [UT-HUI-OLVIDEPASSWORD-BE-02](./UT-HUI-OLVIDEPASSWORD-BE-02.md) | MailService y plantilla del correo de recuperación | UT-HUI-OLVIDEPASSWORD-BE-01 |
| [UT-HUI-OLVIDEPASSWORD-BE-03](./UT-HUI-OLVIDEPASSWORD-BE-03.md) | PasswordResetTokenService | Redis (existente), HU-INICIAL-Refresh: RefreshTokenService.revocarTodas |
| [UT-HUI-OLVIDEPASSWORD-BE-04](./UT-HUI-OLVIDEPASSWORD-BE-04.md) | Solicitar recuperación: POST /auth/password/forgot | UT-HUI-OLVIDEPASSWORD-BE-02, UT-HUI-OLVIDEPASSWORD-BE-03 |
| [UT-HUI-OLVIDEPASSWORD-BE-05](./UT-HUI-OLVIDEPASSWORD-BE-05.md) | Restablecer contraseña: POST /auth/password/reset | UT-HUI-OLVIDEPASSWORD-BE-03, UT-HUI-OLVIDEPASSWORD-BE-04 |
| [UT-HUI-OLVIDEPASSWORD-BE-06](./UT-HUI-OLVIDEPASSWORD-BE-06.md) | Tests de recuperación de contraseña | UT-HUI-OLVIDEPASSWORD-BE-05 |

**Cadena:** 01 → 02 → 03 → 04 → 05 → 06

## Criterios de aceptación cubiertos

| CA (HU-INICIAL-OlvidePassword) | Descripción | UTs |
|---|---|---|
| CA01 | Correo enviado | 02, 04, 06 |
| CA02 | Respuesta uniforme | 04, 06 |
| CA03 | Contraseña cambiada y sesiones cerradas | 05, 06 |
| CA04 | Token inválido | 03, 05, 06 |
| CA05 | Política de contraseña | 05, 06 |
| CA06 | Límite 429 | 04, 06 |
| CA07 | Enlace anterior anulado | 03, 06 |
| CA09 | Tiempo equivalente | 04, 06 |
| CA10 | Evento INFO | 05, 06 |

## Follow-ups (no se implementan en estas UT)

- Plantilla HTML de correo con marca del proyecto.
- SMTP real y dominio con SPF/DKIM en producción.

## Qué NO incluyen estas UT

- Cambio de contraseña con sesión iniciada.
