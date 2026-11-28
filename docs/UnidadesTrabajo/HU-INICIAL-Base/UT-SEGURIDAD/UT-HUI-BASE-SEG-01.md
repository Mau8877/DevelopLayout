# UT-HUI-BASE-SEG-01 — Auditoría OWASP de la plantilla

| Campo | Valor |
|-------|-------|
| **Código** | `UT-HUI-BASE-SEG-01` |
| **Nombre** | Auditoría OWASP de la plantilla |
| **Historia** | HU-INICIAL-Base-SEG |
| **Depende de** | — |

## Antes de implementar

[SEGURIDAD_OWASP.md](../../../../.claude/rules/SEGURIDAD_OWASP.md).

## Solicitud de negocio

Todo proyecto que nazca de la plantilla hereda sus fallos: hay que encontrarlos
antes y dejar escrito qué se comprobó y qué no.

## Objetivo

La plantilla auditada frente a los tres estándares, con las fugas corregidas,
cada una con una prueba, y los pendientes anotados.

## Alcance

Revisión de backend, web, mobile, scripts, CI e infraestructura; corrección de
los hallazgos H-01 a H-11 y de las variables de seguridad que Compose no pasaba
al backend.

Archivos esperados (crear/tocar): el informe
[AUDITORIA_OWASP.md](../../../seguridad/AUDITORIA_OWASP.md), el código y las
pruebas que cita, las reglas afectadas, los dos `docker-compose*.yml` y los
`nginx*.conf`.

## Seguridad

Cubre A01 a A10, API1 a API10 y los grupos de MASVS (ver el informe).

## Fuera de alcance (no tocar)

Doble factor, reglas de alerta, firma de Android, protección de capturas,
verificación de la descarga de Maven y cualquier función nueva.

## Resultado esperado

Los hallazgos corregidos y vigilados por pruebas, y un informe que distingue lo
comprobado de lo pendiente.

## Validación

1. `./mvnw test`: pasan, entre otras, `EvasionHttpTest`, `SecretosEnLogsTest`,
   `RecordsConDatosSensiblesTest`, `DatosEnErroresInesperadosTest`,
   `EventosDeSeguridadEnLogsTest`, `EsperasLimitadasTest` y `PasswordComunTest`
   (incluyen accesos denegados).
2. `pnpm test:scripts`, `pnpm test:e2e` y `pnpm test:prod`.
3. `pnpm audit --audit-level moderate`.

## Resultado de la verificación

La tabla por control, con su estado y evidencia, y la lista de hallazgos están en
el [informe](../../../seguridad/AUDITORIA_OWASP.md).

**Sin verificar:** dispositivo y servidor reales, escaneo dinámico, auditoría
local de dependencias Java y Flutter y `gitleaks` en local.
