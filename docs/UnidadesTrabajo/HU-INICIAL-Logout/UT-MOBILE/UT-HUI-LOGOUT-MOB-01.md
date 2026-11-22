# UT-HUI-LOGOUT-MOB-01 — logout en AuthService y authProvider

| Campo | Valor |
|-------|-------|
| **Código** | `UT-HUI-LOGOUT-MOB-01` |
| **Nombre** | logout en AuthService y authProvider |
| **Historia** | HU-INICIAL-Logout-MOB |
| **Depende de** | — |

## Antes de implementar

Leer `mobile/ESTADO_GLOBAL_MOBILE.md`, `mobile/CONSUMO_API_MOBILE.md` y `.claude/rules/SEGURIDAD_OWASP.md`.

## Solicitud de negocio

Al salir hay que borrar los tokens del dispositivo.

## Objetivo

`authProvider.logout()` llama a la API y borra el almacenamiento seguro.

## Alcance

- Editar `AuthService` agregando `logout()` (envía el refresh en el body; tolera errores de red) y `authProvider` agregando `logout()` que siempre borra `SecureTokenStorage` y deja el estado sin sesión.

**Archivos esperados (crear/tocar):**

- `projects/mobile/lib/features/auth/services/auth_service.dart`
- `projects/mobile/lib/features/auth/providers/auth_provider.dart`

## Fuera de alcance (no tocar)

- Botón en la interfaz (UT 02).

## Resultado esperado

Tras `logout()` no quedan tokens guardados.

## Validación

1. Test con Dio falso: logout con y sin red deja el storage vacío.

## Seguridad

MASVS-STORAGE: borrado seguro de credenciales.
