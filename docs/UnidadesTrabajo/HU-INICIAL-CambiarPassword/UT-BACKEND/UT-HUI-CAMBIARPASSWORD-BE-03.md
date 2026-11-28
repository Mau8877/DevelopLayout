# UT-HUI-CAMBIARPASSWORD-BE-03 — Correo de aviso de contraseña cambiada

| Campo | Valor |
|-------|-------|
| **Código** | `UT-HUI-CAMBIARPASSWORD-BE-03` |
| **Nombre** | Correo de aviso de contraseña cambiada |
| **Historia** | HU-INICIAL-CambiarPassword-BE |
| **Depende de** | — |

## Antes de implementar

IDIOMAS_BACKEND (correos) y SEGURIDAD_AUTH_BACKEND (correo).

## Solicitud de negocio

La persona tiene que enterarse si alguien cambió su contraseña.

## Objetivo

Se puede enviar el aviso en español o inglés.

## Alcance

- `CorreoCuentaService.enviarAvisoCambioPassword(String destinatario, Locale idioma)`.
- Claves `auth.correo.cambio.asunto` y `auth.correo.cambio.cuerpo` en los dos archivos de idioma. El cuerpo dice que la contraseña cambió, que se cerraron las sesiones y que, si no fue esa persona, use «¿Olvidaste tu contraseña?» y avise a un administrador.
- Sin enlaces, tokens ni contraseñas.

**Archivos esperados (crear/tocar):**

- `projects/backend/src/main/java/com/example/backend/features/auth/service/CorreoCuentaService.java`
- `projects/backend/src/main/resources/i18n/auth/messages.properties`
- `projects/backend/src/main/resources/i18n/auth/messages_en.properties`
- `projects/backend/src/test/java/com/example/backend/features/auth/service/CorreoCuentaServiceTest.java`

## Seguridad

A07: el aviso no lleva nada que sirva para entrar a la cuenta.

## Fuera de alcance (no tocar)

- Los correos de recuperación e invitación.
- Enviar este aviso desde la recuperación de contraseña.
- Llamar al método desde el flujo (BE-04).

## Resultado esperado

El aviso existe en los dos idiomas.

## Validación

1. En cada idioma: asunto y cuerpo correctos, remitente de la configuración.
2. El cuerpo no contiene `http` ni marcadores sin reemplazar.
3. Las pruebas de archivos de idioma siguen en verde.
