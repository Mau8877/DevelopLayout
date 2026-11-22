# Historia de usuario

| Id | Nombre corto de HU | Prioridad | PHU | Estado |
| --- | --- | --- | --- | --- |
| HU-INICIAL-BITACORA | Consultar la bitácora | Media |  | Pendiente |

**Como:** administrador
**Quiero:** consultar la bitácora de eventos del sistema con filtros
**Para:** auditar quién hizo qué y detectar actividad sospechosa

**Descripción:**
La bitácora registra eventos de seguridad (login, fallos, bloqueos, cierres de sesión, recuperación de contraseña) y cambios de negocio de cualquier módulo. El administrador la consulta en una tabla paginada, ordenada de lo más reciente a lo más antiguo, filtrando por acción, módulo, resultado, usuario y rango de fechas. Los registros no se pueden editar ni borrar desde la aplicación; una tarea diaria elimina los anteriores al período de retención (12 meses por defecto, configurable). Un usuario sin rol `SUPER_ADMIN` no tiene acceso.

Depende de [HU-INICIAL-Base](HU-INICIAL-Base.md) (tabla y servicio de registro).

---

## Conversación/Reglas

**Reglas de negocio**

- Solo `SUPER_ADMIN`. Solo lectura: no hay endpoints de edición ni borrado.
- Filtros: `filter[accion]`, `filter[modulo]`, `filter[resultado]`, `filter[usuarioCodigo]`, `desde`, `hasta`, más `page`/`pageSize` (máximo 100).
- La retención se configura con `app.bitacora.retention-months` (12 por defecto). La purga corre a diario y se registra en los logs, no en la propia bitácora.
- La consulta nunca devuelve el `id` interno.
- Cómo registrar eventos desde otros módulos: [BITACORA_BACKEND](../../.claude/rules/backend/BITACORA_BACKEND.md).

---

## Criterios de aceptación

- **CA01:** Dado un administrador, el sistema debe listar los eventos paginados del más reciente al más antiguo.
- **CA02:** Dado filtros por acción, módulo, resultado, usuario o fechas, el sistema debe devolver solo los eventos que los cumplen.
- **CA03:** Dado un rango con `desde` posterior a `hasta`, el sistema debe responder 400 con error catalogado.
- **CA04:** Dado un usuario sin rol `SUPER_ADMIN`, el sistema debe responder 403.
- **CA05:** Dado cualquier evento, el detalle no debe contener contraseñas, tokens ni datos de pago.
- **CA06:** Dado que existen eventos anteriores al período de retención, la tarea diaria debe eliminarlos y conservar los demás.
- **CA07:** Dado el panel web, el administrador debe poder filtrar y paginar desde la tabla sin recargar la página.

## Seguridad

- A09/API4: paginación con máximo, filtros validados, sin exponer datos sensibles. Acceso solo para `SUPER_ADMIN`.

## Desarrollador

Pendiente de asignar.
