# UT-HUI-CAMBIARPASSWORD-MOB-01 — Servicio y cierre de la sesión local

| Campo | Valor |
|-------|-------|
| **Código** | `UT-HUI-CAMBIARPASSWORD-MOB-01` |
| **Nombre** | Servicio y cierre de la sesión local |
| **Historia** | HU-INICIAL-CambiarPassword-MOB |
| **Depende de** | BE-05 |

## Antes de implementar

CONSUMO_API_MOBILE, ESTADO_GLOBAL_MOBILE y ERRORES_LOGGING_MOBILE.

## Solicitud de negocio

La app necesita pedir el cambio y quedar sin sesión cuando se completa.

## Objetivo

Servicio y método del provider de sesión.

## Alcance

- `AuthService.cambiarPassword(actual, nueva)`: `POST /auth/password/change` con el `ApiClient` compartido.
- `AuthNotifier.cambiarPassword(actual, nueva)`: llama al servicio y, **solo si tuvo éxito**, descarta los tokens y deja la sesión en `null`, así el router lleva al login. Ante un error lo relanza sin tocar la sesión. No llama a logout: el servidor ya cerró las sesiones.

**Archivos esperados (crear/tocar):**

- `projects/mobile/lib/features/auth/services/auth_service.dart`
- `projects/mobile/lib/features/auth/providers/auth_provider.dart`
- `projects/mobile/test/features/auth/cambiar_password_test.dart`
- `projects/mobile/test/features/auth/fake_auth_service.dart`

## Seguridad

MASVS-STORAGE: los tokens revocados se borran del almacenamiento seguro y las contraseñas no se guardan ni se registran.

## Fuera de alcance (no tocar)

- La pantalla.
- El interceptor de renovación.

## Resultado esperado

La lógica queda probada sin interfaz.

## Validación

1. Éxito: se envían solo las dos contraseñas, se borran los tokens y la sesión queda en `null`.
2. Errores 400, 423, 429 y sin red: tokens y sesión se conservan.
3. Con un access token vencido, renueva una vez y completa el cambio.
