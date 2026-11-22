---
globs: projects/backend/**/*
---

# Schemas de PostgreSQL — Backend

Complementa a [PERSISTENCIA_BD_BACKEND.md](PERSISTENCIA_BD_BACKEND.md),
[BASE_DE_DATOS_PRIVADA_RLS.md](BASE_DE_DATOS_PRIVADA_RLS.md) y
[ARQUITECTURA_BACKEND.md](../ARQUITECTURA_BACKEND.md).

La base de datos se organiza **por schemas, uno por dominio de negocio**,
no con todas las tablas mezcladas en `public`. Es el mismo criterio de
"feature-based" del código, llevado a la base: quien abre la base ve de un
vistazo a qué dominio pertenece cada tabla, y cada dominio tiene sus
propios permisos.

## Schemas actuales

| Schema | Dominio | Tablas |
| --- | --- | --- |
| `authentication` | Identidad, acceso y trazabilidad. **Siempre existe**, en todo proyecto que nazca de esta plantilla. | `usuarios`, `roles`, `usuarios_roles`, `sesiones`, `bitacora` |
| `public` | Solo lo técnico: `flyway_schema_history` y extensiones. **Ninguna tabla de negocio.** | — |

Cada proyecto agrega los suyos (`orders`, `teams`, ...) y los lista en esta
tabla en el mismo cambio que los crea.

> `bitacora` vive en `authentication` aunque registra eventos de cualquier
> módulo (ver [BITACORA_BACKEND.md](BITACORA_BACKEND.md)). Es una decisión
> consciente: hoy es el único consumidor de auditoría. Si la auditoría
> crece (más tablas, otros reportes), se evalúa moverla a su propio schema.

## Regla estricta: toda feature declara su schema

Al crear una feature (en el `README.md` de sus Unidades de Trabajo de
backend, sección **Decisiones de diseño**, ver
[UNIDADES_DE_TRABAJO.md](../UNIDADES_DE_TRABAJO.md)) se escribe:

```
Schema: {nombre} — {existente | nuevo} — {justificación en una frase}
```

Una feature con tablas que no declara su schema no se implementa. No se
asume `public` ni "el que ya esté".

### ¿Schema nuevo o uno existente?

Se crea un schema nuevo **solo si se cumplen la mayoría** de estas
condiciones:

1. Es un **dominio de negocio con identidad propia** (pedidos, equipos,
   facturación), no un detalle de otro dominio.
2. Va a tener **3 o más tablas** relacionadas entre sí, o ya se sabe que
   crecerá a eso.
3. Tiene **permisos, retención o ciclo de vida propios** (otro equipo lo
   mantiene, otra política de borrado, otra política de RLS).
4. Podría separarse algún día en su propio servicio sin arrastrar tablas
   de otros dominios.

Si no se cumple, **la tabla va al schema del dominio más cercano**. Nunca:

- Un schema por tabla o por Historia de Usuario.
- Una tabla de negocio en `public`.
- Un schema "common" o "misc" como cajón de sastre.

Si hay duda, se prefiere el schema existente: separar después es una
migración; fusionar schemas después también, pero con más dolor.

## Nombres

- Schema: **minúscula, `snake_case`, sustantivo del dominio** (`authentication`,
  `orders`, `teams`). Inmutable una vez creado: renombrarlo rompe migraciones
  ya aplicadas.
- Las tablas siguen [PERSISTENCIA_BD_BACKEND.md](PERSISTENCIA_BD_BACKEND.md)
  (plural, `snake_case`).

## Cómo se implementa

### Migraciones Flyway

- Flyway guarda su historial en `public.flyway_schema_history` (no en un
  schema de negocio).
- La primera migración de un schema lo crea:
  `CREATE SCHEMA IF NOT EXISTS orders;`. Después, **todo objeto se escribe
  calificado** (`orders.pedidos`, `orders.idx_pedidos_usuario`), nunca
  dependiendo del `search_path`.
- Una migración toca **un solo schema** salvo que la dependencia sea
  explícita (ej. una FK hacia `authentication.usuarios`).
- Las migraciones ya aplicadas no se tocan (checksum de Flyway): la regla
  rige para lo nuevo. Si hay tablas previas en `public`, se mueven con una
  migración `ALTER TABLE ... SET SCHEMA`.

### Permisos del rol `app_runtime`

La migración que crea el schema también:

```sql
GRANT USAGE ON SCHEMA orders TO app_runtime;
ALTER DEFAULT PRIVILEGES IN SCHEMA orders
    GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO app_runtime;
ALTER DEFAULT PRIVILEGES IN SCHEMA orders GRANT USAGE ON SEQUENCES TO app_runtime;
```

Sin `USAGE` sobre el schema, el rol no ve sus tablas aunque tenga `GRANT`
sobre ellas. `app_runtime` nunca recibe `CREATE` sobre ningún schema; y se
revoca `CREATE` sobre `public` a `PUBLIC`.

### Funciones `SECURITY DEFINER`

Fijan el `search_path` explícitamente al schema que usan
(`SET search_path = pg_catalog, authentication`, nunca `public`), con
`REVOKE ALL ... FROM PUBLIC` y `GRANT EXECUTE` solo a `app_runtime`. Es
la defensa contra el secuestro de funciones por `search_path`.

### Models JPA

El schema va **explícito en el Model**, no en una configuración global:

```java
@Entity
@Table(name = "usuarios", schema = "authentication")
@AttributeOverride(name = "codigo", column = @Column(name = "codigo_usuario"))
public class Usuario extends BaseModel { ... }
```

No se configura `hibernate.default_schema`: con un default global, olvidar
el `schema` en un Model nuevo lo deja silenciosamente en el schema
equivocado. Las consultas nativas (`@Query(nativeQuery = true)`) también
califican el schema.

### RLS, errores y logs

RLS sigue exactamente igual
([BASE_DE_DATOS_PRIVADA_RLS.md](BASE_DE_DATOS_PRIVADA_RLS.md)): por tabla,
con `ENABLE` + `FORCE` y políticas en la misma migración. Los prefijos de
código de error y el campo `module` de los logs siguen al módulo Java
(`auth`, `pedidos`), no al nombre del schema.

## Dependencias entre schemas

Mismo sentido que la regla de dependencia de
[ARQUITECTURA_BACKEND.md](../ARQUITECTURA_BACKEND.md#regla-de-dependencia):

- Un schema de negocio **puede** referenciar a `authentication` con una
  FK (ej. `orders.pedidos.usuario_id → authentication.usuarios.id`).
- `authentication` **nunca** referencia a un schema de negocio.
- Entre schemas de negocio no hay FKs salvo justificación escrita en el
  `README.md` de las UT. Aun con FK, desde Java un módulo accede a los datos
  de otro a través del `service` de su dueño, no de su `repository`.

## Tests

- Un test de migración por schema verifica que sus tablas están en el
  schema correcto (`SELECT table_schema, table_name FROM
  information_schema.tables WHERE table_schema = '...'`).
- Los tests de RLS ([BASE_DE_DATOS_PRIVADA_RLS.md](BASE_DE_DATOS_PRIVADA_RLS.md#6-tests))
  usan nombres calificados y el rol `app_runtime`.
- Un test confirma que `app_runtime` no puede crear objetos en ningún schema.
