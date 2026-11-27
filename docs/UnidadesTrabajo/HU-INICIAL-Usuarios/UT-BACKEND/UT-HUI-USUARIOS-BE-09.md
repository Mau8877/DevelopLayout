# UT-HUI-USUARIOS-BE-09 — «Olvidé mi contraseña» acepta la invitación

| Campo | Valor |
|-------|-------|
| **Código** | `UT-HUI-USUARIOS-BE-09` |
| **Nombre** | «Olvidé mi contraseña» acepta la invitación |
| **Historia** | HU-INICIAL-Usuarios-BE |
| **Depende de** | 08 |

## Antes de implementar

SEGURIDAD_AUTH_BACKEND (recuperación).

## Solicitud de negocio

Quien define su contraseña por recuperación ya demostró que controla el correo.

## Objetivo

Restablecer la contraseña de una cuenta pendiente la deja aceptada.

## Alcance

- En `RecuperacionService.restablecer`, dentro de la transacción de escritura: si la cuenta está pendiente, quitar la marca y registrar además `AUTH_INVITACION_ACEPTADA`.
- Después de confirmar, anular el token de invitación. Aunque la anulación falle, el enlace no sirve porque la cuenta ya no está pendiente.
- `solicitar` no cambia: una cuenta pendiente y activa ya recibe el enlace.

**Archivos esperados (crear/tocar):**

- `projects/backend/src/main/java/com/example/backend/features/auth/service/RecuperacionService.java`
- `projects/backend/src/test/java/com/example/backend/features/auth/RecuperacionInvitacionTest.java`

## Seguridad

No cambia quién puede restablecer ni las respuestas de `forgot` (siguen sin revelar si la cuenta existe).

## Fuera de alcance (no tocar)

- Cambiar mensajes o límites de la recuperación.

## Resultado esperado

La cuenta queda aceptada por cualquiera de los dos caminos.

## Validación

1. Cuenta pendiente que restablece: queda aceptada, con los dos eventos, y puede iniciar sesión.
2. Acceso denegado: después, el enlace de invitación da `ERR_AUTH_11`.
3. Las pruebas de recuperación existentes siguen en verde.
