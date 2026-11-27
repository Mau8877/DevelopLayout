# UT-HUI-OLVIDEPASSWORD-BE-07 — Correo en producción y prueba de punta a punta

| Campo | Valor |
|-------|-------|
| **Código** | `UT-HUI-OLVIDEPASSWORD-BE-07` |
| **Nombre** | Correo en producción y prueba de punta a punta |
| **Historia** | HU-INICIAL-OlvidePassword-BE |
| **Depende de** | UT-HUI-OLVIDEPASSWORD-BE-05 |

## Antes de implementar

Leer `.claude/rules/CI_CD.md` (Prueba de producción), `docs/INFRAESTRUCTURA.md` y `.claude/rules/SEGURIDAD_OWASP.md`.

## Solicitud de negocio

Producción no puede desplegarse sin poder enviar el enlace, ni caerse porque el correo falle.

## Objetivo

El compose de producción exige la configuración de correo y la prueba de punta a punta la cubre.

## Alcance

- `docker-compose.prod.yml`, backend: `SPRING_MAIL_HOST`, `MAIL_FROM` y `APP_FRONTEND_BASE_URL` obligatorias (`:?`); `SPRING_MAIL_PORT` (587), `SPRING_MAIL_USERNAME`, `SPRING_MAIL_PASSWORD`, `SPRING_MAIL_SMTP_AUTH` (`true`) y `SPRING_MAIL_SMTP_STARTTLS` (`true`).
- `.env.example` de la raíz: esas variables en el bloque «solo producción», **comentadas** y sin `CAMBIAME`.
- `ci.yml`, paso «Validar docker-compose.prod.yml»: valores de relleno para las obligatorias.
- `scripts/prod-smoke.mjs`: SMTP inalcanzable (`smtp.invalid`) y comprobar que el backend queda sano, que la solicitud responde 200 igual para el administrador y para un correo inexistente, y que `/health` sigue `UP` después.
- `docs/INFRAESTRUCTURA.md` y `docs/README.md`: variables, que el servidor SMTP lo elige cada proyecto, que SPF/DKIM son del dominio y que el envío no está garantizado.

**Archivos esperados (crear/tocar):**

- `docker-compose.prod.yml`
- `.env.example`
- `.github/workflows/ci.yml`
- `scripts/prod-smoke.mjs`
- `docs/INFRAESTRUCTURA.md`
- `docs/README.md`

## Seguridad

A02: sin valores por defecto que parezcan funcionar; STARTTLS por defecto; las credenciales SMTP solo en el `.env`.

## Fuera de alcance (no tocar)

- Elegir un proveedor o guardar credenciales reales.
- Comprobar la entrega real (necesita un SMTP del proyecto).
- Mailpit en la prueba de producción.

## Resultado esperado

Sin las variables, el compose de producción falla; con un SMTP caído, la aplicación sigue funcionando.

## Validación

1. `docker compose -f docker-compose.prod.yml config` falla sin `SPRING_MAIL_HOST`.
2. `pnpm test:prod` y `pnpm test:scripts` en verde.
