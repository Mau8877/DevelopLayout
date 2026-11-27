# UT-HUI-USUARIOS-FE-05 — Reenviar, desactivar y reactivar

| Campo | Valor |
|-------|-------|
| **Código** | `UT-HUI-USUARIOS-FE-05` |
| **Nombre** | Reenviar, desactivar y reactivar |
| **Historia** | HU-INICIAL-Usuarios-FE |
| **Depende de** | 01, 02, 03 |

## Antes de implementar

ERRORES_LOGGING_FRONTEND.

## Solicitud de negocio

El administrador actúa sobre cada cuenta.

## Objetivo

Acciones por fila según la situación de la cuenta.

## Alcance

- `RowActionButton`: **reenviar** (solo activa y pendiente), **desactivar** (activa) y **reactivar** (inactiva). La propia cuenta (`useSession`) no ofrece desactivar.
- Desactivar pide confirmación con `ModalConfirmacion`, avisando que se cierran sus sesiones. Reactivar y reenviar no la piden.
- Toast con el resultado; reenvío no enviado como advertencia; 409 y 404 muestran el mensaje y refrescan la lista.
- Si responde 401, no se maneja en la pantalla: lo resuelve `apiClient`.

**Archivos esperados (crear/tocar):**

- `projects/frontend/src/features/usuarios/screens/UsuariosScreen.tsx`
- `projects/frontend/src/features/usuarios/tests/UsuariosAcciones.test.tsx`

## Seguridad

Ocultar un botón es comodidad; el backend rechaza igual (CA15, CA18).

## Fuera de alcance (no tocar)

- Cambiar roles (HU Roles).

## Resultado esperado

Cada fila ofrece solo las acciones que aplican.

## Validación

1. Cada situación muestra sus acciones; la propia cuenta no tiene «Desactivar».
2. No se desactiva sin confirmar; al confirmar se llama una vez y la lista se actualiza.
3. Acceso denegado: un 409 o 403 muestra el mensaje y la fila no cambia de forma local.
