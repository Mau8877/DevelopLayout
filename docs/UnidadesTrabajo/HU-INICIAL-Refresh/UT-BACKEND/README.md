# Unidades de trabajo — HU-INICIAL-Refresh-BE

**Historia:** Renovar la sesión con rotación del refresh token.
**Fuente HU:** [`HU-INICIAL-Refresh.md`](../../../HistoriasDeUsuario/HU-INICIAL-Refresh.md)
**Recurso HTTP / contrato:** `POST /api/v1/auth/refresh`.

## Antes de implementar cualquier UT

Leer las rules de [`backend/`](../../../../.claude/rules/backend/) y [`SEGURIDAD_OWASP.md`](../../../../.claude/rules/SEGURIDAD_OWASP.md). Estas fichas **no** sustituyen esas reglas: si hay conflicto, ganan las rules del proyecto.

## Decisiones de diseño

- `refresh_hash_anterior` en la misma fila de `sesion` permite distinguir un token reutilizado de uno inexistente, sin guardar nada en Redis.
- Redis solo guarda la lista de sesiones revocadas (TTL = vida del access token).

## Orden de ejecución

| Código | Nombre | Depende de |
|--------|--------|------------|
| [UT-HUI-REFRESH-BE-01](./UT-HUI-REFRESH-BE-01.md) | SesionService.rotar, revocación y detección de reutilización | Login: SesionService.crear (HU-INICIAL-Login) |
| [UT-HUI-REFRESH-BE-02](./UT-HUI-REFRESH-BE-02.md) | AuthService.refresh y endpoint POST /refresh | UT-HUI-REFRESH-BE-01 |
| [UT-HUI-REFRESH-BE-03](./UT-HUI-REFRESH-BE-03.md) | Tests de renovación | UT-HUI-REFRESH-BE-02 |

**Cadena:** 01 → 02 → 03

## Criterios de aceptación cubiertos

| CA (HU-INICIAL-Refresh) | Descripción | UTs |
|---|---|---|
| CA01 | Rotación | 01, 02, 03 |
| CA02 | Token inválido | 01, 03 |
| CA03 | Reutilización | 01, 03 |
| CA05 | Usuario desactivado | 02, 03 |
| CA06 | Redis caído | 03 |
| CA07 | CSRF | 02, 03 |
| CA08 | Vida máxima de la sesión | 01, 03 |

## Follow-ups (no se implementan en estas UT)

- Ninguno por ahora.

## Qué NO incluyen estas UT

- Logout y recuperación.
