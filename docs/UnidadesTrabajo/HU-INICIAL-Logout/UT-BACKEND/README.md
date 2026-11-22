# Unidades de trabajo — HU-INICIAL-Logout-BE

**Historia:** Cerrar sesión revocando el refresh token.
**Fuente HU:** [`HU-INICIAL-Logout.md`](../../../HistoriasDeUsuario/HU-INICIAL-Logout.md)
**Recurso HTTP / contrato:** `POST /api/v1/auth/logout`.

## Antes de implementar cualquier UT

Leer las rules de [`backend/`](../../../../.claude/rules/backend/) y [`SEGURIDAD_OWASP.md`](../../../../.claude/rules/SEGURIDAD_OWASP.md). Estas fichas **no** sustituyen esas reglas: si hay conflicto, ganan las rules del proyecto.

## Decisiones de diseño

- `permitAll` en logout para que funcione con el access token vencido; el refresh token y el CSRF son la protección.

## Orden de ejecución

| Código | Nombre | Depende de |
|--------|--------|------------|
| [UT-HUI-LOGOUT-BE-01](./UT-HUI-LOGOUT-BE-01.md) | SesionService.cerrar, AuthService.logout y endpoint POST /logout | Refresh: SesionService.revocar (HU-INICIAL-Refresh) |
| [UT-HUI-LOGOUT-BE-02](./UT-HUI-LOGOUT-BE-02.md) | Tests de logout | UT-HUI-LOGOUT-BE-01 |

**Cadena:** 01 → 02

## Criterios de aceptación cubiertos

| CA (HU-INICIAL-Logout) | Descripción | UTs |
|---|---|---|
| CA01 | Invalidación y borrado de cookies | 01, 02 |
| CA02 | Idempotencia | 01, 02 |
| CA05 | Token anterior inválido | 01, 02 |
| CA06 | CSRF | 01, 02 |
| CA07 | Access token rechazado de inmediato y evento | 01, 02 |

## Follow-ups (no se implementan en estas UT)

- Ninguno por ahora.

## Qué NO incluyen estas UT

- Recuperación de contraseña.
