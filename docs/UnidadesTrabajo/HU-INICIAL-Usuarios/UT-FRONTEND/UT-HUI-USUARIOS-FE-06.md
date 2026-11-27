# UT-HUI-USUARIOS-FE-06 — Pantalla para aceptar la invitación

| Campo | Valor |
|-------|-------|
| **Código** | `UT-HUI-USUARIOS-FE-06` |
| **Nombre** | Pantalla para aceptar la invitación |
| **Historia** | HU-INICIAL-Usuarios-FE |
| **Depende de** | — (necesita el endpoint de la UT BE-08) |

## Antes de implementar

RUTAS_NAVEGACION_FRONTEND, FORMULARIOS_ZOD_TANSTACK_FRONTEND, `ResetPasswordPage` y CI_CD (prueba de producción).

## Solicitud de negocio

La persona invitada elige su contraseña desde el enlace.

## Objetivo

Ruta pública `/accept-invitation`.

## Alcance

- Extraer la lectura del token del fragmento de `ResetPasswordPage` a un hook de la feature `auth` (`useTokenDelFragmento`) y usarlo en ambas páginas: el token queda solo en memoria y sale de la URL.
- Extraer de `ResetPasswordScreen` el formulario de contraseña y confirmación a un componente de la feature, reutilizado por las dos pantallas. Esquema: `resetPasswordSchema`.
- `AceptarInvitacionScreen`: al terminar, aviso y `/login`. Con `ERR_AUTH_11` o sin token: explica que pida otra invitación al administrador. Los errores de contraseña van bajo el campo.
- nginx (desarrollo y producción): `Referrer-Policy: no-referrer` también para `/accept-invitation`; `prod-smoke.mjs` lo comprueba.

**Archivos esperados (crear/tocar):**

- `projects/frontend/src/routes/accept-invitation.tsx`
- `projects/frontend/src/routeTree.gen.ts`
- `projects/frontend/src/features/auth/screens/AceptarInvitacionPage.tsx`
- `projects/frontend/src/features/auth/screens/AceptarInvitacionScreen.tsx`
- `projects/frontend/src/features/auth/screens/ResetPasswordPage.tsx`
- `projects/frontend/src/features/auth/screens/ResetPasswordScreen.tsx`
- `projects/frontend/src/features/auth/components/FormularioNuevaPassword.tsx`
- `projects/frontend/src/features/auth/api/useTokenDelFragmento.ts`
- `projects/frontend/src/features/auth/api/useAceptarInvitacion.ts`
- `projects/frontend/src/features/auth/tests/AceptarInvitacionScreen.test.tsx`
- `projects/frontend/src/features/auth/index.ts`
- `infrastructure/nginx/nginx.conf`
- `infrastructure/nginx/nginx.prod.conf`
- `scripts/prod-smoke.mjs`

## Seguridad

El token no va en la consulta, no se guarda en almacenamiento del navegador ni viaja en `Referer`. Es pública porque la credencial es el enlace.

## Fuera de alcance (no tocar)

- Iniciar sesión automáticamente.
- Pantalla en mobile.

## Resultado esperado

El enlace del correo permite activar la cuenta desde la web.

## Validación

1. Envío válido con Enter; contraseñas que no coinciden no envían; la URL queda sin token.
2. Acceso denegado: enlace inservible o ausente muestra el mensaje y no envía.
3. Las pruebas de restablecimiento siguen en verde.
4. `pnpm test:prod` verifica la cabecera en `/accept-invitation`.
5. En el navegador con Mailpit: crear, abrir el correo, aceptar e iniciar sesión.
