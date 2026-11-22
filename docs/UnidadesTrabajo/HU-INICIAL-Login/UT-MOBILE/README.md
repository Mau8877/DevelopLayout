# Unidades de trabajo — HU-INICIAL-Login-MOB

**Historia:** Login, sesión persistente segura y redirect global en la app.
**Fuente HU:** [`HU-INICIAL-Login.md`](../../../HistoriasDeUsuario/HU-INICIAL-Login.md)
**Recurso HTTP / contrato:** Pantallas `/login` y `/`.

## Antes de implementar cualquier UT

Leer las rules de [`mobile/`](../../../../.claude/rules/mobile/) y [`SEGURIDAD_OWASP.md`](../../../../.claude/rules/SEGURIDAD_OWASP.md). Estas fichas **no** sustituyen esas reglas: si hay conflicto, ganan las rules del proyecto.

## Decisiones de diseño

- Los DTOs viven en `features/{feature}/models/` y los tokens en `core/storage/` (UT 01 actualiza las rules).
- Los tokens van en el body y se guardan en `flutter_secure_storage`.

## Orden de ejecución

| Código | Nombre | Depende de |
|--------|--------|------------|
| [UT-HUI-LOGIN-MOB-01](./UT-HUI-LOGIN-MOB-01.md) | Convención de modelos y almacenamiento seguro de tokens | — |
| [UT-HUI-LOGIN-MOB-02](./UT-HUI-LOGIN-MOB-02.md) | ApiClient: header mobile y Bearer | UT-HUI-LOGIN-MOB-01 |
| [UT-HUI-LOGIN-MOB-03](./UT-HUI-LOGIN-MOB-03.md) | Modelos y AuthService (login y me) | UT-HUI-LOGIN-MOB-01, UT-HUI-LOGIN-MOB-02 |
| [UT-HUI-LOGIN-MOB-04](./UT-HUI-LOGIN-MOB-04.md) | authProvider: estado de sesión | UT-HUI-LOGIN-MOB-03 |
| [UT-HUI-LOGIN-MOB-05](./UT-HUI-LOGIN-MOB-05.md) | LoginScreen | UT-HUI-LOGIN-MOB-04 |
| [UT-HUI-LOGIN-MOB-06](./UT-HUI-LOGIN-MOB-06.md) | GoRouter con redirect global y arranque | UT-HUI-LOGIN-MOB-05 |
| [UT-HUI-LOGIN-MOB-07](./UT-HUI-LOGIN-MOB-07.md) | Tests de login mobile | UT-HUI-LOGIN-MOB-06 |

**Cadena:** 01 → 02 → 03 → 04 → 05 → 06 → 07

## Criterios de aceptación cubiertos

| CA (HU-INICIAL-Login) | Descripción | UTs |
|---|---|---|
| CA01 | Login correcto | 03, 04, 05 |
| CA02 | Error genérico | 03, 05 |
| CA05 | Validación | 05 |
| CA06 | Tokens en almacenamiento seguro | 01, 03 |
| CA07 | Redirección | 06 |

## Follow-ups (no se implementan en estas UT)

- Bloqueo biométrico opcional (MASVS-AUTH), si el proyecto lo pide.

## Qué NO incluyen estas UT

- Refresh, logout y recuperación.
