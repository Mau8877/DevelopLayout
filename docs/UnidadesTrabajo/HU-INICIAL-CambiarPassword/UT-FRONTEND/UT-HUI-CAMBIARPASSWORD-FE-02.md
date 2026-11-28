# UT-HUI-CAMBIARPASSWORD-FE-02 — Pantalla y opción del menú

| Campo | Valor |
|-------|-------|
| **Código** | `UT-HUI-CAMBIARPASSWORD-FE-02` |
| **Nombre** | Pantalla y opción del menú |
| **Historia** | HU-INICIAL-CambiarPassword-FE |
| **Depende de** | 01 |

## Antes de implementar

COMPONENTIZACION_FRONTEND, RUTAS_NAVEGACION_FRONTEND, IDIOMAS_FRONTEND y TESTING_FRONTEND.

## Solicitud de negocio

La opción «Cambiar contraseña» del menú tiene que funcionar.

## Objetivo

Pantalla en `/change-password`, abierta desde el menú, para cualquier rol.

## Alcance

- `CambiarPasswordScreen`: tres campos (`autocomplete` `current-password` y `new-password`), aviso previo de que se cerrarán todas las sesiones y habrá que volver a entrar, botón deshabilitado mientras envía y envío con Enter.
- `ERR_AUTH_12` bajo la contraseña actual; `ERR_AUTH_06` y `ERR_SYS_01` bajo la nueva; 423 y 429 como advertencia; el resto como error del formulario. Se muestra el mensaje del servidor.
- Al éxito, aviso en el login: la contraseña cambió y hay que entrar con la nueva.
- Ruta `_authenticated/change-password.tsx` con `LayoutPorRol` (pantalla común a todos los roles) y `routeTree.gen.ts` regenerado.
- `AppHeader` y `AppShell` reciben `onChangePassword` (no conocen rutas); los dos layouts de rol lo conectan.

**Archivos esperados (crear/tocar):**

- `projects/frontend/src/features/auth/screens/CambiarPasswordScreen.tsx`
- `projects/frontend/src/features/auth/screens/index.ts`
- `projects/frontend/src/features/auth/index.ts`
- `projects/frontend/src/routes/_authenticated/change-password.tsx`
- `projects/frontend/src/routeTree.gen.ts`
- `projects/frontend/src/components/layout/AppHeader.tsx`
- `projects/frontend/src/components/layout/AppShell.tsx`
- `projects/frontend/src/layouts/admin/AdminLayout.tsx`
- `projects/frontend/src/layouts/usuario/UsuarioLayout.tsx`
- `projects/frontend/src/features/auth/language/es.json`
- `projects/frontend/src/features/auth/language/en.json`
- `projects/frontend/src/features/auth/tests/CambiarPasswordScreen.test.tsx`

## Seguridad

La ruta protegida es comodidad; quien autoriza es el backend. Los mensajes se muestran como texto.

## Fuera de alcance (no tocar)

- Las opciones «Mi perfil» y «Configuración».
- Reutilizar la pantalla de restablecer, que usa un token y no pide la actual.

## Resultado esperado

Cualquier usuario puede cambiar su contraseña desde la web.

## Validación

1. Envío con Enter; un solo envío mientras está pendiente.
2. Cada código de error aparece en su lugar y la persona sigue en la pantalla.
3. Al éxito llega al login con el aviso.
4. La pantalla se ve en español y en inglés.
5. Acceso denegado: sin sesión, `/change-password` lleva al login.
6. `pnpm lint`, `pnpm test` y `pnpm build` en verde.
