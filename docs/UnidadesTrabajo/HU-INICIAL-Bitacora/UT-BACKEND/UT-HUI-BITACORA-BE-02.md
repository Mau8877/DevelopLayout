# UT-HUI-BITACORA-BE-02 — Purga diaria por retención

| Campo | Valor |
|-------|-------|
| **Código** | `UT-HUI-BITACORA-BE-02` |
| **Nombre** | Purga diaria por retención |
| **Historia** | HU-INICIAL-Bitacora-BE |
| **Depende de** | UT-HUI-BITACORA-BE-01 |

## Antes de implementar

Leer `backend/BITACORA_BACKEND.md`, `backend/LOGGING_BACKEND.md` y `.claude/rules/SEGURIDAD_OWASP.md`.

## Solicitud de negocio

La bitácora no puede crecer sin límite.

## Objetivo

Una tarea diaria elimina los eventos anteriores al período de retención.

## Alcance

- `features/bitacora/service/BitacoraPurgaJob.java` con `@Scheduled(cron = "0 0 3 * * *")` que ejecuta `SELECT authentication.purgar_bitacora(:meses)` (consulta nativa) y registra `INFO` con la cantidad borrada (en los logs, no en la bitácora).
- `@EnableScheduling` en `config/SchedulingConfig.java` y `app.bitacora.retention-months` (12 por defecto).
- Instancia única del backend: sin bloqueo distribuido; se documenta que con más réplicas hay que agregarlo.
- Un fallo se loguea como `ERROR` y no detiene la aplicación. Una retención menor a 1 impide arrancar.
- Migración `V20261126090100__purga_bitacora_dueno_sin_superusuario.sql`: política `SELECT` solo para el dueño y solo durante la purga.

**Archivos esperados (crear/tocar):**

- `projects/backend/src/main/java/com/example/backend/features/bitacora/service/BitacoraPurgaJob.java`
- `projects/backend/src/main/java/com/example/backend/config/SchedulingConfig.java`
- `projects/backend/src/main/resources/application.properties`
- `projects/backend/src/main/resources/db/migration/V20261126090100__purga_bitacora_dueno_sin_superusuario.sql`
- `docs/seguridad/RLS_EXCEPCIONES.md`

## Fuera de alcance (no tocar)

- Cambiar la función SQL (UT-HUI-BASE-BE-01).
- Interfaz.

## Resultado esperado

La purga borra solo lo anterior al período de retención.

## Validación

1. Test de integración: filas de hace 13 y 2 meses; tras la purga con 12 queda solo la reciente; el rol `app_runtime` no puede hacer `DELETE` directo ni leer activando `app.purga`. La purga se prueba además con un dueño **sin** superusuario.

## Seguridad

A09: retención controlada sin dar `DELETE` a la aplicación.
