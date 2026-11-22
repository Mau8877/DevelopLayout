# UT-HUI-LOGIN-FE-02 — Schema Zod y tipos de login

| Campo | Valor |
|-------|-------|
| **Código** | `UT-HUI-LOGIN-FE-02` |
| **Nombre** | Schema Zod y tipos de login |
| **Historia** | HU-INICIAL-Login-FE |
| **Depende de** | — |

## Antes de implementar

Leer `frontend/FORMULARIOS_ZOD_TANSTACK_FRONTEND.md` y `.claude/rules/SEGURIDAD_OWASP.md`.

## Solicitud de negocio

Formulario y payload deben validar lo mismo, con mensajes en español.

## Objetivo

Existen `loginSchema` y los tipos de sesión.

## Alcance

- `schemas/loginSchema.ts`: `email` (obligatorio, válido), `password` (obligatorio, mínimo 8, máximo 72 bytes).
- `types/auth.ts`: `UsuarioSesion` (`codigo`, `email`, `roles: string[]`).
- `index.ts` del feature re-exporta lo público.

**Archivos esperados (crear/tocar):**

- `projects/frontend/src/features/auth/schemas/loginSchema.ts`
- `projects/frontend/src/features/auth/types/auth.ts`
- `projects/frontend/src/features/auth/index.ts`

## Fuera de alcance (no tocar)

- Hooks, pantallas, rutas.
- Otras reglas de contraseña (solo largo).

## Resultado esperado

El schema acepta credenciales válidas y rechaza vacías, correo inválido y contraseña corta.

## Validación

1. `pnpm test`: `tests/loginSchema.test.ts` cubre los casos.

## Seguridad

A05: validación también en cliente (no reemplaza la del backend).
