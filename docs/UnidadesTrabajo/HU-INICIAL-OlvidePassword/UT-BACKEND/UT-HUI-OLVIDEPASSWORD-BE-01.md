# UT-HUI-OLVIDEPASSWORD-BE-01 — Mailpit en el compose de desarrollo

| Campo | Valor |
|-------|-------|
| **Código** | `UT-HUI-OLVIDEPASSWORD-BE-01` |
| **Nombre** | Mailpit en el compose de desarrollo |
| **Historia** | HU-INICIAL-OlvidePassword-BE |
| **Depende de** | — |

## Antes de implementar

Leer `docs/INFRAESTRUCTURA.md`, `SEGURIDAD_OWASP.md` (Infraestructura) y `.claude/rules/SEGURIDAD_OWASP.md`.

## Solicitud de negocio

Sin bandeja de correo local no se puede probar la recuperación en desarrollo.

## Objetivo

El compose de desarrollo incluye Mailpit solo en `127.0.0.1`.

## Alcance

- Servicio `mailpit` (imagen con tag fijo) en `docker-compose.yml`: SMTP 1025 y UI 8025 publicados solo en `127.0.0.1`; el backend lo alcanza por la red interna.
- Documentarlo en `docs/INFRAESTRUCTURA.md` y `docs/README.md`.
- No va en `docker-compose.prod.yml` (producción usa un SMTP real).

**Archivos esperados (crear/tocar):**

- `docker-compose.yml`
- `docs/INFRAESTRUCTURA.md`
- `docs/README.md`

## Fuera de alcance (no tocar)

- Código del backend.
- Configuración SMTP de producción.

## Resultado esperado

Con `docker compose up`, la UI de Mailpit abre en `http://127.0.0.1:8025`.

## Validación

1. `docker compose config --quiet` sin errores.
2. Abrir la UI de Mailpit; desde fuera del equipo el puerto no responde.

## Seguridad

A02: sin puertos expuestos a la red, imagen con tag fijo.
