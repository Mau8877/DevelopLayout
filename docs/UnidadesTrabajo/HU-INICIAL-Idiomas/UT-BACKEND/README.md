# Unidades de trabajo — HU-INICIAL-Idiomas-BE

**Historia:** El servidor responde y envía correos en español o inglés según el cliente.
**Fuente HU:** [`HU-INICIAL-Idiomas.md`](../../../HistoriasDeUsuario/HU-INICIAL-Idiomas.md)
**Recurso HTTP / contrato:** Todo `/api/v1/**` (cabecera `Accept-Language`) y los correos

## Antes de implementar cualquier UT

Leer las rules de [`backend/`](../../../../.claude/rules/backend/) y [`SEGURIDAD_OWASP.md`](../../../../.claude/rules/SEGURIDAD_OWASP.md). Estas fichas **no** sustituyen esas reglas: si hay conflicto, ganan las rules del proyecto.

## Decisiones de diseño

Schema: no crea ni cambia tablas.

1. **`.properties` de Spring**, uno por módulo, y no JSON: es el mecanismo estándar y no requiere dependencias.
2. **Las excepciones llevan una clave**, no una frase; se traducen en un único punto.
3. **Migración por módulos** con un respaldo temporal que la UT 04 elimina.
4. **El idioma de la invitación es el del administrador** (decisión de la historia): no se guarda preferencia en la base.

## Seguridad

Lista cerrada de idiomas; mismos códigos y estados; sin enumeración de cuentas en ningún idioma.

## Contrato de datos / recurso HTTP

Sin cambios de contrato: mismas rutas y sobre. Solo cambia el texto de `message` según `Accept-Language`.

## Orden de ejecución

| Código | Nombre | Depende de |
|--------|--------|------------|
| [UT-HUI-IDIOMAS-BE-01](./UT-HUI-IDIOMAS-BE-01.md) | Infraestructura y comunes | — |
| [UT-HUI-IDIOMAS-BE-02](./UT-HUI-IDIOMAS-BE-02.md) | Autenticación | 01 |
| [UT-HUI-IDIOMAS-BE-03](./UT-HUI-IDIOMAS-BE-03.md) | Usuarios y bitácora | 01 |
| [UT-HUI-IDIOMAS-BE-04](./UT-HUI-IDIOMAS-BE-04.md) | Correos y control | 02, 03 |

**Cadena:** 01 → 02, 03 → 04

## Criterios de aceptación cubiertos

| CA | Descripción | UTs |
|----|-------------|-----|
| CA05 | Mensajes del servidor | 01, 02, 03 |
| CA06 | Idioma por defecto | 01 |
| CA07 | Correo de recuperación | 04 |
| CA08 | Correo de invitación | 04 |
| CA10 | Idioma neutro | 02, 03, 04 |
| CA11 | Comprobaciones | 04 |

## Follow-ups (no se implementan en estas UT)

- Guardar el idioma preferido en la cuenta.

## Qué NO incluyen estas UT

- Pantallas web o mobile.
- Más idiomas.
