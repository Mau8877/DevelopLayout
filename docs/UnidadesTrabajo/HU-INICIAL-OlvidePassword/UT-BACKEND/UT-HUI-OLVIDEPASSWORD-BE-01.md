# UT-HUI-OLVIDEPASSWORD-BE-01 — Mailpit en el compose de desarrollo

| Campo | Valor |
|-------|-------|
| **Código** | `UT-HUI-OLVIDEPASSWORD-BE-01` |
| **Nombre** | Mailpit en el compose de desarrollo |
| **Historia** | HU-INICIAL-OlvidePassword-BE |
| **Depende de** | — |

## Antes de implementar

Leer `docs/INFRAESTRUCTURA.md` y `.claude/rules/SEGURIDAD_OWASP.md` (Infraestructura).

## Solicitud de negocio

Sin una bandeja local no se puede probar la recuperación en desarrollo.

## Objetivo

El compose de desarrollo incluye Mailpit y el backend le envía los correos.

## Alcance

- Servicio `mailpit` (`container_name: app-mailpit`) en `docker-compose.yml`, con tag fijo: SMTP `1025` y UI `8025` publicados solo en `127.0.0.1`, `cap_drop: ALL` y `no-new-privileges`.
- En el `environment` del backend de desarrollo: `SPRING_MAIL_HOST=app-mailpit`, `SPRING_MAIL_PORT=1025`, `SPRING_MAIL_SMTP_AUTH=false`, `SPRING_MAIL_SMTP_STARTTLS=false`, `MAIL_FROM=no-reply@localhost` y `APP_FRONTEND_BASE_URL=http://localhost:5173`.
- Descargar la imagen requiere permiso: se indica nombre, tag, origen y tamaño antes de hacerlo.
- Documentarlo en `docs/INFRAESTRUCTURA.md` y en el checklist de `docs/README.md` (nombre del contenedor).

**Archivos esperados (crear/tocar):**

- `docker-compose.yml`
- `docs/INFRAESTRUCTURA.md`
- `docs/README.md`

## Seguridad

A02: puertos solo en `127.0.0.1`, imagen con tag fijo, contenedor sin capabilities. Mailpit no va en producción.

## Fuera de alcance (no tocar)

- Código del backend.
- `docker-compose.prod.yml` (UT 07).

## Resultado esperado

Con `docker compose up`, la UI de Mailpit abre en `http://127.0.0.1:8025`.

## Validación

1. `docker compose config --quiet` sin errores y `pnpm test:scripts` en verde.
2. La UI abre desde el equipo; los puertos figuran publicados solo en `127.0.0.1`.
