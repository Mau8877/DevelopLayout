# UT-HUI-BASE-BE-04 — Administrador inicial en el primer arranque

| Campo | Valor |
|-------|-------|
| **Código** | `UT-HUI-BASE-BE-04` |
| **Nombre** | Administrador inicial en el primer arranque |
| **Historia** | HU-INICIAL-Base-BE |
| **Depende de** | UT-HUI-BASE-BE-03 |

## Antes de implementar

Leer `backend/SEGURIDAD_AUTH_BACKEND.md`, `backend/BASE_DE_DATOS_PRIVADA_RLS.md` (sección 5) y `.claude/rules/SEGURIDAD_OWASP.md`.

## Solicitud de negocio

Sin registro público, alguien tiene que poder entrar la primera vez para administrar el sistema.

## Objetivo

Al arrancar sin administrador, se crea uno con las credenciales de entorno; con administrador existente no pasa nada.

## Alcance

- `features/usuarios/service/AdminInicialInitializer.java` (`ApplicationRunner`, idempotente) que usa un `JdbcTemplate`/`DataSource` con la conexión dueña (`spring.flyway.*`), porque no hay usuario autenticado y RLS está forzado.
- Lee `app.initial-admin.email` y `app.initial-admin.password` (`INITIAL_ADMIN_EMAIL`, `INITIAL_ADMIN_PASSWORD`, sin defaults). Valida correo con formato válido y contraseña de 8 a 72 bytes; si no hay admin y algo falla, lanza excepción y la app no arranca.
- Hashea con `BCryptPasswordEncoder`, inserta usuario `ACTIVO` + fila `usuarios_roles` con `ADMIN`, y registra un `INFO` sin la contraseña.
- Variables nuevas en `.env.example` (raíz y backend) con placeholder `CAMBIAME`, y pasarlas en ambos compose.
- Documentar en `docs/seguridad/RLS_EXCEPCIONES.md` que esta escritura usa la conexión dueña.

**Archivos esperados (crear/tocar):**

- `projects/backend/src/main/java/com/example/backend/features/usuarios/service/AdminInicialInitializer.java`
- `projects/backend/src/main/resources/application.properties`
- `.env.example`
- `projects/backend/.env.example`
- `docker-compose.yml`
- `docker-compose.prod.yml`
- `docs/seguridad/RLS_EXCEPCIONES.md`

## Fuera de alcance (no tocar)

- Cualquier endpoint de alta de usuarios.
- El `PasswordEncoder` como bean de seguridad (UT de HU-INICIAL-Login); aquí se instancia local o se declara el bean mínimo en `config/` sin tocar `SecurityConfig`.
- Cambio de contraseña del admin.

## Resultado esperado

Primer arranque crea el admin; segundo arranque no cambia nada; sin variables falla con mensaje claro.

## Validación

1. Arranque con base vacía y variables: `SELECT email FROM usuarios` muestra el admin y `password_hash` empieza con `$2`.
2. Reiniciar: el recuento de usuarios no cambia.
3. Arrancar sin `INITIAL_ADMIN_PASSWORD` con base vacía: la app falla con el mensaje esperado.
4. Revisar logs: no aparece la contraseña.

## Seguridad

A04/A09: BCrypt, contraseña nunca en logs; A02: sin credenciales por defecto.
