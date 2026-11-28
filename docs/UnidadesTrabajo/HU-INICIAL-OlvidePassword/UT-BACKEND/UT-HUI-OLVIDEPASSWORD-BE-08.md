# UT-HUI-OLVIDEPASSWORD-BE-08 — Aviso de contraseña cambiada

| Campo | Valor |
|-------|-------|
| **Código** | `UT-HUI-OLVIDEPASSWORD-BE-08` |
| **Nombre** | Aviso de contraseña cambiada |
| **Historia** | HU-INICIAL-OlvidePassword-BE |
| **Depende de** | 05 (y el correo de aviso, que ya existe por HU-INICIAL-CambiarPassword) |

## Antes de implementar

SEGURIDAD_AUTH_BACKEND (recuperación y correo) e IDIOMAS_BACKEND.

## Solicitud de negocio

La persona tiene que enterarse de cualquier cambio de su contraseña, también del que se hace con un enlace.

## Objetivo

Un restablecimiento exitoso envía el aviso.

## Alcance

- `AvisoCambioPassword`: el envío en el ejecutor acotado, con el idioma capturado en la petición, que usan el cambio con sesión y la recuperación.
- `RecuperacionService.restablecer()` lo llama con el cambio ya confirmado.
- Ampliar la prueba de navegador de la recuperación.

**Archivos esperados (crear/tocar):**

- `projects/backend/src/main/java/com/example/backend/features/auth/service/AvisoCambioPassword.java`
- `projects/backend/src/main/java/com/example/backend/features/auth/service/RecuperacionService.java`
- `projects/backend/src/main/java/com/example/backend/features/auth/service/CambioPasswordService.java`
- `projects/backend/src/test/java/com/example/backend/features/auth/AuthPasswordResetTest.java`
- `projects/frontend/e2e/cuentas.spec.ts`

## Seguridad

A07 y A09: el aviso no lleva nada que sirva para entrar y un fallo se registra sin el mensaje de la excepción. No cambia la respuesta de `forgot`.

## Fuera de alcance (no tocar)

- El texto del correo y los enlaces de recuperación.
- Avisar al aceptar una invitación.

## Resultado esperado

Todo cambio de contraseña hecho por la persona queda avisado.

## Validación

1. El aviso sale en español y en inglés según la petición.
2. Acceso denegado: un token inventado o una contraseña rechazada no envían nada.
3. Con el correo caído, la contraseña cambia y el fallo queda registrado sin datos.
4. `pnpm test:e2e` recibe el aviso en el buzón de prueba.
