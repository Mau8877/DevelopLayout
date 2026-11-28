# Unidades de trabajo — HU-INICIAL-Base-SEG

**Historia:** verificación de seguridad de la plantilla frente a OWASP y corrección de lo encontrado.
**Fuente HU:** [`HU-INICIAL-Base`](../../../HistoriasDeUsuario/HU-INICIAL-Base.md)
**Recurso HTTP / contrato:** todo `/api/v1/**`; no cambia el contrato.

## Antes de implementar cualquier UT

Leer [SEGURIDAD_OWASP.md](../../../../.claude/rules/SEGURIDAD_OWASP.md) y
[UNIDADES_DE_TRABAJO.md](../../../../.claude/rules/UNIDADES_DE_TRABAJO.md#toda-hu-termina-con-una-ut-de-seguridad).

## Decisiones de diseño

La regla de la UT de seguridad es posterior a las historias iniciales. En vez de
una ficha por historia, esta UT cubre la plantilla completa y su resultado, con
la tabla de controles, es el informe de auditoría.

## Seguridad

Es el objeto de la unidad: ver el informe.

## Contrato de datos / recurso HTTP

Sin cambios de rutas ni de campos. Una contraseña muy usada responde 400 con
`ERR_SYS_01`.

## Orden de ejecución

| Código | Nombre | Depende de |
|--------|--------|------------|
| [UT-HUI-BASE-SEG-01](./UT-HUI-BASE-SEG-01.md) | Auditoría OWASP de la plantilla | — |

**Cadena:** una sola unidad, al final de las historias iniciales.

## Criterios de aceptación cubiertos

| CA (HU-INICIAL-Base) | Descripción | UTs |
|-------------|-------------|-----|
| CA10 | Contraseñas muy usadas | SEG-01 |
| CA11 | Logs sin secretos ni datos personales | SEG-01 |
| CA12 | Rechazos registrados | SEG-01 |
| CA13 | Controles que no se evaden | SEG-01 |
| CA14 | Esperas limitadas | SEG-01 |

El rechazo de contraseñas muy usadas (CA10) es además el CA20 de
`HU-INICIAL-OlvidePassword`, el CA17 de `HU-INICIAL-CambiarPassword` y el CA22
de `HU-INICIAL-Usuarios`.

## Follow-ups (no se implementan en estas UT)

Los pendientes P-01 a P-08 del
[informe](../../../seguridad/AUDITORIA_OWASP.md#pendientes).

## Qué NO incluyen estas UT

Pruebas de penetración, escaneo dinámico, validación en un dispositivo o en un
servidor reales y funciones nuevas (doble factor, alertas).
