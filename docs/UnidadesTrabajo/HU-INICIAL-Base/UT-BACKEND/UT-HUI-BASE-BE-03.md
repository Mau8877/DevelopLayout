# UT-HUI-BASE-BE-03 — Models, enums y repositorios de usuarios y roles

| Campo | Valor |
|-------|-------|
| **Código** | `UT-HUI-BASE-BE-03` |
| **Nombre** | Models, enums y repositorios de usuarios y roles |
| **Historia** | HU-INICIAL-Base-BE |
| **Depende de** | UT-HUI-BASE-BE-01 |

## Antes de implementar

Leer `backend/CONVENCIONES_JAVA_BACKEND.md`, `backend/PERSISTENCIA_BD_BACKEND.md` (patrón BaseModel), `ARQUITECTURA_BACKEND.md` y `.claude/rules/SEGURIDAD_OWASP.md`.

## Solicitud de negocio

La aplicación necesita representar usuarios y roles con el mismo patrón que el resto del proyecto.

## Objetivo

Existen los Models `Usuario`, `Rol` y `UsuarioRol` extendiendo `BaseModel`, con sus repositorios.

## Alcance

- `features/usuarios/model/Usuario.java`, `Rol.java`, `UsuarioRol.java`: `@Entity`, `extends BaseModel`, `@AttributeOverride` del nombre de columna de `codigo`, `@Getter` por clase y `@Setter` solo por campo mutable (nunca `@Data`; `passwordHash` excluido de cualquier `toString`).
- `features/usuarios/enums/EstadoUsuario.java` (`ACTIVO`, `INACTIVO`), persistido `@Enumerated(EnumType.STRING)`.
- `features/usuarios/RolNombres.java` con constantes `ADMIN` y `USUARIO` (los roles son datos, no un enum cerrado).
- `UsuarioRepository` (`findByCodigo`, `findByEmailIgnoreCase`, `existsByEmailIgnoreCase`), `RolRepository` (`findByNombre`), `UsuarioRolRepository` (`findActivosByUsuario`: roles donde ambos `isActive` son `true`). Repositorios tipados con `Long`.

**Archivos esperados (crear/tocar):**

- `projects/backend/src/main/java/com/example/backend/features/usuarios/model/Usuario.java`
- `projects/backend/src/main/java/com/example/backend/features/usuarios/model/Rol.java`
- `projects/backend/src/main/java/com/example/backend/features/usuarios/model/UsuarioRol.java`
- `projects/backend/src/main/java/com/example/backend/features/usuarios/enums/EstadoUsuario.java`
- `projects/backend/src/main/java/com/example/backend/features/usuarios/RolNombres.java`
- `projects/backend/src/main/java/com/example/backend/features/usuarios/repository/UsuarioRepository.java`
- `projects/backend/src/main/java/com/example/backend/features/usuarios/repository/RolRepository.java`
- `projects/backend/src/main/java/com/example/backend/features/usuarios/repository/UsuarioRolRepository.java`

## Fuera de alcance (no tocar)

- Controllers, services y DTOs de usuarios (no hay endpoints de usuarios en esta versión).
- Cargar `UserDetails` o emitir tokens (UT de HU-INICIAL-Login).
- La migración (UT-HUI-BASE-BE-01).

## Resultado esperado

El proyecto compila con los tres Models mapeados a las tablas de la migración.

## Validación

1. `./mvnw clean package` compila y `spring.jpa.hibernate.ddl-auto=validate` (o equivalente en el test) valida el mapeo contra la migración.
2. `./mvnw checkstyle:check` sin errores.

## Seguridad

A01: ningún Model expone `password_hash` por `toString`; setters solo donde hacen falta (A04).
