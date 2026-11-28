# UT-HUI-IDIOMAS-FE-06 — Control automático en la web

| Campo | Valor |
|-------|-------|
| **Código** | `UT-HUI-IDIOMAS-FE-06` |
| **Nombre** | Control automático en la web |
| **Historia** | HU-INICIAL-Idiomas-FE |
| **Depende de** | 02, 04, 05 |

## Antes de implementar

IDIOMAS_FRONTEND (control automático) y CI_CD.

## Solicitud de negocio

La regla tiene que cumplirse sola.

## Objetivo

El CI rechaza texto fijo y voseo.

## Alcance

- Dependencia nueva de desarrollo: `eslint-plugin-i18next` (descarga de npm). Regla `no-literal-string` sobre JSX y atributos visibles; si la versión no soporta el ESLint del proyecto, se escribe una regla propia equivalente.
- Prueba para lo que ESLint no ve: avisos con texto fijo, mensajes de Zod que no son claves y voseo en `es.json`.

**Archivos esperados (crear/tocar):**

- `projects/frontend/eslint.config.js`
- `projects/frontend/src/shared/language/sinTextoFijo.test.ts`
- `projects/frontend/package.json`
- `pnpm-lock.yaml`

## Seguridad

A03: dependencia con versión fija en el lockfile y auditada en CI.

## Fuera de alcance (no tocar)

- Desactivar la regla en archivos completos.

## Resultado esperado

Un texto fijo no llega a `main`.

## Validación

1. Agregar a propósito un texto en JSX, en un `aria-label` y en un aviso hace fallar lint o pruebas.
2. `pnpm audit --audit-level moderate` en verde.
