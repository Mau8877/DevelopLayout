# UT-HUI-USUARIOS-FE-04 — Crear usuario

| Campo | Valor |
|-------|-------|
| **Código** | `UT-HUI-USUARIOS-FE-04` |
| **Nombre** | Crear usuario |
| **Historia** | HU-INICIAL-Usuarios-FE |
| **Depende de** | 01, 02, 03 |

## Antes de implementar

FORMULARIOS_ZOD_TANSTACK_FRONTEND y COMPONENTIZACION_FRONTEND (`modal-layout`).

## Solicitud de negocio

El administrador da de alta indicando correo y roles.

## Objetivo

Botón «Crear usuario» que abre un modal con el formulario.

## Alcance

- `CrearUsuarioModal` con `ModalLayout`, `FormField` y `CheckboxGroupField` (roles `SUPER_ADMIN` y `USUARIO`, definidos una vez en la feature).
- Solo `validators.onChange` y `validarAlSalir`.
- Éxito: cierra y avisa. Si `invitacionEnviada` es `false`, aviso de advertencia: la cuenta se creó, el correo no salió y se puede reenviar.
- Error: el modal sigue abierto con el mensaje del backend (el 409 del correo repetido). Sin doble envío.

**Archivos esperados (crear/tocar):**

- `projects/frontend/src/features/usuarios/components/CrearUsuarioModal.tsx`
- `projects/frontend/src/features/usuarios/components/roles.ts`
- `projects/frontend/src/features/usuarios/screens/UsuariosScreen.tsx`
- `projects/frontend/src/features/usuarios/tests/CrearUsuarioModal.test.tsx`

## Seguridad

API3: solo se envían correo y roles. No hay campo de contraseña.

## Fuera de alcance (no tocar)

- Editar usuarios o sus roles.

## Resultado esperado

Se pueden crear usuarios y se sabe si la invitación salió.

## Validación

1. Envío válido **con Enter**; correo inválido o sin roles muestran error inline y no envían.
2. Prueba del aviso cuando el correo no salió.
3. Acceso denegado o correo repetido: muestra el mensaje y no cierra.
