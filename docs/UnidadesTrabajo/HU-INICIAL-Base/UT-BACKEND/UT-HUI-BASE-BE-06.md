# UT-HUI-BASE-BE-06 — BitacoraService: punto único para registrar eventos

| Campo | Valor |
|-------|-------|
| **Código** | `UT-HUI-BASE-BE-06` |
| **Nombre** | BitacoraService: punto único para registrar eventos |
| **Historia** | HU-INICIAL-Base-BE |
| **Depende de** | UT-HUI-BASE-BE-03 |

## Antes de implementar

Leer `backend/BITACORA_BACKEND.md`, `backend/LOGGING_BACKEND.md`, `backend/CONVENCIONES_JAVA_BACKEND.md` y `.claude/rules/SEGURIDAD_OWASP.md`.

## Solicitud de negocio

Todo evento de seguridad y todo cambio de negocio debe quedar registrado de forma uniforme y sin datos sensibles.

## Objetivo

Existe `BitacoraService` que inserta eventos sanitizados con actor, IP y user agent.

## Alcance

- `common/bitacora/BitacoraService.java` con `registrar(accion, modulo, entidad, entidadCodigo, resultado, detalle)` (se une a la transacción actual, para cambios de negocio) y `registrarEnTransaccionPropia(...)` (`Propagation.REQUIRES_NEW`, para fallos y eventos de seguridad que no deben perderse si la operación hace rollback).
- Actor: el `sub` del `SecurityContext` si hay sesión, o un `actor` explícito; si no hay, `NULL`. IP y user agent salen de la request actual (`RequestContextHolder`) cuando existe, con el user agent truncado a 255.
- Sanitización de `detalle`: rechaza (`IllegalArgumentException`) claves que contengan `password`, `token`, `secret`, `hash`, `authorization` o `card`; solo se permiten valores simples, `UUID` y listas de nombres de campos.
- Constantes de acciones por módulo (`AuthAcciones` en `features/auth/` se agrega en cada HU que las usa, no aquí).

**Archivos esperados (crear/tocar):**

- `projects/backend/src/main/java/com/example/backend/common/bitacora/BitacoraService.java`

## Fuera de alcance (no tocar)

- Acciones concretas de auth (las define cada HU).
- Consulta y purga (HU-INICIAL-Bitacora).
- Crear la regla `BITACORA_BACKEND.md` (ya existe).

## Resultado esperado

Un servicio llama a `registrar` y queda una fila en `bitacora` con los datos correctos; con rollback de la operación, `registrarEnTransaccionPropia` igual deja el evento.

## Validación

1. Test de integración (Testcontainers, rol `app_runtime`): `registrar` inserta una fila con actor, IP y user agent.
2. Test: `detalle` con la clave `password` lanza excepción y no inserta.
3. Test: operación que hace rollback tras `registrarEnTransaccionPropia` conserva el evento y tras `registrar` no.

## Seguridad

A09: auditoría sin secretos, de solo inserción, sin perder eventos de fallo.
