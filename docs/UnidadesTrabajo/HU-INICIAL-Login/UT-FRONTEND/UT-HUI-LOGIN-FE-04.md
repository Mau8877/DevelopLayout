# UT-HUI-LOGIN-FE-04 — Pantalla de login

| Campo | Valor |
|-------|-------|
| **Código** | `UT-HUI-LOGIN-FE-04` |
| **Nombre** | Pantalla de login |
| **Historia** | HU-INICIAL-Login-FE |
| **Depende de** | UT-HUI-LOGIN-FE-03 |

## Antes de implementar

Leer `frontend/COMPONENTIZACION_FRONTEND.md`, `frontend/ERRORES_LOGGING_FRONTEND.md`, `frontend/FORMULARIOS_ZOD_TANSTACK_FRONTEND.md`, `frontend/TAILWIND_STYLES_FRONTEND.md`, `frontend/RUTAS_NAVEGACION_FRONTEND.md` y `.claude/rules/SEGURIDAD_OWASP.md`.

## Solicitud de negocio

Es la puerta de entrada de los usuarios.

## Objetivo

Existe `LoginScreen` en la ruta pública `/login`.

## Alcance

- `screens/LoginScreen.tsx`: TanStack Form + `loginSchema` (`validators.onChange`), errores inline en español, botón deshabilitado y con `debounce` mientras envía.
- `autoComplete="username"`/`"current-password"`, `type="password"`.
- Error del backend mostrado de forma genérica; 423 muestra aviso de bloqueo.
- `routes/login.tsx` monta la pantalla; si ya hay sesión redirige a `/panel`. Tras éxito redirige según el rol.
- Componentes genéricos nuevos (no existían), todos en `components/`, sin conocimiento de auth y con props: `components/ui/label.tsx` y `components/ui/alert.tsx` (estilo shadcn, escritos a mano si no se puede correr el CLI), `components/ui/form-field.tsx` (label + control + error inline + descripción) con su test, y `components/layout/CenteredCardLayout.tsx` (layout centrado para pantallas públicas).
- `LoginScreen` solo compone esos componentes con `input`, `button` y `card` ya existentes; ningún componente genérico vive en `features/auth/components/`.
- Solo clases de tema de Tailwind.

**Archivos esperados (crear/tocar):**

- `projects/frontend/src/features/auth/screens/LoginScreen.tsx`
- `projects/frontend/src/routes/login.tsx`
- `projects/frontend/src/features/auth/index.ts`
- `projects/frontend/src/components/ui/label.tsx`
- `projects/frontend/src/components/ui/alert.tsx`
- `projects/frontend/src/components/ui/form-field.tsx`
- `projects/frontend/src/components/ui/form-field.test.tsx`
- `projects/frontend/src/components/layout/CenteredCardLayout.tsx`

## Fuera de alcance (no tocar)

- Guard de rutas privadas (UT-HUI-LOGIN-FE-05).
- Enlace "¿Olvidaste tu contraseña?" (lo agrega HU-INICIAL-OlvidePassword).
- Registro de usuarios.

## Resultado esperado

El usuario inicia sesión desde `/login` y llega a su pantalla.

## Validación

1. Login correcto redirige; contraseña mala muestra el error genérico; vacíos muestran errores bajo el campo.
2. `pnpm lint` y `pnpm build` sin errores.

## Seguridad

A05/A07: sin `dangerouslySetInnerHTML`, sin detalles internos, sin guardar la contraseña.
