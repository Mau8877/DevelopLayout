# UT-HUI-USUARIOS-FE-02 — Grupo de casillas y confirmación genéricos

| Campo | Valor |
|-------|-------|
| **Código** | `UT-HUI-USUARIOS-FE-02` |
| **Nombre** | Grupo de casillas y confirmación genéricos |
| **Historia** | HU-INICIAL-Usuarios-FE |
| **Depende de** | — |

## Antes de implementar

COMPONENTIZACION_FRONTEND y TAILWIND_STYLES_FRONTEND.

## Solicitud de negocio

Elegir roles y confirmar acciones reversibles se va a repetir.

## Objetivo

Dos componentes genéricos en `components/ui/`.

## Alcance

- `checkbox` con el CLI de shadcn (no hay uno todavía).
- `CheckboxGroupField`: leyenda, opciones (`value`, `label`, `disabled?`, `descripcion?`), valores marcados, `onChange` y error inline. Semántico (`fieldset`/`legend`) y operable con teclado.
- `ModalConfirmacion`: título, descripción, texto del botón, estado de envío, confirmar y cancelar. **No** se usa `ModalDestructive` para desactivar o cambiar roles: afirma que la acción no se puede deshacer, y estas sí se pueden.

**Archivos esperados (crear/tocar):**

- `projects/frontend/src/components/ui/checkbox.tsx`
- `projects/frontend/src/components/ui/checkbox-group-field.tsx`
- `projects/frontend/src/components/ui/checkbox-group-field.test.tsx`
- `projects/frontend/src/components/ui/modal-confirmacion.tsx`
- `projects/frontend/src/components/ui/modal-confirmacion.test.tsx`

## Seguridad

Sin impacto de seguridad: componentes sin datos ni dominio.

## Fuera de alcance (no tocar)

- Textos o tipos de usuarios dentro de los componentes.
- Cambiar `ModalDestructive`.

## Resultado esperado

Hay un grupo de casillas y una confirmación reutilizables.

## Validación

1. Se marca y desmarca con mouse y teclado; muestra el error; respeta opciones deshabilitadas.
2. La confirmación no se cierra mientras envía; cancelar no confirma.
