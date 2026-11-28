# UT-HUI-CAMBIARPASSWORD-BE-01 — Motivo de revocación por cambio de contraseña

| Campo | Valor |
|-------|-------|
| **Código** | `UT-HUI-CAMBIARPASSWORD-BE-01` |
| **Nombre** | Motivo de revocación por cambio de contraseña |
| **Historia** | HU-INICIAL-CambiarPassword-BE |
| **Depende de** | — |

## Antes de implementar

SCHEMAS_BD_BACKEND, PERSISTENCIA_BD_BACKEND y BASE_DE_DATOS_PRIVADA_RLS.

## Solicitud de negocio

Hay que poder distinguir en la base las sesiones cerradas por un cambio de contraseña.

## Objetivo

La base y el backend aceptan el motivo `CAMBIO_PASSWORD`.

## Alcance

- Migración nueva (formato `V{yyyyMMddHHmmss}__...`, posterior a las existentes) que reemplaza la restricción `CHECK` de `authentication.sesion.motivo_revocacion` agregando `CAMBIO_PASSWORD`. Nombres calificados; solo cambia la restricción, no datos.
- `MotivoRevocacion.CAMBIO_PASSWORD`.
- Actualizar el diagrama y la lista de migraciones de la rule de schemas.

**Archivos esperados (crear/tocar):**

- `projects/backend/src/main/resources/db/migration/V20261128090000__motivo_cambio_password.sql`
- `projects/backend/src/main/java/com/example/backend/features/auth/enums/MotivoRevocacion.java`
- `projects/backend/src/test/java/com/example/backend/features/auth/MigracionCambioPasswordTest.java`
- `docs/BASE_DE_DATOS.mermaid`
- `.claude/rules/backend/SCHEMAS_BD_BACKEND.md`

## Seguridad

Sin tablas, columnas ni permisos nuevos: RLS y `GRANT` de `sesion` no cambian (A01).

## Fuera de alcance (no tocar)

- Cambiar migraciones ya aplicadas.
- El servicio y el endpoint.

## Resultado esperado

Una sesión puede quedar `REVOCADA` con el motivo nuevo.

## Validación

1. Prueba sobre Postgres real: se puede guardar `CAMBIO_PASSWORD` y un motivo inventado se rechaza.
2. La migración se aplica sobre una base con sesiones de todos los motivos anteriores.
3. Con el rol `app_runtime`, un usuario sigue sin poder tocar sesiones ajenas.
