# UT-HUI-OLVIDEPASSWORD-FE-03 — Pantalla Restablecer contraseña con token del fragmento

| Campo | Valor |
|-------|-------|
| **Código** | `UT-HUI-OLVIDEPASSWORD-FE-03` |
| **Nombre** | Pantalla Restablecer contraseña con token del fragmento |
| **Historia** | HU-INICIAL-OlvidePassword-FE |
| **Depende de** | UT-HUI-OLVIDEPASSWORD-FE-01 |

## Antes de implementar

Leer `frontend/RUTAS_NAVEGACION_FRONTEND.md`, `SEGURIDAD_OWASP.md` (Frontend) y `.claude/rules/SEGURIDAD_OWASP.md`.

## Solicitud de negocio

El token no debe quedar en el historial, logs ni cabecera `Referer`.

## Objetivo

Existe `ResetPasswordScreen` que lee el token, lo quita de la URL y restablece.

## Alcance

- `routes/reset-password.tsx` pública: lee `location.hash` (`#token=`), lo guarda en un `useRef` y llama a `history.replaceState` para borrarlo de la URL de inmediato.
- `screens/ResetPasswordScreen.tsx`: formulario de contraseña + confirmación; éxito → mensaje y redirección a `/login`; token inválido (`ERR_AUTH_05`) → error con enlace a pedir otro.
- Ruta servida con `Referrer-Policy: no-referrer` (meta o cabecera en `infrastructure/nginx/*.conf` y `projects/frontend/nginx.conf`).

**Archivos esperados (crear/tocar):**

- `projects/frontend/src/routes/reset-password.tsx`
- `projects/frontend/src/features/auth/screens/ResetPasswordScreen.tsx`
- `projects/frontend/nginx.conf`
- `infrastructure/nginx/nginx.prod.conf`

## Fuera de alcance (no tocar)

- Pedir el enlace (UT 02).

## Resultado esperado

Abrir el enlace muestra el formulario y la URL ya no contiene el token.

## Validación

1. Abrir `/reset-password#token=abc`: la barra queda en `/reset-password`; restablecer con token válido redirige a login.
2. Revisar cabeceras: `Referrer-Policy: no-referrer`.

## Seguridad

A07/A02: token fuera de URL, historial y Referer.
