# Unidades de trabajo — HU-INICIAL-OlvidePassword-MOB

**Historia:** Pedir el enlace desde la app (el restablecimiento se completa en la web).
**Fuente HU:** [`HU-INICIAL-OlvidePassword.md`](../../../HistoriasDeUsuario/HU-INICIAL-OlvidePassword.md)
**Recurso HTTP / contrato:** Pantalla `/forgot-password`.

## Antes de implementar cualquier UT

Leer las rules de [`mobile/`](../../../../.claude/rules/mobile/) y [`SEGURIDAD_OWASP.md`](../../../../.claude/rules/SEGURIDAD_OWASP.md). Estas fichas **no** sustituyen esas reglas: si hay conflicto, ganan las rules del proyecto.

## Decisiones de diseño

- Sin deep links: el enlace se abre en el navegador, que tiene la pantalla de restablecer.

## Orden de ejecución

| Código | Nombre | Depende de |
|--------|--------|------------|
| [UT-HUI-OLVIDEPASSWORD-MOB-01](./UT-HUI-OLVIDEPASSWORD-MOB-01.md) | AuthService.solicitarRecuperacion | — |
| [UT-HUI-OLVIDEPASSWORD-MOB-02](./UT-HUI-OLVIDEPASSWORD-MOB-02.md) | Pantalla Olvidé mi contraseña | UT-HUI-OLVIDEPASSWORD-MOB-01 |
| [UT-HUI-OLVIDEPASSWORD-MOB-03](./UT-HUI-OLVIDEPASSWORD-MOB-03.md) | Tests de recuperación mobile | UT-HUI-OLVIDEPASSWORD-MOB-02 |

**Cadena:** 01 → 02 → 03

## Criterios de aceptación cubiertos

| CA (HU-INICIAL-OlvidePassword) | Descripción | UTs |
|---|---|---|
| CA01 | Solicitud enviada | 01, 02, 03 |
| CA06 | Límite 429 | 01, 03 |

## Follow-ups (no se implementan en estas UT)

- Deep links (App Links/Universal Links) cuando exista dominio propio.

## Qué NO incluyen estas UT

- Pantalla de restablecer en mobile.
