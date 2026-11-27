# UT-HUI-USUARIOS-BE-04 — Guardia: solo un administrador vigente puede cambiar usuarios

| Campo | Valor |
|-------|-------|
| **Código** | `UT-HUI-USUARIOS-BE-04` |
| **Nombre** | Guardia: solo un administrador vigente puede cambiar usuarios |
| **Historia** | HU-INICIAL-Usuarios-BE |
| **Depende de** | 01 |

## Antes de implementar

SEGURIDAD_AUTH_BACKEND («Mis sesiones»: solo una sesión activa actúa sobre otras) y `RlsContext`.

## Solicitud de negocio

Dos administradores no pueden dejarse fuera mutuamente, y una sesión ya cerrada no puede seguir cambiando cuentas.

## Objetivo

Un único punto que serializa los cambios de administración y vuelve a comprobar a quien los pide.

## Alcance

`GuardiaAdministradores.exigirVigente(UUID actor, UUID sesionActual)`, que exige una transacción activa (`Propagation.MANDATORY`) y, **en este orden**:

1. Fija `rlsContext.usarAdmin()`.
2. Toma `pg_advisory_xact_lock` con una clave fija (`usuarios.administracion`): todos los cambios de usuarios y roles pasan de a uno. Es antes de leer cualquier entidad.
3. Comprueba, ya con el bloqueo, que el actor está `ACTIVO`, tiene `SUPER_ADMIN` asignado y activo, y que su sesión `sid` sigue `ACTIVA`.
4. Si algo no se cumple, `UnauthorizedException` `ERR_AUTH_04` y no cambia nada.

La sesión se consulta con un método nuevo de `SesionService` (`estaActiva(codigo, usuario)`), no desde su repositorio.

**Por qué alcanza para «siempre queda un administrador que puede entrar»:** quitar poder de administrador (desactivar o quitar el rol) lo hace siempre un actor vigente, comprobado bajo el bloqueo, y nadie puede hacérselo a sí mismo; así que el actor sigue siendo administrador después del cambio. Un administrador con invitación pendiente no tiene sesión, por lo que nunca es actor. No hace falta contar administradores.

**Archivos esperados (crear/tocar):**

- `projects/backend/src/main/java/com/example/backend/features/usuarios/service/GuardiaAdministradores.java`
- `projects/backend/src/main/java/com/example/backend/features/auth/service/SesionService.java`
- `projects/backend/src/test/java/com/example/backend/features/usuarios/GuardiaAdministradoresTest.java`

## Seguridad

A01 y A10: autorización comprobada en el momento del cambio; ante cualquier duda, se deniega.

## Fuera de alcance (no tocar)

- Las operaciones que usan la guardia.
- Las operaciones de «Sesiones de usuarios», que no cambian quién es administrador.

## Resultado esperado

Cualquier cambio de usuarios puede exigir un administrador vigente con una sola llamada.

## Validación

1. Pasa con un administrador activo y su sesión activa.
2. Acceso denegado: actor inactivo, sin el rol, o con la sesión revocada da 401.
3. Con dos transacciones reales, la segunda espera a que termine la primera y ve su resultado.
4. Llamarla sin transacción falla.
