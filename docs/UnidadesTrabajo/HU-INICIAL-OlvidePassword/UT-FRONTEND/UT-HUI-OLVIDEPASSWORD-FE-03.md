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

- `routes/reset-password.tsx` pública (también con sesión iniciada): toma el token de `location.hash` (`#token=`), lo guarda **solo en memoria** y quita el fragmento de la URL de inmediato, reemplazando la entrada del historial (comprobar que el router no lo restaure y que el doble montaje de `StrictMode` no pierda el token).
- `screens/ResetPasswordScreen.tsx`: contraseña y confirmación. Éxito: aviso y redirección a `/login`. `ERR_AUTH_05` o sin token: vista de enlace inválido con acceso a pedir otro. `ERR_AUTH_06` y errores de validación: bajo el campo, **conservando el token** para reintentar. 429: aviso de espera. Al recargar la página el token se pierde y se muestra la vista de enlace inválido.
- `Referrer-Policy: no-referrer` en esa ruta, **sin `add_header` dentro de un `location`** (nginx dejaría de heredar HSTS, CSP y el resto): en `nginx.prod.conf` y `nginx.conf`, un `map` sobre `$uri` da el valor (`/reset-password`, con o sin `/` final → `no-referrer`; el resto, el actual) y el `add_header Referrer-Policy` que ya existe usa esa variable. El fragmento nunca viaja en `Referer`: esto es una segunda defensa.
- `scripts/prod-smoke.mjs`: `/reset-password` responde con `no-referrer` **y conserva** las demás cabeceras; `/` sigue con la política actual.

**Archivos esperados (crear/tocar):**

- `projects/frontend/src/routes/reset-password.tsx`
- `projects/frontend/src/routeTree.gen.ts` (generado)
- `projects/frontend/src/features/auth/screens/ResetPasswordScreen.tsx`
- `projects/frontend/src/features/auth/screens/ResetPasswordPage.tsx` (toma el token y limpia la URL)
- `projects/frontend/src/features/auth/index.ts`
- `infrastructure/nginx/nginx.prod.conf`
- `infrastructure/nginx/nginx.conf`
- `scripts/prod-smoke.mjs`

## Fuera de alcance (no tocar)

- Pedir el enlace (UT 02).

## Resultado esperado

Abrir el enlace muestra el formulario y la URL ya no contiene el token.

## Validación

1. Abrir `/reset-password#token=abc`: la barra queda en `/reset-password` y el botón «atrás» no recupera el token.
2. Con un token válido, elegir la contraseña actual (error bajo el campo) y luego otra: redirige a login.
3. `pnpm test:prod`: cabeceras de `/reset-password` y de `/` correctas.

## Seguridad

A07/A02: token fuera de URL, historial y Referer.
