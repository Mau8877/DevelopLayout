# UT-HUI-USUARIOS-FE-01 — Tipos, esquemas y llamadas de usuarios

| Campo | Valor |
|-------|-------|
| **Código** | `UT-HUI-USUARIOS-FE-01` |
| **Nombre** | Tipos, esquemas y llamadas de usuarios |
| **Historia** | HU-INICIAL-Usuarios-FE |
| **Depende de** | — (necesita el contrato de las UT de backend) |

## Antes de implementar

ARQUITECTURA_FRONTEND, TANSTACK_QUERY_FRONTEND (criterios sensibles) y FORMULARIOS_ZOD_TANSTACK_FRONTEND.

## Solicitud de negocio

Las pantallas necesitan hablar con la API sin repetir lógica.

## Objetivo

La feature `usuarios` con hooks tipados desde el contrato.

## Alcance

- `pnpm openapi:types` y tipos tomados de `shared/api/schema.d.ts`.
- `api/keys.ts`, `useUsuarios` (consulta por POST), `useCrearUsuario`, `useReenviarInvitacion`, `useDesactivarUsuario`, `useReactivarUsuario`.
- El texto buscado **no entra en la `queryKey`** (mismo patrón que `useSesionesAdmin`); `placeholderData` solo conserva resultados de la misma búsqueda.
- Toda mutación invalida los listados.
- `schemas/crearUsuarioSchema.ts`: correo válido y al menos un rol.
- `index.ts` de la feature.

**Archivos esperados (crear/tocar):**

- `projects/frontend/src/shared/api/schema.d.ts`
- `projects/frontend/src/features/usuarios/api/keys.ts`
- `projects/frontend/src/features/usuarios/api/useUsuarios.ts`
- `projects/frontend/src/features/usuarios/api/useCrearUsuario.ts`
- `projects/frontend/src/features/usuarios/api/useReenviarInvitacion.ts`
- `projects/frontend/src/features/usuarios/api/useCambiarEstadoUsuario.ts`
- `projects/frontend/src/features/usuarios/schemas/crearUsuarioSchema.ts`
- `projects/frontend/src/features/usuarios/types/usuario.ts`
- `projects/frontend/src/features/usuarios/tests/usuariosHooks.test.tsx`
- `projects/frontend/src/features/usuarios/index.ts`

## Seguridad

El correo no queda en la URL, en la `queryKey` ni en logs.

## Fuera de alcance (no tocar)

- Pantallas.

## Resultado esperado

La feature puede consultar y modificar usuarios.

## Validación

1. Cada hook llama a su endpoint y las mutaciones invalidan los listados.
2. La `queryKey` no contiene el texto buscado.
3. Acceso denegado: un 403 llega como `ApiError` sin datos en caché.
