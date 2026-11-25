# Documentación

Documentación oficial del proyecto.

## Poniendo en marcha un proyecto nuevo a partir de esta plantilla

Este repo es un esqueleto de arquitectura (Spring Boot + React/Vite + Flutter + Docker). Todos los identificadores están puestos como placeholder genérico (`app`, `com.example`, etc.) para que se reemplacen por el nombre real del proyecto nuevo. Sigue esta lista en orden.

### 1. Elegí el nombre del proyecto

Antes de tocar nada, definí:

- **Nombre del proyecto** (ej: `Acme`) → se usa en textos y README.
- **Slug en minúsculas con guiones** (ej: `acme`) → se usa en nombres de contenedores, red, artefactos.
- **Paquete reverso de dominio** (ej: `com.acme`) → se usa en Java (backend) y en bundle IDs (mobile).

En los ejemplos de abajo se usa `acme` / `com.acme` como referencia.

### 2. Raíz del repo

- [ ] `README.md`: cambiá el título y la descripción.
- [ ] `package.json`: cambiá `"name": "developlayout"` por el slug del proyecto.
- [ ] `docker-compose.yml` **y `docker-compose.prod.yml`**: reemplazá todas las ocurrencias de `app` por el slug elegido (en ambos archivos, con los mismos nombres de contenedor, porque `prometheus.yml` y los dos `nginx*.conf` los referencian):
  - `name: app` / `name: app-prod` (nombre del stack de compose)
  - `container_name: app-*` (proxy, frontend, backend, postgres, redis, minio, minio-init, prometheus, grafana)
  - `networks: app-network` / `app-prod-network`
  - `volumes: app-postgres_data`, `app-redis_data`, `app-minio_data` (y sus equivalentes `app-prod-*` en el compose de producción) — **importante:** si esta máquina corre varios proyectos hechos con esta plantilla, no dejes estos nombres sin el prefijo del proyecto. Docker Compose solo corre la inicialización de Postgres (crear la DB/usuario del `.env`) la primera vez que un volumen está vacío; si dos proyectos comparten el mismo nombre de volumen, el segundo hereda los datos del primero y su base de datos nunca se crea.
- [ ] `.env.example` (raíz): actualizá `POSTGRES_DB` y `POSTGRES_USER` con el nuevo slug, y generá tu propio `.env` real (no se commitea) reemplazando **cada** `CAMBIAME` por una contraseña propia (`openssl rand -base64 24`): `POSTGRES_PASSWORD`, `REDIS_PASSWORD`, `GRAFANA_ADMIN_PASSWORD` y `MINIO_ROOT_PASSWORD`. Ninguna tiene valor por defecto: `docker compose` falla si falta alguna. `MINIO_ROOT_USER` necesita mínimo 3 caracteres y `MINIO_ROOT_PASSWORD` mínimo 8 — si son más cortos, el contenedor de MinIO termina en `exited (1)` y todo lo que depende de él (`minio-init`, `backend`) nunca arranca. Si ya tenés un Postgres en el puerto 5432 de tu máquina, descomentá `POSTGRES_HOST_PORT` (ej. `5433`) para publicar el del contenedor en otro puerto del host.

### 3. Backend (Spring Boot)

- [ ] Renombrá el paquete Java `com.example.backend` → `com.acme.backend` en:
  - `projects/backend/src/main/java/com/example/...` (mover **todo** el árbol de carpetas, incluidas las que solo tienen un `.gitkeep` como `common/constant`, `common/util` y `features` — no alcanza con buscar solo archivos `.java`, esas carpetas vacías también hay que moverlas)
  - `projects/backend/src/test/java/com/example/...` (mover carpetas)
  - El `package` / `import` dentro de cada `.java`
- [ ] Renombrá la clase principal `AppBackendApplication` (y su test) por el nombre que prefieras (ej: `AcmeBackendApplication`).
- [ ] `pom.xml`: `groupId` (`com.example` → `com.acme`) y `artifactId` (`app-backend` → `acme-backend`).
- [ ] `application.properties`:
  - `spring.application.name`
  - URL de datasource por defecto (`app_db` → tu nombre de base de datos; el usuario y la contraseña no tienen default, vienen de `.env`)
  - namespace `app.minio.*` (y actualizá el prefijo si cambiás el nombre)
  - `app.minio.bucket-name`
- [ ] `projects/backend/.env.example`: mismo ajuste de `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`, y una contraseña real en `SPRING_DATA_REDIS_PASSWORD` (la misma que `REDIS_PASSWORD` de la raíz). `CORS_ALLOWED_ORIGINS` ya trae por defecto los orígenes del frontend en desarrollo (`http://localhost:5173` directo, `http://localhost` vía el proxy nginx) — solo hace falta tocarlo si agregás otros orígenes (otro puerto, un dominio de producción, etc.).
- [ ] `src/main/resources/db/migration/V1__init_schema.sql`: cambiá el valor `'App'` de `project_name` (o borrá esa migración de ejemplo cuando tengas tu esquema real).

El scaffold ya trae configurado, sin nada más que hacer:
- **Health check** en `GET /health` (Actuator, base-path movido a la raíz — no es `/actuator/health`).
- **Contrato de la API**: `docs/api/openapi.json` (snapshot versionado; ver `scripts/README.md`, `pnpm openapi:sync`).
- **Swagger UI** en `/swagger-ui/index.html` y OpenAPI JSON en `/v3/api-docs` (acceso directo por el puerto 8080, no a través del proxy). **Solo en desarrollo**: el perfil `dev` los enciende, y en cualquier otro perfil están apagados salvo que pongas `SWAGGER_ENABLED=true`.
- **Configuración segura por defecto**: sin credenciales por defecto fuera del perfil `dev`, sin `show-sql`, `/health` sin detalles, sin trazas de error hacia el cliente, y rate limit por IP sobre `/api/v1/**`. Lo cómodo para desarrollar vive solo en `application-dev.properties`.
- **Métricas Prometheus** en `GET /prometheus`, ya scrapeadas por el servicio `prometheus` de `docker-compose.yml` y con el datasource de Grafana provisionado.
- **CORS** habilitado vía `SecurityConfig` para los orígenes en `app.cors.allowed-origins` (property `CORS_ALLOWED_ORIGINS`).

Si más adelante exponés otros endpoints de Actuator (`/metrics`, `/env`, `/beans`, etc.), agregalos a `management.endpoints.web.exposure.include` y decidí con cuidado si van en `permitAll()` en `SecurityConfig` — quedarían públicos sin autenticación si los dejás ahí sin querer.

### 4. Frontend (React + Vite)

- [ ] `projects/frontend/package.json`: `"name": "frontend"` → el nombre que quieras.
- [ ] `projects/frontend/index.html`: `<title>frontend</title>`.
- [ ] `projects/frontend/src/routes/`: el scaffold trae `index.tsx`, `login.tsx` y `_authenticated/panel.tsx` como placeholders mínimos (no hay `App.tsx`). Reemplazá `panel.tsx` por tu primera pantalla real, sin dejar `_authenticated/` sin ninguna ruta hija: el build falla (ver `.claude/rules/frontend/RUTAS_NAVEGACION_FRONTEND.md`).
- [ ] `projects/frontend/.env.example`: revisá `VITE_API_BASE_URL` si cambiás rutas/puertos. Es solo para desarrollo: la imagen de producción lo fija al construir (`--build-arg`, por defecto `/api/v1`, relativa porque frontend y API comparten origen detrás del proxy).

### 5. Mobile (Flutter)

- [ ] `projects/mobile/pubspec.yaml`: `name: mobile` y `description`.
- [ ] Bundle ID / application ID `com.example.mobile` → `com.acme.mobile` en:
  - `android/app/build.gradle.kts` (`namespace`, `applicationId`)
  - `android/app/src/main/kotlin/com/example/mobile/MainActivity.kt` (mover el paquete y actualizar el `package` declarado)
  - `ios/Runner.xcodeproj/project.pbxproj` (`PRODUCT_BUNDLE_IDENTIFIER`, todas las ocurrencias)
  - `macos/Runner.xcodeproj/project.pbxproj` y `macos/Runner/Configs/AppInfo.xcconfig` (`PRODUCT_BUNDLE_IDENTIFIER`, `PRODUCT_NAME`, `PRODUCT_COPYRIGHT`)
  - `linux/CMakeLists.txt` (`APPLICATION_ID`)
  - `windows/runner/Runner.rc` (`CompanyName`, `LegalCopyright`)
- [ ] Íconos y splash (`android/.../mipmap-*`, `ios/Runner/Assets.xcassets`, `macos/Runner/Assets.xcassets`, `web/icons`, `windows/runner/resources`): reemplazalos por los del proyecto real.
- [ ] `lib/core/network/` (cliente `dio` con el interceptor del sobre de la API) y `lib/core/theme/` ya están implementados, y `lib/features/health/` es un chequeo de conectividad de ejemplo que se borra o reemplaza cuando haya una feature real. `lib/core/routing/` y `lib/features/auth/` están vacíos a propósito: se arman junto con la autenticación (ver `.claude/rules/ARQUITECTURA_MOBILE.md`).
- [ ] `.env` de mobile: `pubspec.yaml` lo declara como asset y no se commitea, así que creá el tuyo con `cp projects/mobile/.env.example projects/mobile/.env`; sin él `flutter analyze` falla con `asset_does_not_exist`.
- [ ] Todos los `import 'package:mobile/...'` de `lib/` y `test/` (por ejemplo `test/widget_test.dart` importa `package:mobile/core/theme/app_theme.dart`) — actualizalos al nuevo nombre de paquete (`name:` de `pubspec.yaml`), por ejemplo `package:acme_mobile/...`. En Dart el nombre del import depende exclusivamente de ese `name:`, así que si no lo actualizás `flutter analyze`/`flutter test` van a fallar con `uri_does_not_exist`.

> Nota: después de renombrar el bundle ID, corré `flutter pub get` y, si usás Android Studio/Xcode, reabrí el proyecto para que regenere los archivos `ephemeral`/`Generated.xcconfig` (se excluyen del repo a propósito, son generados).

### 6. Infraestructura Docker

- [ ] `infrastructure/nginx/nginx.conf` **y `nginx.prod.conf`**: los `proxy_pass` apuntan a `app-frontend` / `app-backend` — actualizá si cambiaste esos nombres de contenedor.
- [ ] `infrastructure/prometheus/prometheus.yml`: `job_name` y `targets: ['app-backend:8080']` (el `metrics_path: '/prometheus'` no lleva el slug, no hace falta tocarlo).
- [ ] `infrastructure/grafana/provisioning/datasources/datasource.yml`: `url: http://app-prometheus:9090`.
- [ ] `infrastructure/minio/create-buckets.sh`: nombre del bucket `app-storage` (tiene que coincidir con `app.minio.bucket-name` de `application.properties`).

### 7. Verificación final

Buscá que no quede ningún rastro del placeholder:

```bash
grep -ril "com\.example\|app-network\|app-prod\|app_db\|app-backend\|app-frontend\|app-storage\|app-postgres_data\|app-redis_data\|app-minio_data" . --exclude-dir=.git --exclude-dir=node_modules --exclude-dir=target
```

Si el comando no devuelve nada, ya no queda nada del template. Después corré:

```bash
pnpm install
pnpm setup:env   # crea los .env con secretos aleatorios (ver scripts/README.md)
# Cambiá INITIAL_ADMIN_EMAIL en .env por un correo real.
docker compose up
```

(`pnpm setup:env` reemplaza el paso manual de copiar los `.env.example` y cambiar cada
`CAMBIAME`; nunca pisa valores que ya tengas.)

para confirmar que el stack completo (proxy, frontend, backend, postgres, redis, minio, prometheus, grafana) levanta correctamente con los nuevos nombres, y confirmá el backend con:

```bash
curl http://localhost:8080/health        # "status":"UP"
curl http://localhost:8080/prometheus | head   # métricas en texto plano
```

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
- **CI** (`.github/workflows/ci.yml`): lint + tests + build por proyecto, más `gitleaks`, Trivy, `pnpm audit` y la construcción de las imágenes de producción. Detalle en [`.claude/rules/CI_CD.md`](../.claude/rules/CI_CD.md).
- **Producción**: `docker-compose.prod.yml` (HTTPS obligatorio, solo el proxy publica puertos, contenedores sin root). Variables extra, certificados y cómo levantarlo en [INFRAESTRUCTURA.md](INFRAESTRUCTURA.md#7c-docker-composeprodyml). No hay CD todavía: depende de decidir el destino del despliegue.
- **Errores de la API**: el catálogo de códigos vive en [`docs/errors/`](errors/README.md).
