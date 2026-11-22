# UT-HUI-LOGIN-MOB-01 — Convención de modelos y almacenamiento seguro de tokens

| Campo | Valor |
|-------|-------|
| **Código** | `UT-HUI-LOGIN-MOB-01` |
| **Nombre** | Convención de modelos y almacenamiento seguro de tokens |
| **Historia** | HU-INICIAL-Login-MOB |
| **Depende de** | — |

## Antes de implementar

Leer `mobile/CONSUMO_API_MOBILE.md`, `ARQUITECTURA_MOBILE.md`, `SEGURIDAD_OWASP.md` (MASVS-STORAGE) y `.claude/rules/SEGURIDAD_OWASP.md`.

## Solicitud de negocio

Falta definir dónde viven los DTOs Dart y dónde se guardan los tokens sin violar la regla de dependencia.

## Objetivo

Quedan definidos `models/` por feature y `core/storage/` con almacenamiento seguro.

## Alcance

- Actualizar `ARQUITECTURA_MOBILE.md` y `CONSUMO_API_MOBILE.md`: los DTOs viven en `lib/features/{feature}/models/` y hay una carpeta nueva `lib/core/storage/`.
- `core/storage/secure_token_storage.dart`: `guardar`, `leerAccess`, `leerRefresh`, `borrar` sobre `flutter_secure_storage`, nunca `shared_preferences`.
- Sin logs de valores.

**Archivos esperados (crear/tocar):**

- `.claude/rules/ARQUITECTURA_MOBILE.md`
- `.claude/rules/mobile/CONSUMO_API_MOBILE.md`
- `projects/mobile/lib/core/storage/secure_token_storage.dart`
- `projects/mobile/pubspec.yaml`

## Fuera de alcance (no tocar)

- Interceptors de red (UT 02).
- Modelos concretos de auth (UT 03).

## Resultado esperado

Existe `SecureTokenStorage` y las reglas reflejan la convención.

## Validación

1. `flutter analyze` sin errores.
2. Test con un backend de storage falso: guardar, leer, borrar.

## Seguridad

MASVS-STORAGE: tokens solo en almacenamiento seguro del sistema.
