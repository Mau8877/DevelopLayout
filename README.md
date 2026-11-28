# DevelopLayout

Plantilla base de arquitectura full-stack. Monorepo con tres proyectos
independientes (backend, frontend, mobile) más la infraestructura de
desarrollo que los conecta a todos.

Trae resuelto lo que casi todo proyecto necesita: inicio de sesión y
renovación, recuperación y cambio de contraseña, usuarios con invitación por
correo, roles, sesiones, bitácora, español e inglés, y las pruebas y el CI que
lo comprueban.

> **¿Primera vez?** Empieza por el [tutorial](docs/TUTORIAL.md): crear tu
> proyecto, levantarlo, recorrerlo y agregar tu primera función.

## Stack

| Proyecto | Tecnología |
| --- | --- |
| **Backend** (`projects/backend`) | Java 21 + Spring Boot 4.2 (Maven), PostgreSQL + Flyway, Redis (caché), MinIO (preparado para archivos; todavía sin código que lo use), JasperReports 7 (preparado para reportes) |
| **Frontend** (`projects/frontend`) | React 19 + TypeScript + Vite, TanStack (Query/Router/Form/Table), Tailwind CSS 4 + shadcn, i18next; Playwright para las pruebas de navegador |
| **Mobile** (`projects/mobile`) | Flutter/Dart, Riverpod (estado), `go_router` (navegación), `dio` (HTTP) |
| **Infraestructura** (`infrastructure/`) | Docker Compose (desarrollo y producción): nginx (proxy), postgres, redis, minio, prometheus, grafana y, en desarrollo, Mailpit (correo de prueba) |

`pnpm-workspace.yaml` maneja `projects/*` como un workspace de pnpm
(el paquete de Node es el frontend; el backend (Maven) y la app (Flutter)
viven en el mismo repo con sus propias herramientas). En la raíz están los
hooks de Git y los comandos comunes (`package.json`).

## Estructura del repo

```
DevelopLayout/
├── projects/
│   ├── backend/       # API REST (Spring Boot)
│   ├── frontend/       # Panel web (React)
│   └── mobile/         # App móvil (Flutter)
├── infrastructure/      # Config de nginx, postgres, redis, minio, prometheus, grafana
├── scripts/             # Inicializador, .env, respaldos, contrato, pruebas de navegador y de producción, update-deps
├── .github/             # CI (ci.yml) y Dependabot
├── docs/
│   ├── TUTORIAL.md      # Cómo usar la plantilla, de principio a fin
│   ├── README.md        # Checklist de puesta en marcha de un proyecto nuevo a partir de esta plantilla
│   ├── INFRAESTRUCTURA.md  # Guía completa de la infraestructura Docker (servicios, volúmenes, variables de entorno)
│   ├── errors/           # Catálogo de códigos de error de la API (ver más abajo)
│   ├── api/              # Contrato OpenAPI versionado
│   ├── BASE_DE_DATOS.mermaid  # Diagrama de la base
│   ├── seguridad/        # Excepciones documentadas a las reglas de seguridad (RLS)
│   ├── HistoriasDeUsuario/
│   └── UnidadesTrabajo/
├── .claude/
│   ├── CLAUDE.md         # Instrucciones de proyecto para Claude Code
│   └── rules/            # Reglas de arquitectura y convenciones (ver más abajo)
├── docker-compose.yml        # Levanta el stack completo para desarrollo
└── docker-compose.prod.yml   # Base de producción (TLS, sin docker.sock, solo el proxy publica puertos)
```

## Puesta en marcha rápida

Requisitos: Docker Desktop, Node 22 o superior y pnpm 11. Para trabajar fuera
de Docker, además JDK 21 y Flutter 3.44.8. Desde la raíz del repo:

```bash
pnpm install
pnpm setup:env    # crea los .env con secretos aleatorios
# Cambia INITIAL_ADMIN_EMAIL en .env y en projects/backend/.env.
docker compose up
```

Entra a http://localhost:5173 con `INITIAL_ADMIN_EMAIL` y el
`INITIAL_ADMIN_PASSWORD` que quedó en `.env`.

| Servicio | URL o puerto |
| --- | --- |
| Web (Vite) | http://localhost:5173 |
| Proxy (nginx) | http://localhost |
| API | http://localhost:8080 (Swagger en `/swagger-ui/index.html`, solo en desarrollo) |
| Correo de prueba (Mailpit) | http://localhost:8025 (SMTP en `1025`) |
| MinIO (API / consola) | `9000` / http://localhost:9001 |
| Prometheus / Grafana | http://localhost:9090 / http://localhost:3000 |
| PostgreSQL / Redis | `5432` (configurable con `POSTGRES_HOST_PORT`) / `6379` |

Todos los puertos se publican solo en `127.0.0.1`. Para comprobar el backend:
`curl http://localhost:8080/health`.

Para ejecutar cada proyecto fuera de Docker (el backend necesita que le pases
las variables de su `.env`), ver el
[tutorial](docs/TUTORIAL.md#6-el-día-a-día).

### Comandos

| Dónde | Comando | Qué hace |
| --- | --- | --- |
| Raíz | `pnpm test:e2e` | Pruebas de navegador en un entorno desechable |
| Raíz | `pnpm test:prod` | Levanta producción con datos desechables y la comprueba |
| Raíz | `pnpm test:scripts` | Pruebas de los scripts |
| `projects/backend` | `./mvnw test` · `./mvnw checkstyle:check` | Pruebas y estilo |
| `projects/frontend` | `pnpm test` · `pnpm lint` · `pnpm build` | Pruebas, lint y compilación |
| `projects/mobile` | `flutter test` · `flutter analyze` | Pruebas y análisis |

El resto (inicializador, `.env`, respaldos y contrato de la API) está en
[`scripts/README.md`](scripts/README.md).

## Arquitectura

Cada proyecto sigue una organización **feature-based**: el código se
agrupa por funcionalidad de negocio (`features/{modulo}/` en los tres
proyectos), no por tipo técnico — así todo lo relacionado a un mismo tema
(ej. "usuarios") vive junto, y agregar un módulo nuevo no obliga a tocar
carpetas transversales compartidas por toda la app.

**Backend** — flujo de una request:

```
Cliente (frontend/mobile)
   │  HTTP + JSON
   ▼
Controller   -- valida el input, delega a UN método de service, envuelve la respuesta
   ▼
Service       -- lógica de negocio, transacciones, logging
   ▼
Repository     -- acceso a datos (Spring Data JPA)
   ▼
PostgreSQL
```

Toda la API responde con el mismo sobre JSON
(`status`/`data`/`message`/`timestamp`/`error`/`meta`), y todo error de
negocio catalogado trae un código fijo (`ERR_{MODULO}_{NUMERO}`) además
del mensaje, que llega en español o inglés según `Accept-Language` — ver
[docs/errors/](docs/errors/README.md) para el listado completo de códigos que
la API puede devolver hoy.

**Frontend** — Zustand para estado de UI, TanStack Query para todo lo que
viene del backend (nunca mezclados), TanStack Router con file-based
routing y un guard único para rutas autenticadas.

**Mobile** — mismo criterio conceptual que el frontend, pero con Riverpod
para ambos casos (datos de API y estado de UI), distinguidos por
convención de nombre (`{feature}ListProvider` vs. `{feature}UiProvider`).

**Idiomas** — ningún texto visible va escrito en el código: la web, la app y
el backend leen archivos de idioma por módulo, en español e inglés neutros.
Las URL van en inglés. Ver [`IDIOMAS.md`](.claude/rules/IDIOMAS.md).

## Documentación y reglas del proyecto

Las decisiones de arquitectura y las convenciones de código **ya están
tomadas** y viven en `.claude/rules/`, organizadas por proyecto
(`backend/`, `frontend/`, `mobile/`) más las transversales a todo el repo
en la raíz de esa carpeta (convenciones de Git, estándares de código
comunes a los tres lenguajes). No son sugerencias — son la fuente de
verdad de cómo se construye este proyecto específico, y priman sobre
cualquier criterio genérico de "buenas prácticas" cuando entran en
conflicto.

Puntos de entrada útiles:

- [`.claude/CLAUDE.md`](.claude/CLAUDE.md) — resumen del proyecto y cómo
  se trabaja acá (incluye las convenciones de commits de Git).
- [`.claude/rules/CONVENCIONES_GIT.md`](.claude/rules/CONVENCIONES_GIT.md) —
  formato de mensajes de commit (`tipo[SCOPE]: descripción`) y ramas.
- [`.claude/rules/ESTANDARES_CODIGO.md`](.claude/rules/ESTANDARES_CODIGO.md) —
  nombrado y comentarios, comunes a Java/TypeScript/Dart.
- [`.claude/rules/SEGURIDAD_OWASP.md`](.claude/rules/SEGURIDAD_OWASP.md) —
  OWASP Top 10, API Security Top 10 y MASVS: toda implementación nueva
  tiene que cumplirlos. La base de datos es siempre privada y con RLS
  ([`BASE_DE_DATOS_PRIVADA_RLS.md`](.claude/rules/backend/BASE_DE_DATOS_PRIVADA_RLS.md)).
- [`.claude/rules/CI_CD.md`](.claude/rules/CI_CD.md) — qué hace el CI,
  los controles de seguridad y por qué todavía no hay CD.
- [`docs/errors/README.md`](docs/errors/README.md) — catálogo vivo de
  todos los códigos de error que la API puede devolver, con su HTTP, su
  significado y dónde se lanza cada uno.
- [`docs/README.md`](docs/README.md) — checklist para poner en marcha un
  proyecto nuevo a partir de esta misma plantilla (renombrar paquetes,
  slugs, bundle IDs, etc.).
- [`docs/INFRAESTRUCTURA.md`](docs/INFRAESTRUCTURA.md) — guía completa de
  los servicios, redes, volúmenes y variables de entorno de
  `docker-compose.yml`, y cómo levantar `docker-compose.prod.yml`.
- [`docs/TUTORIAL.md`](docs/TUTORIAL.md) — cómo usar la plantilla, de crear
  el proyecto a agregar una función.
- [`scripts/README.md`](scripts/README.md) — todos los scripts de la raíz.

## Convenciones de Git (resumen)

Formato de commit obligatorio, validado automáticamente por `commitlint`
en cada commit:

```
tipo[SCOPE]: descripción concreta
```

- **Tipos**: `feat`, `fix`, `refactor`, `docs`, `test`, `chore`, `style`,
  `perf`, `build`, `ci` (Conventional Commits).
- **Scopes**: `BACK`, `FRONT`, `MOBILE`, `INFRA`, `DOCS`, `REPO` — un
  commit nunca toca más de un scope a la vez.

Detalle completo en
[`.claude/rules/CONVENCIONES_GIT.md`](.claude/rules/CONVENCIONES_GIT.md).

## CI/CD

Cada Pull Request contra `main` (y cada push directo a `main`) dispara
[`.github/workflows/ci.yml`](.github/workflows/ci.yml). Un push a otra rama no
lo dispara. Los jobs `backend`, `frontend`, `mobile`, `scripts` y `e2e`
(pruebas de navegador) corren solo si cambió algo que los afecta; `security`
(`gitleaks` + `Trivy`) corre siempre; `docker`, si cambió el backend, la web o
algo de producción, construye y escanea las imágenes y levanta el stack
completo. Dependabot abre PRs semanales de
actualización.

Todavía no hay CD (despliegue automático) — depende de una decisión de
infraestructura pendiente (dónde se aloja el backend/frontend en
producción). El detalle completo, incluyendo cómo se vería CD una vez que
exista ese destino, está en
[`.claude/rules/CI_CD.md`](.claude/rules/CI_CD.md).

## Calidad automática (pre-commit)

`husky` corre `lint-staged` en cada `git commit`, que aplica el linter que
corresponde según qué archivos quedaron staged:

| Lenguaje | Herramienta |
| --- | --- |
| TypeScript (frontend) | ESLint (`--fix`) |
| Java (backend) | Checkstyle |
| Dart (mobile) | `flutter analyze` |

El pre-commit solo lintea — correr la suite de tests de cada proyecto
(`pnpm test`, `./mvnw test`, `flutter test`) antes de commitear queda a
criterio de cada dev.
