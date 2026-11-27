# UT-HUI-ROLES-BE-03 — Cruces concurrentes y documentación

| Campo | Valor |
|-------|-------|
| **Código** | `UT-HUI-ROLES-BE-03` |
| **Nombre** | Cruces concurrentes y documentación |
| **Historia** | HU-INICIAL-Roles-BE |
| **Depende de** | 02 |

## Antes de implementar

SEGURIDAD_AUTH_BACKEND.

## Solicitud de negocio

Ningún cruce de operaciones puede dejar el sistema sin administradores.

## Objetivo

Pruebas concurrentes entre cambios de rol y desactivación, y reglas al día.

## Alcance

- Ampliar `AdministradoresConcurrenciaTest`: (a) dos administradores que se quitan el rol a la vez; (b) uno quita el rol mientras el otro lo desactiva; (c) cambio de roles a la vez que el login de esa persona (la sesión que sobreviva, si hay, tiene los roles nuevos).
- SEGURIDAD_AUTH_BACKEND: tabla de estado y reglas del cambio de roles.

**Archivos esperados (crear/tocar):**

- `projects/backend/src/test/java/com/example/backend/features/usuarios/AdministradoresConcurrenciaTest.java`
- `.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md`

## Seguridad

A01/A10: comprueba la invariante «siempre hay un administrador activo con su rol».

## Fuera de alcance (no tocar)

- Cambiar la guardia, salvo que una prueba demuestre un defecto.

## Resultado esperado

La invariante queda demostrada en los cruces.

## Validación

1. En cada cruce queda al menos un administrador activo con su rol y la operación perdedora es denegada.
2. Comprobar que las pruebas fallan si se quita el bloqueo de la guardia.
