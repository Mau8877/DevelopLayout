# Unidades de trabajo — HU-INICIAL-Login-FE

**Historia:** Pantalla de login, sesión por cookies y guard de rutas.
**Fuente HU:** [`HU-INICIAL-Login.md`](../../../HistoriasDeUsuario/HU-INICIAL-Login.md)
**Recurso HTTP / contrato:** Ruta pública `/login`; rutas `_authenticated/*` protegidas.

## Antes de implementar cualquier UT

Leer las rules de [`frontend/`](../../../../.claude/rules/frontend/) y [`SEGURIDAD_OWASP.md`](../../../../.claude/rules/SEGURIDAD_OWASP.md). Estas fichas **no** sustituyen esas reglas: si hay conflicto, ganan las rules del proyecto.

## Decisiones de diseño

- La sesión se obtiene de `/auth/me` con TanStack Query; no se copia a Zustand ni a ningún storage.

## Orden de ejecución

| Código | Nombre | Depende de |
|--------|--------|------------|
| [UT-HUI-LOGIN-FE-01](./UT-HUI-LOGIN-FE-01.md) | Cliente HTTP con cookies y CSRF | — |
| [UT-HUI-LOGIN-FE-02](./UT-HUI-LOGIN-FE-02.md) | Schema Zod y tipos de login | — |
| [UT-HUI-LOGIN-FE-03](./UT-HUI-LOGIN-FE-03.md) | Hooks de sesión: useLogin y useSession | UT-HUI-LOGIN-FE-01, UT-HUI-LOGIN-FE-02 |
| [UT-HUI-LOGIN-FE-04](./UT-HUI-LOGIN-FE-04.md) | Pantalla de login | UT-HUI-LOGIN-FE-03 |
| [UT-HUI-LOGIN-FE-05](./UT-HUI-LOGIN-FE-05.md) | Guard de rutas autenticadas y redirección por rol | UT-HUI-LOGIN-FE-03, UT-HUI-LOGIN-FE-04 |
| [UT-HUI-LOGIN-FE-06](./UT-HUI-LOGIN-FE-06.md) | Tests de la pantalla y la sesión | UT-HUI-LOGIN-FE-05 |

**Cadena:** 01 → 02 → 03 → 04 → 05 → 06

## Criterios de aceptación cubiertos

| CA (HU-INICIAL-Login) | Descripción | UTs |
|---|---|---|
| CA05 | Errores bajo el campo | 02, 04, 06 |
| CA06 | Sin tokens en storage | 01, 03 |
| CA07 | Redirección por rol | 04, 05 |
| CA08 | Recarga mantiene sesión | 03, 05, 06 |
| CA09 | CSRF | 01 |

## Follow-ups (no se implementan en estas UT)

- Pantallas reales por rol (hoy `panel.tsx` es un placeholder).

## Qué NO incluyen estas UT

- Refresh, logout y recuperación.
