---
globs: projects/backend/**/*
---

# Reportes — JasperReports

Versión: **JasperReports 7.1.x** (`net.sf.jasperreports`: `jasperreports`,
`jasperreports-pdf`, `jasperreports-fonts`, todos con la propiedad
`jasperreports.version` del `pom.xml`; no los administra el BOM de Spring).
Jasper 7 es la línea Jakarta; no mezclar con artefactos 6.x.

> **Incluido aunque todavía no haya reportes**, a propósito: la plantilla deja
> la dependencia lista. Cuesta unas 33 librerías (21 de los 102 MB del jar, medido
> con 7.1.1) que Trivy y Dependabot también vigilan. Un proyecto que no vaya a
> tener reportes quita las tres dependencias y `jasperreports.version` del `pom.xml`.

## Dónde vive

`features/{modulo}/` como cualquier otra lógica: la generación del reporte
es del `service` (nunca del controller). Plantillas `.jrxml` en
`src/main/resources/reports/{modulo}/`.

## Seguridad (obligatorio — A03/A05/A08)

- **Las plantillas son código, no datos.** Solo se usan `.jrxml`/`.jasper`
  versionadas en el repo. **Prohibido** compilar o cargar una plantilla
  subida por un usuario o descargada de una URL externa.
- **Nunca** deserializar un `.jasper`/objeto de origen no confiable.
- **Parámetros siempre como parámetros** (`$P{nombre}`) de Jasper, que usan
  `PreparedStatement`. **Prohibido** `$P!{...}` (sustitución textual, inyección
  SQL) con cualquier valor que venga del cliente.
- Preferir pasar al reporte un `JRBeanCollectionDataSource` (datos ya
  consultados por el `service` con RLS aplicado) en vez de dejar que el
  reporte consulte la base con su propia conexión: así el reporte nunca
  ve más de lo que el usuario puede ver.
- Mantener las propiedades de endurecimiento que Jasper ofrece desde 7.0.x
  (filtro de clases deserializables, lista blanca de URLs de recursos,
  deshabilitar query executors dinámicos). Al actualizar Jasper, **leer sus
  release notes**: varias versiones 7.0.x corrigieron CVEs.
- Los reportes que muestran datos de usuario se generan **autorizando
  primero** (`@PreAuthorize` + propiedad del dato), igual que un `GET`.
- No exponer errores de Jasper al cliente: se capturan en el `service` y se
  lanzan como excepción de negocio catalogada.

## Operación

- Fuentes: la imagen Docker instala `fontconfig` + `ttf-dejavu`; si una
  plantilla usa otra fuente, se agrega en el `Dockerfile` o se usa
  `jasperreports-fonts`. Arranque con `-Djava.awt.headless=true`.
- Los `.jrxml` se compilan una vez (cache en memoria del `JasperReport`),
  no en cada request.
- Reportes grandes: paginar/limitar filas y tamaño (API4); nunca generar un
  reporte ilimitado en memoria.
- Tests: un test por reporte que genera el PDF con datos de prueba y
  verifica que no está vacío.
