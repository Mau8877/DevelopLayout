# UT-HUI-CAMBIARPASSWORD-FE-01 — Datos y reglas del formulario

| Campo | Valor |
|-------|-------|
| **Código** | `UT-HUI-CAMBIARPASSWORD-FE-01` |
| **Nombre** | Datos y reglas del formulario |
| **Historia** | HU-INICIAL-CambiarPassword-FE |
| **Depende de** | BE-05 |

## Antes de implementar

FORMULARIOS_ZOD_TANSTACK_FRONTEND, TANSTACK_QUERY_FRONTEND (renovación de sesión), ERRORES_LOGGING_FRONTEND e IDIOMAS_FRONTEND.

## Solicitud de negocio

La web necesita pedir el cambio y dejar la sesión local cerrada.

## Objetivo

Schema y mutación listos, con sus textos de validación.

## Alcance

- Regenerar `shared/api/schema.d.ts` desde el contrato.
- `cambiarPasswordSchema`: `passwordActual` obligatoria (hasta 72 bytes), `passwordNueva` con la misma regla que el login, `confirmacion` igual a la nueva. La nueva distinta de la actual la comprueba el servidor. Mensajes como claves de idioma.
- `useCambiarPassword`: `POST /auth/password/change`, `gcTime: 0`. Al éxito: `queryClient.clear()`, sesión en `null` y navegar a `/login`. Ante un error, la sesión local no se toca.
- **No usar `conCandadoDeSesion`.** Si el access token venció, el interceptor renueva y esa renovación pide el mismo candado: se bloquearía a sí misma. El servidor ya serializa el cambio con la renovación.

**Archivos esperados (crear/tocar):**

- `projects/frontend/src/shared/api/schema.d.ts`
- `projects/frontend/src/features/auth/schemas/cambiarPasswordSchema.ts`
- `projects/frontend/src/features/auth/schemas/index.ts`
- `projects/frontend/src/features/auth/api/useCambiarPassword.ts`
- `projects/frontend/src/features/auth/api/index.ts`
- `projects/frontend/src/features/auth/language/es.json`
- `projects/frontend/src/features/auth/language/en.json`
- `projects/frontend/src/features/auth/tests/cambiarPasswordSchema.test.ts`
- `projects/frontend/src/features/auth/tests/useCambiarPassword.test.tsx`

## Seguridad

A07 y A09: las contraseñas no quedan en la caché de mutaciones, en una `queryKey`, en el almacenamiento ni en logs.

## Fuera de alcance (no tocar)

- La pantalla y la ruta.
- La renovación en `apiClient`.

## Resultado esperado

La lógica del cambio está probada sin interfaz.

## Validación

1. Schema: cada regla con su clave y la confirmación distinta.
2. Éxito: caché limpia, sesión `null` y navegación a `/login`.
3. Errores 400, 423 y 429: la sesión se conserva y no hay navegación.
4. Con un 401 por access token vencido, renueva y reintenta sin quedarse colgado.
5. Tras terminar, la caché de mutaciones no contiene las contraseñas.
