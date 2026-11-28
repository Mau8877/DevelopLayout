# Unidades de trabajo — HU-INICIAL-CambiarPassword-MOB

**Historia:** Cambiar la contraseña desde la app y volver a iniciar sesión.
**Fuente HU:** [`HU-INICIAL-CambiarPassword.md`](../../../HistoriasDeUsuario/HU-INICIAL-CambiarPassword.md)
**Recurso HTTP / contrato:** Pantalla `/change-password`

## Antes de implementar cualquier UT

Leer las rules de [`mobile/`](../../../../.claude/rules/mobile/), [`IDIOMAS.md`](../../../../.claude/rules/IDIOMAS.md) y [`SEGURIDAD_OWASP.md`](../../../../.claude/rules/SEGURIDAD_OWASP.md). Estas fichas **no** sustituyen esas reglas: si hay conflicto, ganan las rules del proyecto.

## Decisiones de diseño

1. **Sin logout al terminar:** el servidor ya cerró las sesiones; la app solo descarta sus tokens.
2. **Acceso desde la barra superior,** como «Mis sesiones», porque la app no tiene menú de usuario.

## Seguridad

Pantalla privada; tokens revocados borrados del almacenamiento seguro; ninguna contraseña en logs.

## Contrato de datos / recurso HTTP

| Método y ruta | Acceso | Body | Éxito |
|---|---|---|---|
| `POST /api/v1/auth/password/change` | Con sesión (cualquier rol); CSRF en web | `{"passwordActual": "...", "passwordNueva": "..."}` | 200, `data: null`. En web borra las dos cookies de sesión |

| Respuesta | Código | Cuándo |
|---|---|---|
| 400 | `ERR_AUTH_12` (nuevo) | La contraseña actual no es correcta |
| 400 | `ERR_AUTH_06` | La nueva es igual a la actual |
| 400 | `ERR_SYS_01` | La nueva no cumple la política, falta un campo o sobra una propiedad |
| 401 | `ERR_AUTH_07` / `ERR_AUTH_04` | Sin sesión válida, o la sesión se cerró mientras se procesaba |
| 403 | — | Petición web sin CSRF válido |
| 423 | `ERR_AUTH_13` (nuevo) | Cambio bloqueado por intentos fallidos |
| 429 | `ERR_SYS_02` | Límite por IP |

## Orden de ejecución

| Código | Nombre | Depende de |
|--------|--------|------------|
| [UT-HUI-CAMBIARPASSWORD-MOB-01](./UT-HUI-CAMBIARPASSWORD-MOB-01.md) | Servicio y sesión | BE-05 |
| [UT-HUI-CAMBIARPASSWORD-MOB-02](./UT-HUI-CAMBIARPASSWORD-MOB-02.md) | Pantalla y acceso | 01 |

**Cadena:** BE-05 → 01 → 02

## Criterios de aceptación cubiertos

| CA | Descripción | UTs |
|----|-------------|-----|
| CA01 | Cambio correcto | 01, 02 |
| CA02 | Actual incorrecta | 01, 02 |
| CA03 | Cierra la sesión de la app | 01 |
| CA04 | Política | 02 |
| CA09 | Sin sesión | 02 |
| CA15 | Roles e idiomas | 02 |
| CA16 | Un solo envío y campos vacíos | 02 |

## Follow-ups (no se implementan en estas UT)

- Validación en emulador o teléfono (al final del proyecto).

## Qué NO incluyen estas UT

- Biometría.
- Cambios en otras pantallas.
