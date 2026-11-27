# UT-HUI-USUARIOS-BE-02 — Tokens de invitación y anulación de enlaces

| Campo | Valor |
|-------|-------|
| **Código** | `UT-HUI-USUARIOS-BE-02` |
| **Nombre** | Tokens de invitación y anulación de enlaces |
| **Historia** | HU-INICIAL-Usuarios-BE |
| **Depende de** | — |

## Antes de implementar

SEGURIDAD_AUTH_BACKEND (recuperación) y `PasswordResetTokenService`.

## Solicitud de negocio

La invitación es un enlace de un solo uso que vence a las 24 horas.

## Objetivo

Un almacén de tokens de un solo uso, con una variante para recuperación y otra para invitación, ambas anulables.

## Alcance

- Extraer la lógica de `PasswordResetTokenService` a una clase base `TokensDeUnUso(redis, prefijo, vigencia)` y agregar `anular(UUID usuario)` (atómico: borra el token vigente y su índice).
- `PasswordResetTokenService` conserva prefijo (`auth:reset:`), vigencia y comportamiento.
- Nuevo `InvitacionTokenService` con prefijo `auth:invite:` y `app.auth.invite-ttl=PT24H`.
- Un token de un tipo nunca sirve como el otro.

**Archivos esperados (crear/tocar):**

- `projects/backend/src/main/java/com/example/backend/features/auth/service/TokensDeUnUso.java`
- `projects/backend/src/main/java/com/example/backend/features/auth/service/PasswordResetTokenService.java`
- `projects/backend/src/main/java/com/example/backend/features/auth/service/InvitacionTokenService.java`
- `projects/backend/src/main/resources/application.properties`
- `projects/backend/src/test/java/com/example/backend/features/auth/InvitacionTokenServiceTest.java`

## Seguridad

A07: 32 bytes aleatorios, en Redis solo el hash, uno vigente por usuario, consumo atómico y fallo cerrado si Redis no responde.

## Fuera de alcance (no tocar)

- Endpoints y correo.

## Resultado esperado

Se pueden emitir, consultar, consumir y anular invitaciones.

## Validación

1. Emitir otra invalida la anterior; consumir dos veces solo funciona una; vence según la propiedad.
2. `anular` deja el enlace sin efecto; anular sin token no falla.
3. Acceso denegado: un token de invitación no se acepta como de recuperación, ni al revés.
4. Las pruebas de recuperación existentes siguen en verde.
