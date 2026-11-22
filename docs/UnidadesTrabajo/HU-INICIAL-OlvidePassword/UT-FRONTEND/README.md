# Unidades de trabajo — HU-INICIAL-OlvidePassword-FE

**Historia:** Pantallas para pedir y completar el restablecimiento.
**Fuente HU:** [`HU-INICIAL-OlvidePassword.md`](../../../HistoriasDeUsuario/HU-INICIAL-OlvidePassword.md)
**Recurso HTTP / contrato:** Rutas públicas `/forgot-password` y `/reset-password`.

## Antes de implementar cualquier UT

Leer las rules de [`frontend/`](../../../../.claude/rules/frontend/) y [`SEGURIDAD_OWASP.md`](../../../../.claude/rules/SEGURIDAD_OWASP.md). Estas fichas **no** sustituyen esas reglas: si hay conflicto, ganan las rules del proyecto.

## Orden de ejecución

| Código | Nombre | Depende de |
|--------|--------|------------|
| [UT-HUI-OLVIDEPASSWORD-FE-01](./UT-HUI-OLVIDEPASSWORD-FE-01.md) | Schemas y hooks de recuperación | — |
| [UT-HUI-OLVIDEPASSWORD-FE-02](./UT-HUI-OLVIDEPASSWORD-FE-02.md) | Pantalla Olvidé mi contraseña | UT-HUI-OLVIDEPASSWORD-FE-01 |
| [UT-HUI-OLVIDEPASSWORD-FE-03](./UT-HUI-OLVIDEPASSWORD-FE-03.md) | Pantalla Restablecer contraseña con token del fragmento | UT-HUI-OLVIDEPASSWORD-FE-01 |
| [UT-HUI-OLVIDEPASSWORD-FE-04](./UT-HUI-OLVIDEPASSWORD-FE-04.md) | Tests de recuperación web | UT-HUI-OLVIDEPASSWORD-FE-03 |

**Cadena:** 01 → 02 → 03 → 04

## Criterios de aceptación cubiertos

| CA (HU-INICIAL-OlvidePassword) | Descripción | UTs |
|---|---|---|
| CA01 | Solicitud enviada | 01, 02 |
| CA03 | Restablecer | 01, 03 |
| CA04 | Token inválido | 03 |
| CA05 | Contraseña inválida | 01, 03, 04 |
| CA08 | Token fuera de la URL y Referrer-Policy | 03, 04 |

## Follow-ups (no se implementan en estas UT)

- Ninguno por ahora.

## Qué NO incluyen estas UT

- Cambio de contraseña con sesión iniciada.
