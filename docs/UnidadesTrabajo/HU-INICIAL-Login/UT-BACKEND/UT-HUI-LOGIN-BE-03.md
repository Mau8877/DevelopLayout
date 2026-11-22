# UT-HUI-LOGIN-BE-03 — UserDetailsService y contexto RLS por transacción

| Campo | Valor |
|-------|-------|
| **Código** | `UT-HUI-LOGIN-BE-03` |
| **Nombre** | UserDetailsService y contexto RLS por transacción |
| **Historia** | HU-INICIAL-Login-BE |
| **Depende de** | Models y repositorios de usuarios (HU-INICIAL-Base) |

## Antes de implementar

Leer `backend/SEGURIDAD_AUTH_BACKEND.md`, `backend/BASE_DE_DATOS_PRIVADA_RLS.md` (sección 4), `backend/PERSISTENCIA_BD_BACKEND.md` y `.claude/rules/SEGURIDAD_OWASP.md`.

## Solicitud de negocio

Para validar credenciales hay que leer el usuario con RLS activo, y el contexto de usuario debe fijarse de forma uniforme.

## Objetivo

El sistema carga un usuario con sus roles activos y existe un único componente que fija el contexto RLS.

## Alcance

- `security/RlsContext.java` con `usarCorreoDeLogin(String email)` y `usarUsuario(UUID codigo)`: ejecutan `set_config('app.login_email' | 'app.current_user', :v, true)` con alcance de transacción; exigen transacción activa.
- `security/UserDetailsServiceImpl.java`: en `@Transactional(readOnly = true)` fija el correo de login, busca el usuario, fija `app.current_user` y carga los roles donde `UsuarioRol.isActive` y `Rol.isActive` son `true`; `authorities` como `ROLE_{nombre}` y `enabled = (estado == ACTIVO)`.
- Usuario inexistente: `UsernameNotFoundException` (el login la convierte en el error genérico).

**Archivos esperados (crear/tocar):**

- `projects/backend/src/main/java/com/example/backend/security/RlsContext.java`
- `projects/backend/src/main/java/com/example/backend/security/UserDetailsServiceImpl.java`

## Fuera de alcance (no tocar)

- Verificar contraseñas o lockout (UT-HUI-LOGIN-BE-07).
- Otro uso de `RlsContext` fuera de auth.
- Cambios a la migración de RLS.

## Resultado esperado

`loadUserByUsername` devuelve usuario y roles activos usando RLS, y falla sin transacción.

## Validación

1. Test de integración (Testcontainers, rol `app_runtime`): carga el admin sembrado con `ROLE_ADMIN`.
2. Test: rol con `Rol.isActive=false` no aparece; usuario `INACTIVO` da `enabled=false`.
3. Test: `RlsContext` sin transacción lanza excepción.

## Seguridad

A01: `is_local=true` para no filtrar contexto entre requests del pool; falla cerrado sin contexto.
