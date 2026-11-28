# UT-HUI-PRUEBASNAVEGADOR-FE-04 — Flujos de idioma

| Campo | Valor |
|-------|-------|
| **Código** | `UT-HUI-PRUEBASNAVEGADOR-FE-04` |
| **Nombre** | Flujos de idioma |
| **Historia** | HU-INICIAL-PruebasNavegador-FE |
| **Depende de** | 01 |

## Antes de implementar

IDIOMAS.md e IDIOMAS_FRONTEND.

## Solicitud de negocio

El idioma cruza navegador, servidor y correo.

## Objetivo

La selección de idioma comprobada de punta a punta.

## Alcance

- Navegador en inglés, en español y en un tercer idioma.
- Selector antes y después de entrar, y que se recuerde al recargar.
- Un error del servidor y un correo en el idioma elegido; una fecha cambia de formato.

**Archivos esperados (crear/tocar):**

- `projects/frontend/e2e/idiomas.spec.ts`

## Seguridad

Sin impacto de seguridad: comprueba presentación e idioma.

## Fuera de alcance (no tocar)

- Comparar capturas de pantalla.

## Resultado esperado

El comportamiento de idioma está protegido.

## Validación

1. Las pruebas pasan con el navegador configurado en cada idioma.
