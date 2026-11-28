# UT-HUI-IDIOMAS-MOB-04 — Control automático en la app

| Campo | Valor |
|-------|-------|
| **Código** | `UT-HUI-IDIOMAS-MOB-04` |
| **Nombre** | Control automático en la app |
| **Historia** | HU-INICIAL-Idiomas-MOB |
| **Depende de** | 03 |

## Antes de implementar

IDIOMAS_MOBILE (control automático).

## Solicitud de negocio

La regla tiene que cumplirse sola.

## Objetivo

Pruebas que rechazan texto fijo, claves rotas y voseo.

## Alcance

- Pruebas: paridad de claves y marcadores; claves usadas que existen; carpetas `language` declaradas en `pubspec.yaml` y `app_texts.dart`; sin literales en los widgets y propiedades visibles; sin voseo.

**Archivos esperados (crear/tocar):**

- `projects/mobile/test/shared/language/archivos_de_idioma_test.dart`

## Seguridad

Sin impacto de seguridad: son pruebas.

## Fuera de alcance (no tocar)

- Un paquete de lint nuevo.

## Resultado esperado

Un texto fijo no llega a `main`.

## Validación

1. Agregar a propósito un `Text` fijo, una clave inexistente y un voseo hace fallar las pruebas.
