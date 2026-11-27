# UT-HUI-USUARIOS-BE-03 — Correo de invitación

| Campo | Valor |
|-------|-------|
| **Código** | `UT-HUI-USUARIOS-BE-03` |
| **Nombre** | Correo de invitación |
| **Historia** | HU-INICIAL-Usuarios-BE |
| **Depende de** | — |

## Antes de implementar

SEGURIDAD_AUTH_BACKEND y `CorreoRecuperacionService`.

## Solicitud de negocio

La persona invitada recibe el enlace para elegir su contraseña.

## Objetivo

Un único servicio de correo de cuenta que envía recuperación e invitación.

## Alcance

- Renombrar `CorreoRecuperacionService` a `CorreoCuentaService` (misma validación de arranque).
- `enviarEnlace` pasa a `enviarRecuperacion`; nuevo `enviarInvitacion(destinatario, token)`.
- Enlace `{app.frontend-base-url}/accept-invitation#token=...`; texto plano, en español, que avisa que sirve una vez, vence en 24 horas y, si venció, hay que pedir otra al administrador.

**Archivos esperados (crear/tocar):**

- `projects/backend/src/main/java/com/example/backend/features/auth/service/CorreoCuentaService.java`
- `projects/backend/src/main/java/com/example/backend/features/auth/service/RecuperacionService.java`
- `projects/backend/src/test/java/com/example/backend/features/auth/CorreoCuentaServiceTest.java`

## Seguridad

El token va en el fragmento y la dirección sale solo de la configuración. El servicio no loguea.

## Fuera de alcance (no tocar)

- Quién llama al envío (UT 06 y 07).

## Resultado esperado

La aplicación puede enviar los dos correos.

## Validación

1. El enlace usa la URL configurada y la ruta `/accept-invitation` con el token en el fragmento.
2. Las pruebas del correo de recuperación (incluida la validación de arranque) siguen en verde con el nombre nuevo.
