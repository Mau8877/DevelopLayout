# UT-HUI-SESIONES-MOB-01 — Modelo y servicio de sesiones

| Campo | Valor |
|-------|-------|
| **Código** | `UT-HUI-SESIONES-MOB-01` |
| **Nombre** | Modelo y servicio de sesiones |
| **Historia** | HU-INICIAL-Sesiones-MOB |
| **Depende de** | UT-HUI-SESIONES-BE-02 |

## Antes de implementar

Leer `mobile/CONSUMO_API_MOBILE.md`.

## Solicitud de negocio

La app necesita consultar y cerrar sesiones.

## Objetivo

Existen `MiSesion`, `SesionesService` y `resumirDispositivo`.

## Alcance

- `models/mi_sesion.dart` con `fromJson` (`ip`, `userAgent` y `ultimoUsoEn` pueden faltar) y el resumen de dispositivo, con las mismas reglas que la web.
- `models/pagina_sesiones.dart`: elementos y `meta` (el interceptor entrega `{data, meta}` en listados).
- `services/sesiones_service.dart`: listar por página, cerrar una y cerrar las demás, con el `Dio` de `ApiClient`.

**Archivos esperados (crear/tocar):**

- `projects/mobile/lib/features/auth/models/mi_sesion.dart`
- `projects/mobile/lib/features/auth/models/pagina_sesiones.dart`
- `projects/mobile/lib/features/auth/services/sesiones_service.dart`

## Seguridad

MASVS-PRIVACY: IP y user agent no se loguean ni se guardan en el dispositivo.

## Fuera de alcance (no tocar)

- Pantalla (UT 02).

## Resultado esperado

El servicio habla con los tres endpoints.

## Validación

1. Tests con el adaptador falso de Dio (en la UT 03).
2. `flutter analyze` limpio.
