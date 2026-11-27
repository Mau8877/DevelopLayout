# UT-HUI-SESIONES-MOB-03 — Tests de mis sesiones

| Campo | Valor |
|-------|-------|
| **Código** | `UT-HUI-SESIONES-MOB-03` |
| **Nombre** | Tests de mis sesiones |
| **Historia** | HU-INICIAL-Sesiones-MOB |
| **Depende de** | UT-HUI-SESIONES-MOB-02 |

## Antes de implementar

Leer `mobile/TESTING_MOBILE.md`.

## Solicitud de negocio

Probar el mapeo y que no se cierre nada sin confirmar.

## Objetivo

Tests del modelo, el servicio y el comportamiento de la pantalla.

## Alcance

`test/features/auth/mis_sesiones_test.dart`: `fromJson` con campos ausentes; resumen de dispositivo; rutas y métodos del servicio; la actual sin cierre; nada se cierra sin confirmar; tras confirmar se recarga; la ruta exige sesión (`redirectSegunSesion`).

**Archivos esperados (crear/tocar):**

- `projects/mobile/test/features/auth/mis_sesiones_test.dart`

## Seguridad

Cubre CA01, CA02, CA04, CA05 y CA11 del lado mobile.

## Fuera de alcance (no tocar)

- Código de producción.
- Pruebas en emulador.

## Resultado esperado

Pasan.

## Validación

1. `flutter test` en verde.
