# UT-HUI-IDIOMAS-FE-02 — Selector de idioma

| Campo | Valor |
|-------|-------|
| **Código** | `UT-HUI-IDIOMAS-FE-02` |
| **Nombre** | Selector de idioma |
| **Historia** | HU-INICIAL-Idiomas-FE |
| **Depende de** | 01 |

## Antes de implementar

COMPONENTIZACION_FRONTEND y TAILWIND_STYLES_FRONTEND.

## Solicitud de negocio

La persona elige su idioma, también antes de entrar.

## Objetivo

Un selector accesible en la cabecera y en las pantallas sin sesión.

## Alcance

- Componente `SelectorIdioma` en `components/layout/`, con `dropdown-menu`; cada idioma se nombra en su propio idioma («Español», «English»).
- Va en `AppHeader` y en `CenteredCardLayout` (login, recuperación, restablecer e invitación).
- Cambiar no recarga ni borra lo escrito.

**Archivos esperados (crear/tocar):**

- `projects/frontend/src/components/layout/SelectorIdioma.tsx`
- `projects/frontend/src/components/layout/SelectorIdioma.test.tsx`
- `projects/frontend/src/components/layout/AppHeader.tsx`
- `projects/frontend/src/components/layout/CenteredCardLayout.tsx`
- `projects/frontend/src/shared/language/es.json`
- `projects/frontend/src/shared/language/en.json`

## Seguridad

Sin impacto de seguridad: no cambia permisos ni datos.

## Fuera de alcance (no tocar)

- Guardar el idioma en la cuenta.

## Resultado esperado

El idioma se cambia desde cualquier pantalla.

## Validación

1. Operable con teclado, marca el idioma actual y lo recuerda al recargar.
2. Cambiar de idioma con un formulario a medio llenar conserva los valores.
