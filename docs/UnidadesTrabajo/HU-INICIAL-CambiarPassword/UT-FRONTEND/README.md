# Unidades de trabajo — HU-INICIAL-CambiarPassword-FE

**Historia:** Cambiar la contraseña desde el menú y volver a iniciar sesión.
**Fuente HU:** [`HU-INICIAL-CambiarPassword.md`](../../../HistoriasDeUsuario/HU-INICIAL-CambiarPassword.md)
**Recurso HTTP / contrato:** Pantalla `/change-password`

## Antes de implementar cualquier UT

Leer las rules de [`frontend/`](../../../../.claude/rules/frontend/), [`IDIOMAS.md`](../../../../.claude/rules/IDIOMAS.md) y [`SEGURIDAD_OWASP.md`](../../../../.claude/rules/SEGURIDAD_OWASP.md). Estas fichas **no** sustituyen esas reglas: si hay conflicto, ganan las rules del proyecto.

## Decisiones de diseño

1. **Sin el candado de sesión** en la mutación, para que una renovación a mitad no la bloquee.
2. **Pantalla propia,** no la de restablecer: pide la contraseña actual y se usa con sesión.
3. **Pantalla común a todos los roles,** como «Mis sesiones».

## Seguridad

Pantalla privada. Las contraseñas no se guardan en el cliente. Los errores muestran el mensaje del servidor y nunca cierran la sesión por un 400, 423 o 429.

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
| [UT-HUI-CAMBIARPASSWORD-FE-01](./UT-HUI-CAMBIARPASSWORD-FE-01.md) | Datos y reglas | BE-05 |
| [UT-HUI-CAMBIARPASSWORD-FE-02](./UT-HUI-CAMBIARPASSWORD-FE-02.md) | Pantalla y menú | 01 |
| [UT-HUI-CAMBIARPASSWORD-FE-03](./UT-HUI-CAMBIARPASSWORD-FE-03.md) | Prueba de navegador | 02 |

**Cadena:** BE-05 → 01 → 02 → 03

## Criterios de aceptación cubiertos

| CA | Descripción | UTs |
|----|-------------|-----|
| CA01 | Cambio correcto | 01, 02, 03 |
| CA02 | Actual incorrecta | 02, 03 |
| CA03 | Cierra todas las sesiones | 03 |
| CA04 | Política | 01, 02 |
| CA06 | Bloqueo | 02 |
| CA07 | Límite por IP | 02 |
| CA09 | Sin sesión | 02 |
| CA11 | Aviso por correo | 03 |
| CA15 | Roles e idiomas | 02 |
| CA16 | Un solo envío y campos vacíos | 01, 02 |

## Follow-ups (no se implementan en estas UT)

- Decidir qué hacer con «Mi perfil» y «Configuración».

## Qué NO incluyen estas UT

- La app mobile.
- Cambios en la recuperación de contraseña.
