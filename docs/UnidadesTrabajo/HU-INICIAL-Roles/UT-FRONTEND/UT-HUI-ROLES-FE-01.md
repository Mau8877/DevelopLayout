# UT-HUI-ROLES-FE-01 — Cambiar roles desde la lista de usuarios

| Campo | Valor |
|-------|-------|
| **Código** | `UT-HUI-ROLES-FE-01` |
| **Nombre** | Cambiar roles desde la lista de usuarios |
| **Historia** | HU-INICIAL-Roles-FE |
| **Depende de** | — (con las UT de usuarios y la UT BE-02 hechas) |

## Antes de implementar

COMPONENTIZACION_FRONTEND, TANSTACK_QUERY_FRONTEND y ERRORES_LOGGING_FRONTEND.

## Solicitud de negocio

El administrador cambia roles sin salir de la lista.

## Objetivo

Acción «Cambiar roles» con selección y confirmación.

## Alcance

- `pnpm openapi:types`; `useCambiarRoles`, que invalida los listados.
- Acción por fila que abre `RolesModal` (`ModalLayout` + `CheckboxGroupField`, con los roles actuales marcados; esquema con al menos uno).
- En la cuenta propia (`useSession`), la casilla `SUPER_ADMIN` está deshabilitada y se explica por qué.
- Si no cambió nada, guardar cierra sin llamar. Si cambió, `ModalConfirmacion` avisa que se cerrarán las sesiones (y, en la cuenta propia, que habrá que iniciar sesión de nuevo).
- Éxito: aviso y lista actualizada. Cambio propio: la siguiente petición recibe 401 y `apiClient` lleva al login; la pantalla no lo maneja.
- Error: muestra el mensaje y deja elegir de nuevo.

**Archivos esperados (crear/tocar):**

- `projects/frontend/src/shared/api/schema.d.ts`
- `projects/frontend/src/features/usuarios/api/useCambiarRoles.ts`
- `projects/frontend/src/features/usuarios/schemas/rolesSchema.ts`
- `projects/frontend/src/features/usuarios/components/RolesModal.tsx`
- `projects/frontend/src/features/usuarios/screens/UsuariosScreen.tsx`
- `projects/frontend/src/features/usuarios/tests/RolesModal.test.tsx`

## Seguridad

Deshabilitar la casilla es comodidad; el backend rechaza igual (CA05). Solo se envía la lista de roles.

## Fuera de alcance (no tocar)

- Crear o editar roles.
- Componentes genéricos nuevos.

## Resultado esperado

Los roles se cambian desde la lista, con confirmación.

## Validación

1. Marca los roles actuales; sin ninguno no deja guardar; envío con Enter.
2. No llama sin confirmar ni cuando no hay cambios; al confirmar envía el conjunto completo y refresca.
3. Cuenta propia: `SUPER_ADMIN` deshabilitado.
4. Acceso denegado: un 403 o 409 muestra el mensaje y la lista no cambia.
5. En el navegador: cambiar roles de otro usuario con sesión abierta y comprobar que la pierde.
