# Unidades de trabajo — HU-INICIAL-CambiarPassword-BE

**Historia:** Cambiar la propia contraseña con la sesión iniciada y cerrar todas las sesiones.
**Fuente HU:** [`HU-INICIAL-CambiarPassword.md`](../../../HistoriasDeUsuario/HU-INICIAL-CambiarPassword.md)
**Recurso HTTP / contrato:** `POST /api/v1/auth/password/change`

## Antes de implementar cualquier UT

Leer las rules de [`backend/`](../../../../.claude/rules/backend/), [`IDIOMAS.md`](../../../../.claude/rules/IDIOMAS.md) y [`SEGURIDAD_OWASP.md`](../../../../.claude/rules/SEGURIDAD_OWASP.md). Estas fichas **no** sustituyen esas reglas: si hay conflicto, ganan las rules del proyecto.

## Decisiones de diseño

Schema: `authentication` — existente — no hay tablas nuevas, solo un valor más en una restricción.

1. **400 y no 401 para la contraseña actual incorrecta:** un 401 haría que los clientes intentaran renovar o cerrar la sesión.
2. **Contador propio por cuenta,** separado del de login, y reservado antes de comparar.
3. **Mismos bloqueos que la recuperación** (primero la fila del usuario, luego sus sesiones) para que no se crucen con un login ni con un restablecimiento.
4. **Los enlaces pendientes se anulan antes de confirmar:** si falla, no cambia la contraseña. Si fallara la confirmación después, se pierde el enlace y se pide otro.
5. **El aviso se envía en el ejecutor acotado:** un SMTP lento no retrasa la respuesta. El envío no está garantizado.

## Seguridad

Solo la propia persona, con sesión vigente y su contraseña actual. Sin tablas nuevas: rige el RLS de `usuario` y `sesion` y el trigger que reserva el cambio de contraseña a su dueño. Límite por cuenta y por IP. Se registran éxito, fallo y bloqueo, sin datos sensibles.

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
| [UT-HUI-CAMBIARPASSWORD-BE-01](./UT-HUI-CAMBIARPASSWORD-BE-01.md) | Motivo de revocación | — |
| [UT-HUI-CAMBIARPASSWORD-BE-02](./UT-HUI-CAMBIARPASSWORD-BE-02.md) | Límite de intentos | — |
| [UT-HUI-CAMBIARPASSWORD-BE-03](./UT-HUI-CAMBIARPASSWORD-BE-03.md) | Correo de aviso | — |
| [UT-HUI-CAMBIARPASSWORD-BE-04](./UT-HUI-CAMBIARPASSWORD-BE-04.md) | Servicio | 01, 02, 03 |
| [UT-HUI-CAMBIARPASSWORD-BE-05](./UT-HUI-CAMBIARPASSWORD-BE-05.md) | Endpoint y contrato | 04 |

**Cadena:** 01, 02, 03 → 04 → 05

## Criterios de aceptación cubiertos

| CA | Descripción | UTs |
|----|-------------|-----|
| CA01 | Cambio correcto | 04, 05 |
| CA02 | Actual incorrecta | 04, 05 |
| CA03 | Cierra todas las sesiones | 01, 04 |
| CA04 | Política | 05 |
| CA05 | Igual a la actual | 04 |
| CA06 | Bloqueo por intentos | 02, 04 |
| CA07 | Límite por IP | 05 |
| CA08 | Anula enlaces | 04 |
| CA09 | Sin sesión | 05 |
| CA10 | Carreras | 04 |
| CA11 | Aviso por correo | 03, 04 |
| CA12 | Bitácora | 04 |
| CA13 | CSRF | 05 |
| CA14 | Cambios simultáneos | 04 |

## Follow-ups (no se implementan en estas UT)

- Enviar el mismo aviso al restablecer con «¿Olvidaste tu contraseña?» (se dejó fuera de esta historia).
- Ampliar la prueba de producción con este flujo.

## Qué NO incluyen estas UT

- Que un administrador cambie la contraseña de otra persona.
- Historial de contraseñas o reglas de complejidad.
