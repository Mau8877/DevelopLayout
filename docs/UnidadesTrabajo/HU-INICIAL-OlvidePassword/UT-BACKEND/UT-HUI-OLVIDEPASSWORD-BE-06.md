# UT-HUI-OLVIDEPASSWORD-BE-06 — Tests de recuperación de contraseña

| Campo | Valor |
|-------|-------|
| **Código** | `UT-HUI-OLVIDEPASSWORD-BE-06` |
| **Nombre** | Tests de recuperación de contraseña |
| **Historia** | HU-INICIAL-OlvidePassword-BE |
| **Depende de** | UT-HUI-OLVIDEPASSWORD-BE-05 |

## Antes de implementar

Leer `backend/TESTING_BACKEND.md` y `.claude/rules/SEGURIDAD_OWASP.md`.

## Solicitud de negocio

Probar la uniformidad, el un solo uso, las carreras y la revocación.

## Objetivo

Tests de integración que cubren CA01–CA07, CA09–CA16 y CA18.

## Alcance

`AuthPasswordResetTest` sobre `PostgresRuntimeIntegrationTest` (Postgres y Redis reales, app como `app_runtime`), con `JavaMailSender` reemplazado por un doble que captura el enlace. Verifica el sobre `ApiResponse`.

- Lo asíncrono se espera con Awaitility (condición, no `sleep`).
- **CA09 sin cronometrar:** con el envío detenido en una barrera, la respuesta ya volvió; y en el hilo de la petición no se consulta la cuenta.
- **CA13 determinista:** un espía del `PasswordEncoder` ejecuta el restablecimiento justo después de que el login compare la contraseña anterior; el login debe fallar y no quedar sesión activa. Además, un caso concurrente real.
- **CA18:** dos restablecimientos simultáneos con el mismo enlace: un 200 y un `ERR_AUTH_05`.
- **CA15:** el doble lanza al enviar: 200, `/health` `UP`, bitácora `FALLO`.
- CA10/CA11: eventos presentes, con IP y user agent, sin token, contraseña ni correo en `detalle` ni en logs.
- Pool de conexiones pequeño (como `AuthPoolTest`): varias solicitudes y restablecimientos a la vez no lo agotan.

**Archivos esperados (crear/tocar):**

- `projects/backend/src/test/java/com/example/backend/features/auth/AuthPasswordResetTest.java`

## Seguridad

Evidencia de A01, A07, API4, API6 y A09.

## Fuera de alcance (no tocar)

- Código de producción (si un test descubre un defecto, se corrige en la UT que lo introdujo).
- CA08 (web) y CA17 (cubierto en las UT 02 y 07).

## Resultado esperado

Los tests pasan de forma repetible.

## Validación

1. `./mvnw test` en verde, varias veces seguidas.
2. Quitar la comprobación del hash en el login hace fallar el test de CA13.
