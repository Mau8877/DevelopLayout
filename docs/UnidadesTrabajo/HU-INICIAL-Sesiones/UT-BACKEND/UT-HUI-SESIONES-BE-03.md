# UT-HUI-SESIONES-BE-03 — Tests de mis sesiones

| Campo | Valor |
|-------|-------|
| **Código** | `UT-HUI-SESIONES-BE-03` |
| **Nombre** | Tests de mis sesiones |
| **Historia** | HU-INICIAL-Sesiones-BE |
| **Depende de** | UT-HUI-SESIONES-BE-02 |

## Antes de implementar

Leer `backend/TESTING_BACKEND.md` y `backend/BASE_DE_DATOS_PRIVADA_RLS.md` (tests).

## Solicitud de negocio

Probar el aislamiento entre usuarios y las carreras entre cierres.

## Objetivo

Tests de integración que cubren CA01–CA10.

## Alcance

`MisSesionesControllerTest` sobre `AuthSesionIntegrationTest` (app como `app_runtime`, RLS real), verificando el sobre `ApiResponse`:

- Listado: solo las propias, sin vencidas, actual primero también con `pageSize=1`, `meta` correcto, sin campos internos.
- **Autorización:** otro usuario recibe 404 al cerrar y no ve nada ajeno; un `SUPER_ADMIN` usando estos endpoints tampoco ve ni cierra sesiones ajenas; sin token 401; web sin CSRF 403.
- Cierre: access y refresh de la cerrada dejan de servir; la actual sigue; evento en bitácora con `motivo` y sin datos sensibles; repetir el cierre no duplica el evento.
- `cerrar-otras` alcanza una sesión activa pero vencida.
- **CA08 determinista:** revocar en la base la sesión actual (sin anotarla en Redis, para que el token pase el filtro) y pedir el cierre: 401 y nada cambia.
- **CA09:** dos cierres cruzados simultáneos, repetido varias veces: queda exactamente una activa y ninguna respuesta es 500.
- Agregar el flujo a `AuthPoolTest` si hace falta para descartar agotamiento del pool.

**Archivos esperados (crear/tocar):**

- `projects/backend/src/test/java/com/example/backend/features/auth/MisSesionesControllerTest.java`

## Seguridad

Evidencia de A01, A07, API1 y API5.

## Fuera de alcance (no tocar)

- Código de producción (un defecto se corrige en la UT que lo introdujo).

## Resultado esperado

Los tests pasan de forma repetible.

## Validación

1. `./mvnw test` en verde varias veces.
2. Quitar la comprobación de la sesión actual hace fallar los tests de CA08 y CA09.
