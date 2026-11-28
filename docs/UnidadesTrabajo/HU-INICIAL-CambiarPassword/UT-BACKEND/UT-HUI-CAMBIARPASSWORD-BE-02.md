# UT-HUI-CAMBIARPASSWORD-BE-02 — Límite de intentos del cambio de contraseña

| Campo | Valor |
|-------|-------|
| **Código** | `UT-HUI-CAMBIARPASSWORD-BE-02` |
| **Nombre** | Límite de intentos del cambio de contraseña |
| **Historia** | HU-INICIAL-CambiarPassword-BE |
| **Depende de** | — |

## Antes de implementar

SEGURIDAD_AUTH_BACKEND (límite de intentos de login) y TESTING_BACKEND.

## Solicitud de negocio

Una sesión robada no puede servir para adivinar la contraseña actual.

## Objetivo

Un contador por cuenta, propio de este flujo, que bloquea tras 5 fallos.

## Alcance

- `CambioPasswordIntentosService` (Redis), con clave propia por cuenta (hash del `codigo`), separada de los contadores de login.
- `reservar(usuario)`: **cuenta el intento antes** de comprobar la contraseña, en un script Lua atómico; devuelve si se permite. Con el cupo agotado no suma ni alarga el bloqueo.
- Cada reserva renueva el vencimiento; así, tras el quinto fallo, el bloqueo dura 15 minutos.
- `reiniciar(usuario)`: borra el contador.
- Configuración `app.auth.password-change-max-attempts=5` y `app.auth.password-change-lock=PT15M`; un máximo menor a 1 impide arrancar.
- Si Redis falla, el error se propaga (falla cerrado).

**Archivos esperados (crear/tocar):**

- `projects/backend/src/main/java/com/example/backend/features/auth/service/CambioPasswordIntentosService.java`
- `projects/backend/src/test/java/com/example/backend/features/auth/CambioPasswordIntentosServiceTest.java`
- `projects/backend/src/main/resources/application.properties`

## Seguridad

A07 y API4: reservar antes de comparar impide que peticiones simultáneas prueben más contraseñas que el límite (CA06).

## Fuera de alcance (no tocar)

- `LoginAttemptService` y sus contadores.
- Cerrar sesiones al llegar al límite.

## Resultado esperado

El flujo puede saber, de forma atómica, si todavía admite un intento.

## Validación

1. Cinco reservas se permiten y la sexta no; `reiniciar` vuelve a cero.
2. 40 reservas simultáneas permiten exactamente 5.
3. Reservar con el cupo agotado no alarga el vencimiento.
4. El contador de una cuenta no afecta a otra ni a los de login.
