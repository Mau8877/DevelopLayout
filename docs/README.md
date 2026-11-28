# Documentación

Documentación oficial del proyecto. Si es la primera vez que usas la
plantilla, el [tutorial](TUTORIAL.md) recorre todo paso a paso; esta guía es el
checklist de puesta en marcha y mantenimiento.

| Documento | Para qué |
| --- | --- |
| [TUTORIAL.md](TUTORIAL.md) | Usar la plantilla de principio a fin |
| [INFRAESTRUCTURA.md](INFRAESTRUCTURA.md) | Docker, variables, producción y respaldos |
| [BASE_DE_DATOS.mermaid](BASE_DE_DATOS.mermaid) | Diagrama de la base |
| [errors/](errors/README.md) | Códigos de error de la API |
| [seguridad/RLS_EXCEPCIONES.md](seguridad/RLS_EXCEPCIONES.md) | Excepciones de seguridad en la base |
| [HistoriasDeUsuario/](HistoriasDeUsuario/) y [UnidadesTrabajo/](UnidadesTrabajo/) | Qué se construyó y cómo |
| [../scripts/README.md](../scripts/README.md) | Los scripts de la raíz |

## Poniendo en marcha un proyecto nuevo a partir de esta plantilla

Este repo es un esqueleto de arquitectura (Spring Boot + React/Vite + Flutter + Docker) con nombres genéricos (`app`, `com.example`, `mobile`). Un script los cambia todos por los de tu proyecto.

### 1. Copiá la plantilla y elegí los nombres

Partí de una copia recién clonada, **sin cambios sin commitear** (en GitHub: «Use this template»; o cloná y reemplazá el historial con `rm -rf .git && git init`, y hacé el primer commit).

| Dato | Ejemplo | Dónde se usa |
| --- | --- | --- |
| `--nombre` | `"Acme Tienda"` | Textos visibles de la web y la app, y el título de los README |
| `--slug` | `acme-tienda` | Contenedores, red, volúmenes, bucket, imágenes, artefacto Maven, base (`acme_tienda_db`) y paquete Dart (`acme_tienda_mobile`) |
| `--paquete` | `com.acme` | Paquete Java (`com.acme.backend`) e identificador de la app (`com.acme.mobile`) |

El paquete es tu dominio al revés, en minúsculas y **sin guiones ni guiones bajos** (iOS y Android no admiten lo mismo).

### 2. Ejecutá el inicializador

```bash
pnpm project:init --nombre "Acme Tienda" --slug acme-tienda --paquete com.acme --dry-run  # solo muestra los cambios
pnpm project:init --nombre "Acme Tienda" --slug acme-tienda --paquete com.acme
```

No necesita `pnpm install` (alcanza con Node y git: también sirve `node scripts/init-project.mjs`). Edita los archivos, mueve las carpetas de Java y de Kotlin y renombra la clase principal. Se usa **una sola vez**: se niega si ya se ejecutó o si hay cambios sin commitear. Revisá el resultado con `git status` y `git diff`; para deshacerlo, `git checkout -- . && git clean -fd`.

**A propósito, no cambia:**

- Ningún `.env` (tienen secretos). Se crean en el paso siguiente.
- El rol de base `app_runtime` ni las propiedades `app.*`: son nombres internos, y el rol está en migraciones.
- Las historias de usuario y sus tareas (`docs/HistoriasDeUsuario/`, `docs/UnidadesTrabajo/`), ni esta guía ni el [tutorial](TUTORIAL.md): describen la plantilla y cómo se construyó.

> Si en la misma máquina corrés varios proyectos de esta plantilla, usá un slug distinto en cada uno: los volúmenes llevan ese prefijo, y dos proyectos con el mismo comparten base de datos.

### 3. Preparar y levantar

```bash
pnpm install
pnpm setup:env   # crea los .env con secretos aleatorios (ver scripts/README.md)
# Cambiá INITIAL_ADMIN_EMAIL en .env y en projects/backend/.env por un correo real.
docker compose up
```

La web queda en http://localhost:5173 y el correo de prueba (invitaciones,
recuperación y avisos) en http://localhost:8025.

`MINIO_ROOT_USER` necesita al menos 3 caracteres y `MINIO_ROOT_PASSWORD`, 8 (los generados ya cumplen). Si ya tenés un Postgres en el puerto 5432, descomentá `POSTGRES_HOST_PORT`. Confirmá el backend con:

```bash
curl http://localhost:8080/health        # "status":"UP"
curl http://localhost:8080/prometheus | head   # métricas en texto plano
```

### 4. Lo que queda a mano

- [ ] Descripción de `README.md` y de `pubspec.yaml`, y el titular de `LICENSE`.
- [ ] Íconos y pantalla de inicio: `android/.../mipmap-*`, `ios/Runner/Assets.xcassets`, `macos/Runner/Assets.xcassets`, `web/icons` y `windows/runner/resources`.
- [ ] Solo si vas a publicar la app de escritorio: el nombre del ejecutable (`BINARY_NAME` en `linux/` y `windows/CMakeLists.txt`, `PRODUCT_NAME` en `macos/Runner/Configs/AppInfo.xcconfig` y los textos de `windows/runner/Runner.rc`).
- [ ] Colores de marca (`projects/frontend/src/index.css` y `projects/mobile/lib/core/theme/app_theme.dart`).
- [ ] Reemplazá las pantallas de ejemplo por las tuyas: `routes/index.tsx` y `_authenticated/dashboard.tsx` en la web (sin dejar `_authenticated/` sin ninguna ruta hija: el build falla) y `lib/features/health/` en mobile. En la app, los shells por rol traen secciones de ejemplo (por ejemplo «Pedidos»): cámbialas por las tuyas.
- [ ] Borrá la migración de ejemplo (`V1__init_schema.sql`) **antes** de crear tu primera base, si no la querés.

Después de renombrar el identificador de la app, corré `flutter pub get` y reabrí el proyecto en Android Studio o Xcode para que regeneren sus archivos.

El scaffold ya trae configurado, sin nada más que hacer:
- **Health check** en `GET /health` (Actuator, base-path movido a la raíz — no es `/actuator/health`).
- **Contrato de la API**: `docs/api/openapi.json` (snapshot versionado; ver `scripts/README.md`, `pnpm openapi:sync`).
- **Swagger UI** en `/swagger-ui/index.html` y OpenAPI JSON en `/v3/api-docs` (acceso directo por el puerto 8080, no a través del proxy). **Solo en desarrollo**: el perfil `dev` los enciende, y en cualquier otro perfil están apagados salvo que pongas `SWAGGER_ENABLED=true`.
- **Configuración segura por defecto**: sin credenciales por defecto fuera del perfil `dev`, sin `show-sql`, `/health` sin detalles, sin trazas de error hacia el cliente, y rate limit por IP sobre `/api/v1/**`. Lo cómodo para desarrollar vive solo en `application-dev.properties`.
- **Métricas Prometheus** en `GET /prometheus`, ya scrapeadas por el servicio `prometheus` de `docker-compose.yml` y con el datasource de Grafana provisionado.
- **CORS** habilitado vía `SecurityConfig` para los orígenes en `app.cors.allowed-origins` (property `CORS_ALLOWED_ORIGINS`).
- **Cuentas completas:** inicio de sesión con bloqueo por intentos, renovación, cierre de sesión, recuperación y cambio de contraseña con aviso por correo, usuarios con invitación, roles, sesiones y bitácora.
- **Español e inglés** en web, app y respuestas y correos del backend.
- **Pruebas** de backend, web, mobile, scripts, navegador (`pnpm test:e2e`) y producción (`pnpm test:prod`).

Si más adelante exponés otros endpoints de Actuator (`/metrics`, `/env`, `/beans`, etc.), agregalos a `management.endpoints.web.exposure.include` y decidí con cuidado si van en `permitAll()` en `SecurityConfig` — quedarían públicos sin autenticación si los dejás ahí sin querer.

## Actualizando versiones (Spring Boot, React, Flutter, Docker)

Importante: la plantilla (`DevelopLayout`) y un proyecto ya generado a partir de ella (ej. renombrado a `Acme`) **no están enlazados**. Una vez que copiás/renombrás el template quedan como repos independientes — actualizar `DevelopLayout` después no le llega solo a un proyecto ya creado. Hay que aplicar el mismo cambio a mano (o vía diff/cherry-pick) en cada proyecto que lo necesite.

### Actualizar un major de framework (poco frecuente)

| Stack | Dónde está pineado | Cómo se actualiza |
|---|---|---|
| Spring Boot | `projects/backend/pom.xml`: `spring-boot-starter-parent` y `java.version` | Subís el `<version>` del parent (y `java.version` si el major lo pide), corrés `mvn dependency:resolve`, y revisás el changelog de Spring Boot por breaking changes. Si subís `java.version`, subí también las imágenes `maven:3.9-eclipse-temurin-21-alpine` (en `docker-compose.yml` y en `projects/backend/Dockerfile`) y `eclipse-temurin:21-jre-alpine` (runtime del `Dockerfile`) para que coincidan. JasperReports se versiona con la property `jasperreports.version` del `pom.xml` y se mantiene en la última disponible. |
| React/Vite | `projects/frontend/package.json`, lockfile en [pnpm-lock.yaml](../pnpm-lock.yaml) (raíz, workspace pnpm) | `pnpm up -r` (dentro de rango semver) o `pnpm up -r --latest` para saltar de major; después corré lint, tests y build, porque un major puede romper (ej. TanStack Table v9 cambió su API). Como el lockfile vive en la raíz del monorepo, corré los comandos ahí, no dentro de `projects/frontend`. TypeScript se mantiene en 6.x hasta que `typescript-eslint` soporte la 7. Las dependencias transitivas vulnerables se fuerzan con `overrides` en `pnpm-workspace.yaml`. |
| Flutter | `sdk: ^3.12.2` en `projects/mobile/pubspec.yaml` | Cambiás el SDK (con `fvm` o el instalador de Flutter) y corrés `flutter pub upgrade --major-versions`. `pubspec.lock` sí se commitea, así que el build es reproducible entre máquinas; tras actualizar, commiteá el lock nuevo. |
| Infra Docker | Tags de imagen en `docker-compose.yml` (`postgres:18-alpine`, `redis:8-alpine`, `node:22-alpine`, etc.) | Cambiás el tag en `docker-compose.yml` **y en `docker-compose.prod.yml`** y corrés `docker compose pull && docker compose up`. Todas las imágenes llevan tag fijo (`prometheus:v3.14.0`, `grafana:13.3.0`, etc.); la excepción es MinIO, que usa `latest` en desarrollo y exige un tag o digest fijo en producción (`MINIO_IMAGE`). |

### Actualizar librerías de rutina (parches/minor) en un proyecto ya generado

- **Backend**: `./mvnw versions:display-dependency-updates` lista qué dependencias tienen versión nueva (funciona sin declarar el plugin en el `pom.xml`).
- **Frontend**: `pnpm outdated -r` y `pnpm up -r` (respeta los rangos del `package.json`); para saltar de major, `pnpm up --latest` paquete por paquete.
- **Mobile**: `flutter pub outdated` y `flutter pub upgrade`.

Estos comandos se corren en el proyecto ya renombrado, no en la plantilla — salvo que quieras que el *próximo* proyecto que arranques desde cero ya venga con esas versiones más nuevas, en cuyo caso el cambio va en `DevelopLayout`.

### Todo junto: scripts y Dependabot

- `scripts/windows/update-deps.ps1` y `scripts/linux/update-deps.sh` actualizan y verifican los tres proyectos de una vez (dependencias, `pnpm audit`, lint, tests, build, `flutter analyze`, `mvnw verify`) y escriben todo en `update-deps.log` en la raíz (ignorado por git). Detalle en [scripts/README.md](../scripts/README.md).
- `.github/dependabot.yml` abre una PR semanal por ecosistema (Maven, npm, pub, Docker y GitHub Actions).

## Seguridad, CI y producción

- **Reglas de seguridad** (OWASP Top 10, API Security Top 10, MASVS, base de datos privada con RLS): [`.claude/rules/SEGURIDAD_OWASP.md`](../.claude/rules/SEGURIDAD_OWASP.md) y [`.claude/rules/backend/BASE_DE_DATOS_PRIVADA_RLS.md`](../.claude/rules/backend/BASE_DE_DATOS_PRIVADA_RLS.md). Toda implementación nueva tiene que cumplirlas.
- **CI** (`.github/workflows/ci.yml`, en cada Pull Request y push a `main`): lint + tests + build por proyecto, pruebas de scripts y de navegador, `gitleaks`, Trivy, `pnpm audit`, y la construcción y prueba de las imágenes de producción. Detalle en [`.claude/rules/CI_CD.md`](../.claude/rules/CI_CD.md).
- **Producción**: `docker-compose.prod.yml` (HTTPS obligatorio, solo el proxy publica puertos, contenedores sin root). Variables extra, certificados y cómo levantarlo en [INFRAESTRUCTURA.md](INFRAESTRUCTURA.md#7c-docker-composeprodyml). No hay CD todavía: depende de decidir el destino del despliegue.
- **Errores de la API**: el catálogo de códigos vive en [`docs/errors/`](errors/README.md).
