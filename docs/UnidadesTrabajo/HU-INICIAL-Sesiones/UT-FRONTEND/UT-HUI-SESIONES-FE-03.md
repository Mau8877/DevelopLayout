# UT-HUI-SESIONES-FE-03 — Tests de la pantalla

| Campo | Valor |
|-------|-------|
| **Código** | `UT-HUI-SESIONES-FE-03` |
| **Nombre** | Tests de la pantalla |
| **Historia** | HU-INICIAL-Sesiones-FE |
| **Depende de** | UT-HUI-SESIONES-FE-02 |

## Antes de implementar

Leer `frontend/TESTING_FRONTEND.md`.

## Solicitud de negocio

Evitar que un cambio permita cerrar la sesión actual o cerrar sin confirmar.

## Objetivo

Tests de `MisSesionesScreen`.

## Alcance

`tests/MisSesionesScreen.test.tsx`: marca la actual y no le ofrece cierre; nada se cierra sin confirmar; confirmar llama al endpoint correcto y vuelve a pedir el listado; un 404 muestra el aviso y refresca; «Cerrar todas las demás» deshabilitado con una sola sesión; error de carga con «Reintentar». `SesionesAdminScreen.test.tsx` sigue pasando (ajustar solo lo que cambie por el resumen de dispositivo).

**Archivos esperados (crear/tocar):**

- `projects/frontend/src/features/auth/tests/MisSesionesScreen.test.tsx`
- `projects/frontend/src/features/auth/tests/SesionesAdminScreen.test.tsx`

## Seguridad

Cubre CA01, CA02, CA04, CA05 y CA11 del lado web.

## Fuera de alcance (no tocar)

- Código de producción.

## Resultado esperado

Pasan.

## Validación

1. `pnpm test` en verde.
