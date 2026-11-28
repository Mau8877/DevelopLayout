---
globs: projects/backend/**/*
---

# Testing — Backend

## Foco

Los tests se concentran en **`service`** (lógica de negocio) y **`controller`**
(contrato HTTP: status codes, forma del `ApiResponse`, validación de
input). No se exige cobertura exhaustiva de mappers triviales, DTOs o
getters/setters generados.

## Starters ya instalados — usar el que corresponde

El proyecto ya trae los starters de test de Spring Boot 4 en `pom.xml`; cada
tipo de test usa el que le corresponde, no todos en todos lados:

| Qué se testea | Starter / herramienta |
| --- | --- |
| Controllers (contrato HTTP, `MockMvc`) | `spring-boot-starter-webmvc-test` |
| Repositorios/queries JPA | `spring-boot-starter-data-jpa-test` (`@DataJpaTest`) |
| Endpoints protegidos / reglas de `SecurityConfig` | `spring-boot-starter-security-test` |
| Migraciones Flyway | `spring-boot-starter-flyway-test` |
| Validaciones Bean Validation en DTOs | `spring-boot-starter-validation-test` |
| Health/actuator | `spring-boot-starter-actuator-test` |

## Regla estricta: tests al día

Todo cambio de código que altera un comportamiento existente **debe**
actualizar sus tests correspondientes en el mismo cambio. No se permite:

- Dejar un test en rojo "para arreglar después".
- Dejar un test verde pero que ya no verifica lo que el código realmente
  hace (test desactualizado que pasa por casualidad).
- Comentar/skippear (`@Disabled`) un test para que el build pase, sin
  justificación explícita y un plan concreto de cuándo se re-habilita.

## Tests de integración con BD real: Testcontainers

Para tests que necesitan una base de datos real (no un mock del
repositorio) se usa **Testcontainers** con PostgreSQL — el mismo motor que
`docker-compose.yml` usa en desarrollo (`postgres:18-alpine`), para que el
comportamiento en test sea fiel al de verdad (constraints, tipos de dato,
etc.).

**Ya agregado** en `pom.xml`: `org.springframework.boot:spring-boot-testcontainers`,
`org.testcontainers:testcontainers-junit-jupiter` y
`org.testcontainers:testcontainers-postgresql` (Testcontainers **2.x**: los
artefactos se renombraron con prefijo `testcontainers-` y la clase es
`org.testcontainers.postgresql.PostgreSQLContainer`, ya sin genéricos). Sin
`<version>`: la administra el BOM de Spring Boot (4.2 importa
`testcontainers-bom`).

- Se reservan para flujos donde el mock de repositorio no es suficiente
  (queries complejas, constraints de BD, comportamiento de Flyway). Para
  lógica de negocio pura en el `service`, se sigue mockeando el repositorio
  (`@Mock`/`@MockBean`) — no todo test pasa a ser de integración por default.
- **Prerequisito de infra**: Testcontainers necesita hablar con el daemon de
  Docker desde DENTRO del contenedor del backend — `docker-compose.yml`
  monta `/var/run/docker.sock:/var/run/docker.sock` en el servicio
  `backend` para esto.
- **Tests con `@SpringBootTest`** (contexto completo): extienden
  `support/PostgresIntegrationTest`, que levanta un Postgres descartable con
  Testcontainers y pisa las propiedades de datasource/Flyway con
  `@DynamicPropertySource`. Nunca dependen de un Postgres instalado en la
  máquina ni de credenciales del entorno. Sin Docker, quedan SKIPPED
  (`@Testcontainers(disabledWithoutDocker = true)`).
- **Patrón obligatorio para tests que usan Testcontainers a mano**: no manejar el
  container con `@Testcontainers`/`@Container` (arranca automático y sin
  forma limpia de saltear el test si Docker no está disponible). En cambio,
  chequear disponibilidad en un `@BeforeAll` con
  `DockerClientFactory.instance().isDockerAvailable()` +
  `Assumptions.assumeTrue(...)`, y manejar el container a mano
  (try-with-resources) dentro del test. Así, si Docker no está accesible, el
  test queda **SKIPPED** (no rompe el build) en vez de fallar — ver
  `TestcontainersSmokeTest.java` como referencia.

> **Docker Desktop en Windows, nota conocida:** montar
> `/var/run/docker.sock` no alcanza siempre — Docker Desktop puede exponer
> un socket restringido/"stub" que responde a `/info` con casi todos los
> campos vacíos (`"ID":""`, `"ServerVersion":""`, etc.) en vez del daemon
> real, y Testcontainers falla con "Could not find a valid Docker
> environment" aunque el mount esté bien hecho. Hay que revisar, en la
> máquina host, **Docker Desktop → Settings → Advanced → "Allow the default
> Docker socket to be used"** (tiene que estar tildado). Confirmado en esta
> sesión: con el mount + `DOCKER_HOST=unix:///var/run/docker.sock` +
> `TESTCONTAINERS_RYUK_DISABLED=true` en el `environment` del servicio
> `backend`, seguía sin conectar — es un tema de configuración de Docker
> Desktop en el host, no del código ni de `docker-compose.yml`. Por eso el
> patrón de `assumeTrue` de arriba es obligatorio: un dev sin ese setting
> tildado sigue pudiendo correr `mvn test` sin que le rompa el build.

## Bases de los tests de integración

- `support/PostgresIntegrationTest`: Postgres descartable, un solo rol.
- `support/PostgresRuntimeIntegrationTest`: Postgres y Redis descartables, con
  la app conectada como `app_runtime` (RLS aplica de verdad). Es la base para
  todo flujo con datos o autenticación.
- **CSRF se prueba contra un servidor real** (`WebEnvironment.RANDOM_PORT`, ver
  `AuthCsrfCookieTest`): MockMvc sustituye el repositorio de CSRF y dejó pasar
  un defecto que borraba la cookie `XSRF-TOKEN`.

## Carreras y concurrencia

Todo flujo en el que dos peticiones puedan pisarse (cambiar una contraseña,
cerrar sesiones, consumir un token) lleva una prueba que **fuerza el cruce**, no
que confíe en la suerte: se detiene un paso intermedio con un `@MockitoSpyBean`
(por ejemplo `PasswordEncoder`, que corre entre las dos transacciones) y una
barrera o un `CountDownLatch`. Referencia: `CambioPasswordConcurrenciaTest`.

Si el flujo toma conexiones, se suma un caso a `AuthPoolTest` (pool de 3).

Una prueba que lee «el último correo» filtra por lo que busca: los avisos se
envían en segundo plano y pueden colarse.

## Niveles de log en una prueba

El nivel de un logger es de toda la JVM. Una prueba que lo cambia lo hace en
`@BeforeEach` y lo **restaura** en `@AfterEach`; con `logging.level.*` como
propiedad quedaría activo para las pruebas siguientes (ver `SecretosEnLogsTest`).

## Convenciones

- Seguridad (obligatorio, ver [SEGURIDAD_OWASP.md](../SEGURIDAD_OWASP.md)):
  todo endpoint nuevo lleva tests de autorización — acceso permitido,
  **sin token (401)**, **rol insuficiente (403)** y **recurso de otro usuario
  (404/403)**. Las tablas con RLS se prueban con el rol `app_runtime` (ver
  [BASE_DE_DATOS_PRIVADA_RLS.md](BASE_DE_DATOS_PRIVADA_RLS.md)).
- Clases de test: `XxxServiceTest`, `XxxControllerTest` (sufijo `Test`, no
  `Tests` ni `Spec`). Una base abstracta común puede llamarse
  `XxxIntegrationTest`.
- Un límite por IP o por cuenta se comparte entre las pruebas del mismo
  contexto: cada prueba usa su propia IP y crea sus propias cuentas.
- Nombre de métodos: describen condición + resultado esperado, ej.
  `crearUsuario_conEmailDuplicado_lanzaConflictException`.
- Los tests de controller verifican también la forma del `ApiResponse`
  (`status`, `data`, `error` cuando corresponde) — no solo el status code
  HTTP. Un test que solo chequea `status().isOk()` sin mirar el body no
  cubre el contrato real de [RESPONSES_BACKEND.md](RESPONSES_BACKEND.md).
