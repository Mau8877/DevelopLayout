# UT-HUI-BASE-BE-05 — Test de integración de RLS como app_runtime

| Campo | Valor |
|-------|-------|
| **Código** | `UT-HUI-BASE-BE-05` |
| **Nombre** | Test de integración de RLS como app_runtime |
| **Historia** | HU-INICIAL-Base-BE |
| **Depende de** | UT-HUI-BASE-BE-02, UT-HUI-BASE-BE-04, UT-HUI-BASE-BE-06 |

## Antes de implementar

Leer `backend/TESTING_BACKEND.md` (patrón Testcontainers con `assumeTrue`), `backend/BASE_DE_DATOS_PRIVADA_RLS.md` (sección 6) y `.claude/rules/SEGURIDAD_OWASP.md`.

## Solicitud de negocio

RLS solo protege si alguien comprueba que de verdad filtra.

## Objetivo

Hay un test que prueba el aislamiento por usuario y el fallo cerrado conectándose como `app_runtime`.

## Alcance

- `features/usuarios/RlsAutenticacionTest.java` (Testcontainers Postgres, `@BeforeAll` con `DockerClientFactory...isDockerAvailable()` + `assumeTrue`).
- Aplica las migraciones con el dueño y abre una conexión como `app_runtime`.
- Casos: sin contexto no se ve ninguna fila de `usuario`/`usuario_rol`; con `app.current_user` = A solo se ve A; con `app.login_email` = correo de A solo se ve esa fila; intentar `UPDATE` de la fila de B no afecta filas; `app_runtime` no puede `INSERT` en `rol`.
- Casos de `sesion`: sin contexto no se ve nada; con `app.current_user` solo las propias; con `app.refresh_hash` solo la fila de ese hash (actual o anterior); con `app.is_admin` se ven todas. Casos de `bitacora`: `INSERT` permitido, `SELECT` solo con `app.is_admin`, `UPDATE`/`DELETE` rechazados.
- Todas las consultas del test usan nombres calificados con `authentication.`; un caso confirma que `app_runtime` no puede crear objetos en `authentication` ni en `public`.
- Caso extra: insertar una `sesion` con `usuario_codigo` de otro usuario falla (FK compuesta).

**Archivos esperados (crear/tocar):**

- `projects/backend/src/test/java/com/example/backend/features/usuarios/RlsAutenticacionTest.java`

## Fuera de alcance (no tocar)

- Cambiar políticas o migraciones (si el test falla, se corrige en la UT que corresponde).
- Tests de login u otros flujos.

## Resultado esperado

El test pasa en verde con Docker, o queda `SKIPPED` sin Docker.

## Validación

1. `./mvnw test` ejecuta `RlsAutenticacionTest`: todos los casos pasan.
2. Quitar a propósito `FORCE ROW LEVEL SECURITY` en una prueba local: el test falla (comprobación de que el test detecta el problema).

## Seguridad

A01: evidencia automatizada de que RLS falla cerrado.
