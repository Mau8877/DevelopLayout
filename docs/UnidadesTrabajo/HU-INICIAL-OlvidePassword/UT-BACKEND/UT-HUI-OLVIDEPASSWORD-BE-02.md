# UT-HUI-OLVIDEPASSWORD-BE-02 — Envío del correo de recuperación

| Campo | Valor |
|-------|-------|
| **Código** | `UT-HUI-OLVIDEPASSWORD-BE-02` |
| **Nombre** | Envío del correo de recuperación |
| **Historia** | HU-INICIAL-OlvidePassword-BE |
| **Depende de** | UT-HUI-OLVIDEPASSWORD-BE-01 |

## Antes de implementar

Leer `backend/CONVENCIONES_JAVA_BACKEND.md`, `backend/LOGGING_BACKEND.md` y `.claude/rules/SEGURIDAD_OWASP.md`.

## Solicitud de negocio

El sistema debe poder enviar el enlace sin que un servidor de correo caído tumbe la aplicación.

## Objetivo

Existe `CorreoRecuperacionService.enviarEnlace(email, token)` con su configuración validada al arrancar.

## Alcance

- `spring-boot-starter-mail` en `pom.xml` (versión del BOM).
- `features/auth/service/CorreoRecuperacionService.java` (vive en `auth`: es su único consumidor). Arma `{app.frontend-base-url}/reset-password#token={token}` y envía **texto plano**, sin contraseña. Un fallo de envío se propaga como excepción; quien la captura y la registra es la UT 04.
- `application.properties`, sin credenciales por defecto: `spring.mail.host=${SPRING_MAIL_HOST:}`, `port` (587), `username`, `password`, autenticación y STARTTLS **activados y obligatorios por defecto**, y timeouts de conexión, lectura y escritura de 5 s (API10).
- `management.health.mail.enabled=false`: por defecto Spring suma el correo a `/health`, y un SMTP caído dejaría el contenedor como no sano. Comprobarlo.
- Validación al arrancar: host, `app.mail.from` y `app.frontend-base-url` presentes; la URL debe ser `https`, salvo `localhost` o `127.0.0.1`. Si falta algo, la aplicación no arranca y el mensaje dice qué variable.
- `application-dev.properties`: Mailpit en `localhost:1025`, sin autenticación ni STARTTLS. `application-test.properties`: host inexistente (`smtp.invalid`); los tests reemplazan `JavaMailSender` por un doble.
- `projects/backend/.env.example`: variables de correo **comentadas** (sin `CAMBIAME`: `setup:env` generaría una contraseña inventada).

**Archivos esperados (crear/tocar):**

- `projects/backend/src/main/java/com/example/backend/features/auth/service/CorreoRecuperacionService.java`
- `projects/backend/pom.xml`
- `projects/backend/src/main/resources/application.properties`
- `projects/backend/src/main/resources/application-dev.properties`
- `projects/backend/src/test/resources/application-test.properties`
- `projects/backend/.env.example`
- `projects/backend/src/test/java/com/example/backend/features/auth/service/CorreoRecuperacionServiceTest.java`

## Seguridad

A02/A04: SMTP por entorno y cifrado por defecto. A09: ni el token ni el correo en los logs. La dirección del enlace sale solo de la configuración, nunca de cabeceras como `Host`.

## Fuera de alcance (no tocar)

- Generar o guardar el token (UT 03).
- Endpoints (UT 04 y 05).
- Compose de producción y `ci.yml` (UT 07): **las UT 02 y 07 van en el mismo PR**, porque desde esta UT producción exige estas variables.

## Resultado esperado

El servicio produce un correo con el enlace correcto y la aplicación no arranca mal configurada.

## Validación

1. Test con `JavaMailSender` falso: destinatario, remitente, enlace en el fragmento y texto plano; el token no aparece en los logs.
2. Test de la validación: sin host, sin remitente o con URL `http` de un dominio, falla; `http://localhost:5173` pasa.
3. Con el SMTP inalcanzable, `/health` responde `UP`.
