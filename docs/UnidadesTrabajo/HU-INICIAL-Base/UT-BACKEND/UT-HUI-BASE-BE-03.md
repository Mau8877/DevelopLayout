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
- `features/usuarios/RolNombres.java` con constantes `SUPER_ADMIN` y `USUARIO` (los roles son datos, no un enum cerrado).
- `UsuarioRepository` (`findByCodigo`, `findByEmailIgnoreCase`, `existsByEmailIgnoreCase`), `RolRepository` (`findByNombre`), `UsuarioRolRepository` (`findActivosByUsuario`: roles donde ambos `isActive` son `true`). Repositorios tipados con `Long`.
- `features/auth/model/Sesion.java` (extiende `BaseModel`, `@AttributeOverride` de `codigo`, `@Getter` por clase, `@Setter` por campo mutable: `refreshHash`, `refreshHashAnterior`, `estado`, `expiraEn`, `ultimoUsoEn`, `revocadaEn`, `motivoRevocacion`; el hash nunca entra en `toString`).
- `features/auth/enums/EstadoSesion.java`, `TipoCliente.java`, `MotivoRevocacion.java` (`LOGOUT`, `USUARIO`, `ADMIN`, `REUSO`, `RESET_PASSWORD`), todos `@Enumerated(EnumType.STRING)`.
- `features/auth/repository/SesionRepository.java` (`findByRefreshHash`, `findByRefreshHashAnterior`, `findByCodigo`, `findActivasByUsuarioCodigo`).
- `common/bitacora/model/Bitacora.java` (extiende `BaseModel`, `detalle` como `jsonb` vía `@JdbcTypeCode(SqlTypes.JSON)`), `common/bitacora/ResultadoBitacora.java` y `common/bitacora/repository/BitacoraRepository.java` (solo `save`; las consultas las agrega HU-INICIAL-Bitacora).
- Todo Model lleva el schema explícito: `@Table(name = "usuarios", schema = "authentication")` (igual para `Rol`, `UsuarioRol`, `Sesion` y `Bitacora`). No se configura `hibernate.default_schema`.

**Archivos esperados (crear/tocar):**

- `projects/backend/src/main/java/com/example/backend/features/usuarios/model/Usuario.java`
- `projects/backend/src/main/java/com/example/backend/features/usuarios/model/Rol.java`
- `projects/backend/src/main/java/com/example/backend/features/usuarios/model/UsuarioRol.java`
- `projects/backend/src/main/java/com/example/backend/features/usuarios/enums/EstadoUsuario.java`
- `projects/backend/src/main/java/com/example/backend/features/usuarios/RolNombres.java`
- `projects/backend/src/main/java/com/example/backend/features/usuarios/repository/UsuarioRepository.java`
- `projects/backend/src/main/java/com/example/backend/features/usuarios/repository/RolRepository.java`
- `projects/backend/src/main/java/com/example/backend/features/usuarios/repository/UsuarioRolRepository.java`
- `projects/backend/src/main/java/com/example/backend/features/auth/model/Sesion.java`
- `projects/backend/src/main/java/com/example/backend/features/auth/enums/EstadoSesion.java`
- `projects/backend/src/main/java/com/example/backend/features/auth/enums/TipoCliente.java`
- `projects/backend/src/main/java/com/example/backend/features/auth/enums/MotivoRevocacion.java`
- `projects/backend/src/main/java/com/example/backend/features/auth/repository/SesionRepository.java`
- `projects/backend/src/main/java/com/example/backend/common/bitacora/model/Bitacora.java`
- `projects/backend/src/main/java/com/example/backend/common/bitacora/ResultadoBitacora.java`
- `projects/backend/src/main/java/com/example/backend/common/bitacora/repository/BitacoraRepository.java`

## Fuera de alcance (no tocar)

- Controllers, services y DTOs de usuarios (no hay endpoints de usuarios en esta versión).
- Cargar `UserDetails` o emitir tokens (UT de HU-INICIAL-Login).
- La migración (UT-HUI-BASE-BE-01).
- Servicios que usan `Sesion` o `Bitacora` (UT de las HU de auth y UT-HUI-BASE-BE-06).

## Resultado esperado

El proyecto compila con los tres Models mapeados a las tablas de la migración.

## Validación

1. `./mvnw clean package` compila y `spring.jpa.hibernate.ddl-auto=validate` (o equivalente en el test) valida el mapeo contra la migración.
2. `./mvnw checkstyle:check` sin errores.

## Seguridad

A01: ningún Model expone `password_hash` por `toString`; setters solo donde hacen falta (A04).
