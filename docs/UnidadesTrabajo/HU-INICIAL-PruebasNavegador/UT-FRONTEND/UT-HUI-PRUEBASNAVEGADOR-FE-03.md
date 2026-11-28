# UT-HUI-PRUEBASNAVEGADOR-FE-03 — Flujos de cuentas y administración

| Campo | Valor |
|-------|-------|
| **Código** | `UT-HUI-PRUEBASNAVEGADOR-FE-03` |
| **Nombre** | Flujos de cuentas y administración |
| **Historia** | HU-INICIAL-PruebasNavegador-FE |
| **Depende de** | 01 |

## Antes de implementar

SEGURIDAD_AUTH_BACKEND (recuperación y administración).

## Solicitud de negocio

Son los flujos que cruzan web, backend, base y correo.

## Objetivo

Recuperación, invitación, usuarios, roles y permisos en el navegador.

## Alcance

- Recuperar la contraseña con el correo capturado; el enlace no sirve dos veces.
- Invitar, aceptar e iniciar sesión; buscar, desactivar y reactivar; cambiar roles y ver caer la sesión afectada, también la propia.
- Un `USUARIO` no entra a las pantallas de administración; la bitácora muestra un evento de lo hecho.

**Archivos esperados (crear/tocar):**

- `projects/frontend/e2e/cuentas.spec.ts`
- `projects/frontend/e2e/administracion.spec.ts`
- `projects/frontend/e2e/soporte/`

## Seguridad

A01: acceso permitido y denegado comprobados en el navegador. Los enlaces solo se leen del buzón desechable.

## Fuera de alcance (no tocar)

- Cubrir cada validación de los formularios (ya está en las pruebas de componentes).

## Resultado esperado

Los flujos de cuentas están probados de punta a punta.

## Validación

1. Cada prueba crea sus datos y pasa sola o junto a las demás.
2. Acceso denegado: `USUARIO` en `/users` termina fuera.
