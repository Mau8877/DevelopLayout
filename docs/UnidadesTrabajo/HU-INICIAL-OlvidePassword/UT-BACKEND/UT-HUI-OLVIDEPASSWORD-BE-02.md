# UT-HUI-OLVIDEPASSWORD-BE-02 — MailService y plantilla del correo de recuperación

| Campo | Valor |
|-------|-------|
| **Código** | `UT-HUI-OLVIDEPASSWORD-BE-02` |
| **Nombre** | MailService y plantilla del correo de recuperación |
| **Historia** | HU-INICIAL-OlvidePassword-BE |
| **Depende de** | UT-HUI-OLVIDEPASSWORD-BE-01 |

## Antes de implementar

Leer `backend/CONVENCIONES_JAVA_BACKEND.md`, `backend/LOGGING_BACKEND.md` y `.claude/rules/SEGURIDAD_OWASP.md`.

## Solicitud de negocio

El sistema debe poder enviar correos de forma segura y desacoplada.

## Objetivo

Existe `MailService` que envía el correo de restablecimiento.

## Alcance

- `spring-boot-starter-mail` en `pom.xml`; propiedades `spring.mail.*` sin credenciales por defecto (dev: host Mailpit en `application-dev.properties`); `app.mail.from` y `app.frontend-base-url` (para armar el enlace).
- `common/mail/MailService.java` con `enviarRestablecerPassword(String email, String token)`: el enlace es `{frontend-base-url}/reset-password#token=...` (el token va en el fragmento, que el navegador no envía al servidor ni en `Referer`), texto plano, sin contraseña.
- Errores de envío: log `ERROR` sin el token y excepción de infraestructura (la solicitud ya responde de forma uniforme; ver UT 04).

**Archivos esperados (crear/tocar):**

- `projects/backend/src/main/java/com/example/backend/common/mail/MailService.java`
- `projects/backend/pom.xml`
- `projects/backend/src/main/resources/application.properties`
- `projects/backend/src/main/resources/application-dev.properties`
- `.env.example`
- `projects/backend/.env.example`

## Fuera de alcance (no tocar)

- Generar o guardar el token (UT 03).
- Endpoints.

## Resultado esperado

Con Mailpit, `enviarRestablecerPassword` deja un correo con el enlace.

## Validación

1. Test de integración con Mailpit (o `GreenMail`) verificando destinatario y enlace; el token no aparece en los logs.

## Seguridad

A02/A09: SMTP configurable por entorno, token nunca en logs.
