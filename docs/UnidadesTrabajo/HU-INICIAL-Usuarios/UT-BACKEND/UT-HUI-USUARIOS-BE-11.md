# UT-HUI-USUARIOS-BE-11 — Prueba de producción y documentación

| Campo | Valor |
|-------|-------|
| **Código** | `UT-HUI-USUARIOS-BE-11` |
| **Nombre** | Prueba de producción y documentación |
| **Historia** | HU-INICIAL-Usuarios-BE |
| **Depende de** | 07, 09, 10 |

## Antes de implementar

CI_CD (prueba de producción) y GUIA_ERRORES_BACKEND.

## Solicitud de negocio

Lo nuevo tiene que comprobarse con el stack real y quedar documentado.

## Objetivo

La prueba de producción cubre el alta con el correo caído y las reglas reflejan el comportamiento.

## Alcance

- `scripts/prod-smoke.mjs`: con el administrador inicial, crear un usuario con el correo caído → 201 con `invitacionEnviada=false`, la aplicación sigue sana, la cuenta aparece en el listado y no puede iniciar sesión; desactivarla.
- SEGURIDAD_AUTH_BACKEND: tabla de estado, sección de administración de usuarios (guardia, contraseña inutilizable, anulación de enlaces), endpoint público nuevo y renombre del servicio de correo.

**Archivos esperados (crear/tocar):**

- `scripts/prod-smoke.mjs`
- `.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md`

## Seguridad

No relaja ninguna protección de la prueba (prefijos de volúmenes y redes, puertos locales).

## Fuera de alcance (no tocar)

- Un SMTP real.
- El estado de la HU, que se actualiza al terminar también la web.

## Resultado esperado

El stack de producción demuestra el alta sin depender del correo.

## Validación

1. `pnpm test:prod` y `node --test "scripts/**/*.test.mjs"` en verde.
2. La comprobación de acceso denegado: la cuenta creada y desactivada no puede iniciar sesión.
