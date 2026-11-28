# UT-HUI-IDIOMAS-FE-04 — Migrar autenticación y validaciones

| Campo | Valor |
|-------|-------|
| **Código** | `UT-HUI-IDIOMAS-FE-04` |
| **Nombre** | Migrar autenticación y validaciones |
| **Historia** | HU-INICIAL-Idiomas-FE |
| **Depende de** | 03 |

## Antes de implementar

IDIOMAS_FRONTEND y FORMULARIOS_ZOD_TANSTACK_FRONTEND.

## Solicitud de negocio

Las pantallas de acceso son lo primero que ve cualquiera.

## Objetivo

`features/auth` sin texto fijo y con validaciones por clave.

## Alcance

- `features/auth/language/{es,en}.json`.
- Mensajes de Zod como claves, traducidos en un único punto al mostrarse (`primerError` o el campo); un error visible cambia de idioma sin revalidar.
- Actualizar las pruebas al español neutro.

**Archivos esperados (crear/tocar):**

- `projects/frontend/src/features/auth/`
- `projects/frontend/src/utils/primerError.ts`
- `projects/frontend/src/i18next.d.ts`

## Seguridad

Los errores del servidor se muestran tal cual llegan (sin reinterpretar códigos que revelen cuentas).

## Fuera de alcance (no tocar)

- Otras features.

## Resultado esperado

El acceso se ve completo en ambos idiomas.

## Validación

1. Un error de validación visible cambia de idioma con el selector.
2. Acceso denegado: credenciales incorrectas muestran el mensaje del servidor en ambos idiomas.
3. Pruebas de `features/auth` en verde.
