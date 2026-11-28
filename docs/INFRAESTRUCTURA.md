# Infraestructura

Este documento describe **todo** lo que compone la infraestructura de
DevelopLayout: los servicios de `docker-compose.yml`, la carpeta
`infrastructure/`, las redes, los volúmenes, las variables de entorno, y
los dos `Dockerfile` de producción. El objetivo es que después de leer esto
no quede ninguna duda sobre qué es cada pieza, para qué sirve, y por qué
está configurada como está.

## 1. Panorama general

```
                    ┌─────────────────────────────────────────┐
                    │         docker-compose.yml (dev)          │
                    │                                           │
  Internet/host ───▶│  proxy (nginx :80)                        │
                    │    ├── /api/*  ──▶ backend  (:8080)       │
                    │    └── /*      ──▶ frontend (:5173)       │
                    │                                           │
                    │  backend  ──▶ postgres (:5432)            │
                    │           ──▶ redis    (:6379)            │
                    │           ──▶ minio    (:9000)            │
                    │           ──▶ Docker socket (Testcontainers)│
                    │                                           │
                    │           ──▶ mailpit  (:1025, correo)    │
                    │  minio-permisos, minio-init (una corrida) │
                    │  prometheus ──▶ scrapea backend (:8080)   │
                    │  grafana    ──▶ lee de prometheus         │
                    └─────────────────────────────────────────┘
```

Todos los servicios viven en **una sola red Docker** (`app-network`,
bridge) y se resuelven entre sí por **nombre de contenedor** (DNS interno de
Docker) — nunca por IP fija. Los puertos también están publicados al host
(`localhost`) para poder acceder a cada uno directo durante desarrollo, sin
pasar por el proxy.

| Puerto en tu máquina | Servicio | Para qué |
| --- | --- | --- |
| `80` | `proxy` (nginx) | Punto de entrada único, simula cómo se accedería en producción |
| `5173` | `frontend` (Vite) | Acceso directo al dev server de React, con HMR |
| `8080` | `backend` (Spring Boot) | Acceso directo a la API, Swagger, `/health`, `/prometheus` |
| `5432` | `postgres` | Conectar un cliente SQL (DBeaver, pgAdmin, etc.) directo |
| `6379` | `redis` | Conectar un cliente Redis directo |
| `9000` / `9001` | `minio` | API S3 (`9000`) y consola web de administración (`9001`) |
| `8025` / `1025` | `mailpit` | Bandeja del correo de prueba (`8025`) y SMTP (`1025`) |
| `9090` | `prometheus` | UI de Prometheus (ver targets, correr queries PromQL) |
| `3000` | `grafana` | Dashboards (sin ninguno provisionado todavía, ver sección 8) |

Todos esos puertos se publican **solo en `127.0.0.1`**: el stack de desarrollo no queda expuesto a tu red local. El de Postgres es configurable con `POSTGRES_HOST_PORT` (por defecto `5432`; usá otro, ej. `5433`, si ya tenés un Postgres propio en esa máquina). Dentro de la red de Docker, el backend siempre usa `5432`.

## 2. Los servicios de `docker-compose.yml`

### 2.1 `proxy` — reverse proxy (nginx)

- **Imagen**: `nginx:stable-alpine`.
- **Rol**: único punto de entrada del stack en el puerto `80`, simulando
  cómo se vería en producción (un solo dominio, no puertos sueltos por
  servicio).
- **Config**: monta `infrastructure/nginx/nginx.conf` de solo lectura, con `server_tokens off`, cabeceras de seguridad, `limit_req` en `/api/` y `X-Forwarded-For` sobrescrito (no acumulado). Este es el proxy de **desarrollo** (HTTP, con WebSockets para Vite); el de producción es `nginx.prod.conf` (ver sección 7c).
- **Reglas de enrutamiento** (ver [sección 3](#3-nginx-enrutamiento-y-el-fix-de-dns-dinámico)):
  `/api/*` → `backend:8080`, cualquier otra ruta → `frontend:5173`.
- **`restart: unless-stopped`**: si el contenedor muere (ver el bug de DNS
  explicado abajo), Docker lo reintenta solo en vez de quedar caído hasta
  que alguien lo note.
- **Depende de**: `frontend` y `backend` (orden de arranque, no espera a que
  estén realmente listos — ver sección 3 para por qué esto ya no es un
  problema).

### 2.2 `frontend` — React + Vite (modo desarrollo)

- **Imagen**: `node:22-alpine` (imagen genérica de Node, no una imagen
  propia — el código se monta desde el host).
- **`working_dir`**: `/app/projects/frontend`.
- **Comando**: `corepack enable pnpm && pnpm install && pnpm run dev --host 0.0.0.0`
  — instala dependencias y arranca Vite en modo dev **cada vez que el
  contenedor arranca** (no hay una imagen pre-buildeada, es intencional
  para desarrollo: el código cambia constantemente).
- **`CHOKIDAR_USEPOLLING=true`**: necesario para que Vite detecte cambios de
  archivos hechos desde Windows a través del bind mount (los eventos
  nativos de filesystem de Linux no siempre se propagan correctamente
  desde un volumen montado en Windows).
- **Volúmenes** (ver [sección 4](#4-volúmenes) para el detalle del porqué):
  - `.:/app` — todo el repo, bind mount desde el host.
  - `frontend_node_modules_root` y `frontend_node_modules_pkg` — **aislados**
    del bind mount de arriba, a propósito.

### 2.3 `backend` — Spring Boot 4.2 / Java 21 (modo desarrollo)

- **Imagen**: `maven:3.9-eclipse-temurin-21-alpine` (Maven + JDK 21, imagen
  genérica).
- **`working_dir`**: `/app/projects/backend`.
- **Comando**: `mvn spring-boot:run -Dspring-boot.run.profiles=dev` — corre
  con Spring Boot DevTools activo (recarga en caliente al cambiar código,
  ver `pom.xml`).
- **Volúmenes**:
  - `.:/app` — todo el repo.
  - `~/.m2:/root/.m2` — el repositorio local de Maven del **host** (tu
    `C:\Users\<usuario>\.m2`), para no re-descargar todas las dependencias
    cada vez que se recrea el contenedor. Esto también significa que
    `./mvnw` corrido desde tu host y `mvn` corrido dentro del contenedor
    **comparten el mismo caché de dependencias**.
  - `/var/run/docker.sock:/var/run/docker.sock` — **solo desarrollo**: dar acceso al socket equivale a control total del host desde ese contenedor, y `docker-compose.prod.yml` no lo monta. Necesario para que
    **Testcontainers** (tests de integración, ver `TESTING_BACKEND.md`)
    pueda levantar contenedores (ej. un Postgres real) desde DENTRO de este
    contenedor. Sin esto, cualquier test que use Testcontainers falla con
    `Could not find a valid Docker environment`.
- **Variables de entorno propias del contenedor** (no vienen de `.env`,
  están fijas en `docker-compose.yml`):
  - `DOCKER_HOST=unix:///var/run/docker.sock` y
    `TESTCONTAINERS_RYUK_DISABLED=true` — necesarias para que Testcontainers
    se conecte de forma confiable al Docker Desktop de Windows a través del
    socket montado (ver nota de Docker Desktop en `TESTING_BACKEND.md`: en
    Windows, sin esto, la detección automática de Testcontainers falla).
- **`depends_on` con `condition: service_healthy`**: espera a que
  `postgres` y `redis` pasen su healthcheck antes de arrancar — a
  diferencia de `proxy`/`frontend`, acá sí importa que la DB esté
  realmente lista (Flyway corre migraciones al arrancar la app).

### 2.4 `postgres` — base de datos (PostgreSQL 18)

- **Imagen**: `postgres:18-alpine`.
- **Credenciales/DB**: vienen de `${POSTGRES_DB}`, `${POSTGRES_USER}`,
  `${POSTGRES_PASSWORD}` (del `.env` de la raíz — ver [sección 6](#6-variables-de-entorno)). La contraseña no tiene default: `docker compose` falla si falta.
  Es el **dueño** del esquema y solo lo usa Flyway. La aplicación se conecta con `app_runtime` (sin DDL y sin saltarse RLS), que crea una migración con `APP_RUNTIME_PASSWORD`.
- **Volúmenes**:
  - `postgres_data:/var/lib/postgresql` — los datos reales, persistentes
    entre reinicios del contenedor.
  - `./infrastructure/postgres/init:/docker-entrypoint-initdb.d` — carpeta
    para scripts SQL/shell que Postgres ejecuta **una sola vez**, la
    primera vez que se inicializa un volumen vacío (convención propia de
    la imagen oficial de Postgres). **Hoy está vacía** (`.gitkeep`), no hay
    ningún script de inicialización custom todavía — el esquema real lo
    crea Flyway desde las migraciones del backend
    (`src/main/resources/db/migration/`), no este mecanismo.
- **Healthcheck**: `pg_isready` cada 10s — es lo que usa `backend` para
  saber cuándo arrancar.

### 2.5 `redis` — caché (Redis 8)

- **Imagen**: `redis:8-alpine`.
- **Config propia**: `infrastructure/redis/redis.conf`, montada de solo
  lectura, con:
  ```
  maxmemory 256mb
  maxmemory-policy noeviction
  appendonly yes
  ```
  — límite de memoria de 256MB, **sin desalojo** y persistencia AOF
  habilitada. Redis guarda datos de seguridad (bloqueos de login y sesiones
  revocadas): al llenarse, las escrituras fallan en vez de borrarlos. Antes de
  usarlo como caché hay que separarlo en otra instancia o dimensionar la memoria.
- **Volumen**: `redis_data:/data` — para que la persistencia AOF de arriba
  tenga dónde escribir.
- **Contraseña obligatoria**: arranca con `--requirepass ${REDIS_PASSWORD}` (el valor viene del `.env`, nunca de `redis.conf`, que se commitea), y su healthcheck se autentica con ella. El backend la recibe como `SPRING_DATA_REDIS_PASSWORD`.
- **Estado de uso real**: el backend guarda ahí los contadores de intentos de
  login y de cambio de contraseña, la lista de sesiones revocadas, y los tokens
  y límites de recuperación e invitación.

### `mailpit` — correo de desarrollo

`axllent/mailpit` atrapa todo lo que envía el backend de desarrollo; no entrega
nada afuera (invitaciones, recuperación y avisos de contraseña). Bandeja en
`http://127.0.0.1:8025`, SMTP en `127.0.0.1:1025` (para correr el backend sin
Docker). Sin root y sin capabilities. No existe en el compose
de producción.

### 2.6 `minio` + `minio-init` — almacenamiento de archivos (S3-compatible)

**Hoy el backend no usa MinIO** (no incluye su cliente Java; las propiedades
`app.minio.*` no las lee nadie). Se levanta siempre, a propósito: la plantilla deja
el almacenamiento de archivos preparado para los proyectos que nazcan de ella.

- **`minio`**: imagen `cgr.dev/chainguard/minio` (`minio/minio` ya no existe en Docker Hub; corre **sin root** (uid 65532, `MINIO_UID` si tu imagen usa otro) y sin capabilities; su healthcheck usa `/dev/tcp` de bash porque no trae curl), expone la API S3 en `9000` y la
  consola web de administración en `9001`. Credenciales de
  `${MINIO_ROOT_USER}`/`${MINIO_ROOT_PASSWORD}`. Volumen `minio_data:/data`
  para persistencia.
- **`minio-permisos`**: contenedor de una sola corrida que, antes de arrancar
  MinIO, deja el volumen en manos de ese usuario (`chown`). Es el único paso como
  root: sin red y solo con las capabilities `CHOWN` y `DAC_OVERRIDE`. Migra
  solo los volúmenes creados cuando MinIO corría como root. **Volver a una
  configuración con root no necesita deshacerlo.**
- **`minio-init`**: contenedor de **una sola corrida** (imagen
  `cgr.dev/chainguard/minio-client`, variante `latest-dev` porque el script
  necesita shell; sin root y sin capabilities). Va **fijada por digest** y nada la
  actualiza sola: para subirla, `docker buildx imagetools inspect
  cgr.dev/chainguard/minio-client:latest-dev` y cambiar el digest en los dos
  compose (un test de `scripts/` exige que coincidan). Es
  el cliente de línea de comandos de MinIO). Corre
  `infrastructure/minio/create-buckets.sh` una vez, crea el bucket, y
  termina (`Exited (0)` es su estado normal y esperado — **no** es un
  servicio que deba quedar `Up`).
- **Qué hace `create-buckets.sh`** (ver el script completo en
  [sección 5](#5-el-script-de-minio-y-el-bug-que-tenía)):
  1. `mc alias set myminio http://minio:9000 "$MINIO_ROOT_USER" "$MINIO_ROOT_PASSWORD"`
     — registra las credenciales.
  2. `mc mb myminio/app-storage --ignore-existing` — crea el
     bucket (el nombre **debe coincidir** con `app.minio.bucket-name` en
     `application.properties` del backend).
  3. `mc anonymous set private myminio/app-storage` — fuerza el bucket a
     **privado** (sin acceso anónimo), aunque uno viejo hubiera quedado
     público. Los archivos se sirven siempre a través del backend
     (autorización por recurso) o con URLs prefirmadas de corta duración.

### 2.7 `prometheus` — recolección de métricas

- **Imagen**: `prom/prometheus:v3.14.0` (tag fijo).
- **Config**: `infrastructure/prometheus/prometheus.yml`:
  ```yaml
  global:
    scrape_interval: 5s
  scrape_configs:
    - job_name: 'app-backend'
      metrics_path: '/prometheus'
      static_configs:
        - targets: ['app-backend:8080']
  ```
  Scrapea el backend cada 5 segundos en `/prometheus` (no `/actuator/prometheus`
  — el backend expone Actuator en la raíz, ver `LOGGING_BACKEND.md`/`SEGURIDAD_AUTH_BACKEND.md`).
  **Solo scrapea el backend** — no hay métricas de frontend, postgres,
  redis ni minio configuradas (ninguno expone un endpoint Prometheus hoy).

### 2.8 `grafana` — visualización de métricas

- **Imagen**: `grafana/grafana:13.3.0` (tag fijo).
- **Provisioning automático**: monta
  `infrastructure/grafana/provisioning/datasources/`, que define un único
  datasource (Prometheus, marcado como default):
  ```yaml
  datasources:
    - name: Prometheus
      type: prometheus
      access: proxy
      url: http://app-prometheus:9090
      isDefault: true
      editable: false
  ```
- **No hay ningún dashboard provisionado** — solo la conexión a Prometheus
  está lista. Si entrás a `http://localhost:3000` vas a poder armar
  queries/paneles a mano, pero no hay nada pre-armado todavía.
- **Login**: usuario y contraseña vienen de `GRAFANA_ADMIN_USER` y
  `GRAFANA_ADMIN_PASSWORD` del `.env` (la contraseña es obligatoria, no
  hay `admin`/`admin`). El registro de usuarios y la telemetría de
  análisis están desactivados.

## 3. nginx: enrutamiento y el fix de DNS dinámico

`infrastructure/nginx/nginx.conf` tiene dos reglas:

| Ruta | Va a | Notas |
| --- | --- | --- |
| `/api/*` | `app-backend:8080` | Pasa `X-Real-IP`/`X-Forwarded-For`/`X-Forwarded-Proto` para que el backend sepa la IP real del cliente |
| `/*` (todo lo demás) | `app-frontend:5173` | Incluye soporte de WebSockets (`Upgrade`/`Connection: upgrade`) — **obligatorio** para que el HMR de Vite funcione a través del proxy |

**Detalle importante, no obvio**: la resolución de esos nombres de
contenedor (`app-backend`, `app-frontend`) es
**dinámica, en tiempo de request**, no fija al arrancar nginx:

```nginx
resolver 127.0.0.11 valid=10s;

location /api/ {
    set $backend_upstream http://app-backend:8080;
    proxy_pass $backend_upstream;
    ...
}
```

`127.0.0.11` es el DNS embebido de Docker dentro de la red del compose.
Usar `set $var ...` + `proxy_pass $var` (en vez de un bloque `upstream {}`
con el hostname fijo) hace que nginx resuelva el nombre **en cada
request**, no una sola vez al arrancar.

**Por qué importa**: con un `upstream {}` fijo (la config original de este
proyecto), si `backend`/`frontend` no estaban levantados en el instante
exacto en que nginx arrancaba, nginx fallaba con
`host not found in upstream` y **el proceso moría por completo**, sin
reintentar — pasó de verdad en esta máquina tras un reinicio de Docker
Desktop a mitad de una sesión de trabajo, y `proxy` quedó `Exited (1)`
durante casi una hora sin que nadie lo notara. Con resolución dinámica,
nginx arranca igual aunque el destino no esté listo todavía, y devuelve
`502` hasta que el contenedor de destino aparezca — sin necesitar
reinicio manual. El `restart: unless-stopped` en `docker-compose.yml` es
la segunda capa de defensa para el mismo problema.

## 4. Volúmenes

| Volumen | Nombre real en Docker | Contenido | Se borra con |
| --- | --- | --- | --- |
| `postgres_data` | `app-postgres_data` | Datos de la base de datos | `docker compose down -v` |
| `redis_data` | `app-redis_data` | Persistencia AOF de Redis | `docker compose down -v` |
| `minio_data` | `app-minio_data` | Objetos guardados en MinIO | `docker compose down -v` |
| `frontend_node_modules_root` | `app-frontend-node_modules-root` | `node_modules` de la raíz del workspace pnpm, **dentro del contenedor** | `docker compose down -v` o `docker compose rm -f -s -v frontend` |
| `frontend_node_modules_pkg` | `app-frontend-node_modules-pkg` | `node_modules` de `projects/frontend/`, **dentro del contenedor** | ídem |

Todos los nombres llevan el prefijo `app-` **a propósito**: esta
plantilla se reutiliza para otros proyectos en la misma máquina, y sin ese
prefijo, dos proyectos distintos basados en la misma plantilla terminarían
compartiendo el mismo volumen físico de Postgres/Redis/MinIO (pasó de
verdad antes de este fix — ver el historial de `docs/README.md`).

**Por qué los `node_modules` del frontend están en volúmenes separados y no
en el bind mount `.:/app`**: si no lo estuvieran, `pnpm install` dentro del
contenedor escribiría symlinks de Linux directo en tu filesystem de
Windows, apuntando a rutas que **solo existen dentro del contenedor**
(ej. `/app/node_modules/.pnpm/...`). Esos symlinks quedan rotos para
cualquier `pnpm`/`tsc`/ESLint que corras en tu host, y un `pnpm install` en
el host los pisa de vuelta en sentido contrario — rompiendo lo del
contenedor. Con volúmenes nombrados, cada lado (host y contenedor) tiene su
propio `node_modules` real, completamente independiente, y no chocan.

El backend **no** tiene este problema porque Maven no genera nada parecido
a `node_modules` dentro del proyecto — todo lo que descarga vive en
`~/.m2`, que ya está mapeado explícitamente desde el host.

## 5. El script de MinIO y el bug que tenía

`infrastructure/minio/create-buckets.sh`:

```sh
#!/bin/sh
set -e

mc alias set myminio http://minio:9000 "$MINIO_ROOT_USER" "$MINIO_ROOT_PASSWORD"
mc mb myminio/app-storage --ignore-existing
mc anonymous set private myminio/app-storage

exit 0
```

**Historia real de un bug que hubo acá**: la versión original de este
script usaba `mc config host add` (un comando **deprecado**, ya no existe
en versiones actuales de `mc`) en vez de `mc alias set`, y no tenía
`set -e`. Resultado: `mc config host add` fallaba con
`command not found`, pero el script seguía igual, y los pasos siguientes
(`mc mb`, `mc anonymous set`) — al no tener un alias `myminio` realmente
registrado — cayeron a operar sobre una **ruta de archivo local dentro del
contenedor descartable**, en vez de MinIO real. El script imprimía
`Bucket created successfully` (mentira) y **el bucket nunca se creaba de
verdad** en el servidor real. Se detectó recién auditando el stack a fondo,
no porque algo fallara ruidosamente. Ahora, con `mc alias set` + `set -e`,
si algo falla de verdad, `minio-init` termina con código de error distinto
de 0 y es visible en `docker ps -a`.

## 6. Variables de entorno

Hay **cuatro** archivos `.env`: raíz, backend, frontend y mobile. Ninguno se
commitea (`.gitignore` los excluye); sus `.env.example` sí son parte del repo y
sirven de plantilla. `pnpm setup:env` los crea todos.

### 6.1 Raíz (`.env`, a partir de `.env.example`)

Usadas por `docker-compose.yml` (y por `docker-compose.prod.yml`) para levantar los servicios de infraestructura. Las contraseñas **no tienen valor por defecto**: el `.env.example` trae `CAMBIAME` y `docker compose` falla si falta alguna (generá cada una con `openssl rand -base64 24`).

| Variable | Ejemplo | Para qué |
| --- | --- | --- |
| `POSTGRES_DB` | `app_db` | Nombre de la base de datos que Postgres crea al inicializar el volumen |
| `POSTGRES_USER` | `app_admin` | Usuario de Postgres |
| `POSTGRES_PASSWORD` | `CAMBIAME` | Password de Postgres (obligatoria) |
| `POSTGRES_HOST_PORT` | `5433` | Puerto del **host** donde se publica Postgres (opcional, default `5432`) |
| `REDIS_PASSWORD` | `CAMBIAME` | Password de Redis (obligatoria) |
| `GRAFANA_ADMIN_USER` | `admin` | Usuario admin de Grafana |
| `GRAFANA_ADMIN_PASSWORD` | `CAMBIAME` | Password admin de Grafana (obligatoria) |
| `APP_RUNTIME_PASSWORD` | `CAMBIAME` | Password del rol `app_runtime` con el que corre la app (obligatoria, solo letras y números). La crea la migración del rol; cambiarla después es `ALTER ROLE app_runtime PASSWORD '...'`, no editar la migración |
| `JWT_SECRET` | `CAMBIAME_MINIMO_32_CARACTERES_ALEATORIOS` | Clave HS256 para firmar los access token (mínimo 32 caracteres, sin default; la app no arranca si falta, es corta o conserva el valor de ejemplo). Generar con `openssl rand -base64 48` |
| `REFRESH_TOKEN_SECRET` | `CAMBIAME_MINIMO_32_CARACTERES_ALEATORIOS` | Clave que firma los refresh token (mínimo 32 caracteres, distinta de `JWT_SECRET`, sin default). Cambiarla cierra todas las sesiones. `pnpm setup:env` la genera |
| `INITIAL_ADMIN_EMAIL` | `admin@midominio.com` | Correo del `SUPER_ADMIN` inicial (solo se usa si todavía no existe ninguno) |
| `INITIAL_ADMIN_PASSWORD` | `CAMBIAME` | Contraseña del `SUPER_ADMIN` inicial (8 a 72 bytes y que no sea una de las muy usadas; sin ella, con la base vacía, la app no arranca) |
| `MINIO_ROOT_USER` | `minio_admin` | Usuario admin de MinIO (mínimo 3 caracteres, si no MinIO no arranca) |
| `MINIO_ROOT_PASSWORD` | `CAMBIAME` | Password admin de MinIO (obligatoria, **mínimo 8 caracteres**, si no MinIO tira `FATAL` y no arranca) |

Solo para producción (ver sección 7c): `TLS_CERT_DIR`, `CORS_ALLOWED_ORIGINS`, `MINIO_IMAGE` y `APP_VERSION`.

### 6.2 Backend (`projects/backend/.env`, a partir de `.env.example`)

Para correr el backend **fuera de Docker**. `application.properties` lee
variables de entorno (`${VARIABLE:default}`), pero **Spring Boot no carga este
archivo solo**: hay que pasarle las variables desde el IDE o la terminal (ver el
[tutorial](TUTORIAL.md#backend-fuera-de-docker)). Con Docker Compose no se usa:
las variables salen del `.env` de la raíz.

| Variable | Para qué | Default si no está |
| --- | --- | --- |
| `SPRING_DATASOURCE_URL` | URL JDBC de Postgres | `jdbc:postgresql://localhost:5432/app_db` |
| `SPRING_DATASOURCE_USERNAME` | Usuario de la DB | sin default (solo el perfil `dev` usa `postgres`) |
| `SPRING_DATASOURCE_PASSWORD` | Password de la DB | sin default (solo el perfil `dev` usa `postgres`) |
| `SPRING_FLYWAY_USER` / `SPRING_FLYWAY_PASSWORD` | Credenciales de Flyway (el rol dueño del esquema) | las del datasource |
| `SPRING_DATA_REDIS_HOST` | Host de Redis | `localhost` |
| `SPRING_DATA_REDIS_PASSWORD` | Password de Redis (la misma que `REDIS_PASSWORD`) | sin default |
| `MINIO_URL` | URL de la API S3 de MinIO | `http://localhost:9000` |
| `MINIO_ROOT_USER` | Usuario de MinIO (reusa el mismo que el root de arriba) | sin default (el perfil `dev` usa `minio_admin`) |
| `MINIO_ROOT_PASSWORD` | Password de MinIO | sin default (el perfil `dev` trae uno local) |
| `SWAGGER_ENABLED` | Enciende Swagger UI y `/v3/api-docs` fuera del perfil `dev` | `false` |
| `CORS_ALLOWED_ORIGINS` | Orígenes permitidos para CORS (separados por coma) | `http://localhost:5173,http://localhost` |
| `RATE_LIMIT_CAPACITY` | Cupo del rate limit por IP (ver `SEGURIDAD_AUTH_BACKEND.md#rate-limit`) | `100` |
| `RATE_LIMIT_REFILL_PER_MINUTE` | Cuántos tokens se regeneran por minuto | `100` |
| `APP_RUNTIME_PASSWORD` | Contraseña con la que la migración crea el rol `app_runtime` | sin default |
| `JWT_SECRET` / `REFRESH_TOKEN_SECRET` | Claves de los tokens (32 caracteres o más, distintas) | sin default |
| `INITIAL_ADMIN_EMAIL` / `INITIAL_ADMIN_PASSWORD` | Primer `SUPER_ADMIN` | sin default |
| `SPRING_MAIL_*`, `MAIL_FROM`, `APP_FRONTEND_BASE_URL` | Correo y dirección de la web (ver 7c) | el perfil `dev` apunta a Mailpit y a `localhost:5173` |
| `LOGIN_MAX_ATTEMPTS_PER_ACCOUNT` | Tope de intentos por cuenta sumando todas las IP | `100` |
| `BITACORA_RETENTION_MONTHS` | Meses que se conserva la bitácora | `12` |

> `CORS_ALLOWED_ORIGINS` y las dos de `RATE_LIMIT_*` tienen default
> razonable en `application.properties`, así que no es obligatorio
> definirlas en `.env` salvo que se necesite un valor distinto.

### 6.3 Frontend (`projects/frontend/.env`, a partir de `.env.example`)

Usadas por Vite (`import.meta.env.VITE_*`, solo las que empiezan con
`VITE_` quedan expuestas al código del navegador):

| Variable | Para qué | Default |
| --- | --- | --- |
| `VITE_API_BASE_URL` | URL base de la API que usa `apiClient` (`src/store/http/apiClient.ts`) | `http://localhost:8080/api/v1` |
| `VITE_ENABLE_API_LOGS` | Prende/apaga el logging de requests HTTP en consola (`src/store/http/createBaseApi.ts`) | `false` |

> Nota: `VITE_API_BASE_URL` apunta **directo al backend** (`:8080`), no al
> proxy (`:80`) — el frontend en desarrollo bypasea nginx para las
> llamadas a la API.

## 7. Los dos `Dockerfile` de producción

`docker-compose.yml` es exclusivamente para **desarrollo** (imágenes
genéricas + bind mount del código, sin build). Para producción existen dos
`Dockerfile` independientes, probados manualmente esta sesión con
`docker build`/`docker run` reales (no solo revisados a ojo):

### 7.1 `projects/backend/Dockerfile`

Build multi-stage self-contenido (contexto = `projects/backend`, no la
raíz — el backend no es un workspace, es un único módulo Maven):

1. **Etapa `builder`** (`maven:3.9-eclipse-temurin-21-alpine`): copia
   `pom.xml` primero (cache de capas de Docker), descarga dependencias
   offline, copia `src/`, compila y empaqueta el `.jar` (`-DskipTests`
   para agilizar el build — los tests ya corrieron en CI/pre-commit antes
   de llegar acá).
2. **Etapa runtime** (`eclipse-temurin:21-jre-alpine`, sin Maven ni
   compiladores — imagen final más liviana): instala `fontconfig` y
   `ttf-dejavu` (JasperReports renderiza con `java.awt` y la imagen alpine
   no trae fuentes), crea un usuario sin privilegios (`springuser`) y corre
   el `.jar` como ese usuario, no como root, en modo headless.

```bash
docker build -f projects/backend/Dockerfile -t app-backend projects/backend
```

Probado real: build exitoso, y corriendo contra la red de
`docker-compose.yml` (Postgres/Redis/MinIO reales) arrancó y `/health`
respondió `200` a la primera, sin ajustes.

### 7.2 `projects/frontend/Dockerfile`

Build multi-stage, pero acá **el contexto de build es la raíz del repo**,
no `projects/frontend` — es un detalle crítico, documentado en un
comentario al principio del propio archivo:

```bash
docker build -f projects/frontend/Dockerfile -t app-frontend .
```

**Por qué contexto raíz**: el proyecto es un workspace de pnpm
(`pnpm-workspace.yaml` en la raíz). Para que `pnpm install` use el
`pnpm-lock.yaml` real (build reproducible) en vez de resolver versiones
nuevas cada vez, hace falta copiar `package.json` + `pnpm-workspace.yaml` +
`pnpm-lock.yaml` de la raíz antes de instalar, algo que un contexto
limitado a `projects/frontend/` no puede ver.

1. **Etapa `builder`** (`node:22-alpine`): copia los manifiestos primero
   (cache de capas), instala con `pnpm install --frozen-lockfile --filter
   frontend...` (falla si el lockfile no está sincronizado,
   en vez de regenerarlo silenciosamente), copia el código y buildea con
   `pnpm --filter frontend build`.
2. **Etapa runtime** (`nginxinc/nginx-unprivileged:stable-alpine-slim`): sirve
   los archivos estáticos compilados (`dist/`) en el puerto `8080`, como
   usuario sin privilegios, con `projects/frontend/nginx.conf` (fallback a
   `index.html` para el router SPA, assets con hash cacheados un año,
   `index.html` sin cache). Este nginx es **distinto** al `proxy`: solo
   sirve estáticos, no enruta la API.
   `VITE_API_BASE_URL` se fija al construir (`--build-arg`, default
   `/api/v1`, relativa porque frontend y API comparten origen detrás del
   proxy).

**Hay un `.dockerignore` en la raíz del repo** (no en
`projects/frontend/`, porque el contexto de build es la raíz — Docker solo
lee el `.dockerignore` que está en la raíz del contexto). Sin él,
`COPY projects/frontend ./projects/frontend` arrastraba el `node_modules`
real del host hacia adentro de la imagen, con symlinks apuntando a rutas
que no existen ahí — rompía el build con `Cannot find module`. Excluye
`node_modules`, `dist`, `.dart_tool`, `build`, `target`, `.git` y los
`.env` reales (dejando pasar los `.env.example`) en cualquier subcarpeta
del repo.

Probado real (versión anterior, con `nginx:alpine`): build exitoso,
contenedor corriendo, `200` + `<title>frontend</title>` al pedir `/`. La
versión actual (nginx-unprivileged slim parcheada) se probó dentro del compose de
producción completo (sección 7c).

## 7c. `docker-compose.prod.yml`

Compose de producción, **en lugar de** `docker-compose.yml` (nunca los dos
a la vez):

```bash
docker compose -f docker-compose.prod.yml --env-file .env up -d --build
```

| Aspecto | Desarrollo | Producción |
| --- | --- | --- |
| Código | bind mount + hot reload | imágenes construidas, sin bind mounts |
| `docker.sock` | montado (Testcontainers) | **no se monta** |
| Puertos publicados | todos, en `127.0.0.1` | solo el proxy: `80` (redirige) y `443` (TLS) |
| Postgres, Redis, MinIO, Prometheus, Grafana | accesibles desde el host | solo red interna |
| Proxy | `nginx.conf`, HTTP | `nginx.prod.conf`, TLS 1.2/1.3, HSTS, CSP estricto |
| Contenedores | root en varios | sin root, `cap_drop: ALL`, `no-new-privileges`, `read_only` donde aplica |

Variables obligatorias además de las de desarrollo (el compose falla si
faltan): `TLS_CERT_DIR` (carpeta con `fullchain.pem` y `privkey.pem`),
`CORS_ALLOWED_ORIGINS` (origen HTTPS del frontend) y `MINIO_IMAGE` (tag o
digest fijo). `APP_VERSION` es opcional (tag de las imágenes construidas).

**Permisos de la clave privada TLS.** El proxy corre como uid `101` (sin
root), así que `privkey.pem` tiene que poder leerla ese usuario; si no,
nginx aborta con `cannot load certificate key ... Permission denied` y el
contenedor se reinicia en bucle. En un servidor real:

```bash
sudo chown root:101 privkey.pem && sudo chmod 640 privkey.pem
```

(con una clave de Let's Encrypt, que `/etc/letsencrypt/live` y `archive`
también dejen atravesar el directorio a ese grupo). No uses `chmod 644`
con una clave real, solo con el certificado de prueba local.

**Correo (recuperar contraseña).** `SPRING_MAIL_HOST`, `MAIL_FROM` y
`APP_FRONTEND_BASE_URL` (HTTPS) son obligatorias: sin ellas el compose falla.
`SPRING_MAIL_PORT` (587), `SPRING_MAIL_USERNAME`, `SPRING_MAIL_PASSWORD`,
`SPRING_MAIL_SMTP_AUTH` y `SPRING_MAIL_SMTP_STARTTLS` (ambas `true`) dependen
del servidor SMTP que elija cada proyecto. Para que los correos no caigan en spam,
el dominio del remitente necesita SPF y DKIM. Un SMTP caído **no** afecta a
`/health` ni al resto de la aplicación: se registra en el log y en la bitácora. El
envío no está garantizado (no hay cola persistente): quien no reciba el enlace
pide otro.

**El proxy y la web se construyen** (`infrastructure/nginx/Dockerfile` y
`projects/frontend/Dockerfile`) para parchear la imagen base. Para recibir parches
nuevos hay que reconstruir sin caché y sobre la base al día:
`docker compose -f docker-compose.prod.yml build --pull --no-cache`.

**Arranque.** El backend tarda unos segundos en estar listo (en la prueba local,
unos 26 s); hasta entonces el proxy responde `502` en `/api/`. No uses
`up --wait` como señal de éxito: sale con código 1 porque `minio-init` es un
contenedor de una sola corrida que termina, y puede volver con el backend todavía
arrancando. Mirá el estado de salud de `app-backend`.

**Prueba automática:** `pnpm test:prod` (y el job `docker` de CI) levanta este
compose con datos descartables y comprueba lo descrito abajo.

**Probado** (2026-11, en una máquina local, certificado autofirmado y volúmenes
vacíos): todos los servicios arrancan; HTTP redirige a HTTPS; login web y mobile,
refresh y logout funcionan; solo el proxy publica puertos; y respaldo +
restauración en un servidor vacío. **No probado:** un servidor Linux real, un
certificado de una autoridad real y carga sostenida.

Grafana y Prometheus no tienen puertos: se consultan por túnel SSH o con
`docker compose exec`, nunca por internet.

## 7b. Seguridad de la infraestructura (resumen)

- Todos los puertos publicados al host van en `127.0.0.1`.
- Redis exige `REDIS_PASSWORD`; Grafana exige `GRAFANA_ADMIN_PASSWORD`;
  `POSTGRES_PASSWORD` y `MINIO_ROOT_PASSWORD` no tienen default
  (`docker compose` falla si faltan).
- El bucket de MinIO es **privado**.
- Imágenes con tag fijo (`prom/prometheus:v3.14.0`, `grafana/grafana:13.3.0`,
  `nginx:stable-alpine`).
- nginx: `server_tokens off`, cabeceras de seguridad, `limit_req` en `/api/`,
  cuerpo máximo de **1 MB** y `X-Forwarded-For` sobrescrito (no acumulado). Un
  endpoint que reciba archivos amplía el límite solo en su `location`.
- El backend limita sus esperas: 30 s por consulta, 10 s para obtener o abrir una
  conexión y 2 s en Redis (ver `PERSISTENCIA_BD_BACKEND.md`).
- El estado frente a OWASP, con sus pendientes, está en
  [seguridad/AUDITORIA_OWASP.md](seguridad/AUDITORIA_OWASP.md).
- Config base del backend segura; lo cómodo (Swagger, `show-sql`, health
  detallado) solo en el perfil `dev`.

## 7d. Respaldos de la base

`pnpm db:backup` / `pnpm db:verify` (ver `scripts/README.md`). Es manual: no hay
programación automática ni copia fuera del servidor — en producción hay que
agendarlo (cron/tarea del proveedor) y enviar el archivo, cifrado, a otro lugar.
El respaldo cubre **solo Postgres**: los archivos de MinIO se respaldan aparte.

### Recuperar en un servidor vacío

Con el mismo `.env` (mismos `POSTGRES_USER` y `POSTGRES_DB`), levantá **solo
Postgres**, restaurá y recién después levantá el resto:

```bash
docker compose -f docker-compose.prod.yml up -d --wait postgres
pnpm db:restore backups/<archivo>.dump --confirmar --prod
docker compose -f docker-compose.prod.yml up -d --build
```

Sin Node en el servidor, o con una base administrada, hay que hacer a mano lo que
hace `db:restore`, en este orden: crear el rol `app_runtime` (`LOGIN`,
`NOSUPERUSER NOBYPASSRLS`, con `APP_RUNTIME_PASSWORD`), restaurar con
`pg_restore --clean --if-exists --no-owner --exit-on-error --single-transaction` y
revocar las sesiones activas.

! Un `pg_restore` directo sobre un servidor vacío **deja la app sin arrancar**: el
volcado no incluye roles, y Flyway no vuelve a crear `app_runtime` porque el
historial restaurado marca esa migración como aplicada. Por la misma razón, no
levantes el backend antes de restaurar.

## 8. Qué NO está implementado todavía (para que quede explícito, no ambiguo)

- **Sin reglas de alerta** — Prometheus recibe las métricas, pero nadie recibe un
  aviso (pendiente P-02 de la
  [auditoría](seguridad/AUDITORIA_OWASP.md#pendientes)).
- **Sin dashboards de Grafana** — solo el datasource de Prometheus está
  provisionado.
- **Sin métricas de nada más que el backend** — frontend/postgres/redis/minio
  no exponen métricas a Prometheus.
- **Rate limit del backend no es distribuido** — vive en memoria del
  proceso (caché acotada), correcto para una sola instancia; con más
  de una réplica cada una tendría su propio cupo independiente (ver
  `SEGURIDAD_AUTH_BACKEND.md#rate-limit`).
- **Sin CD** — existe `docker-compose.prod.yml` (sección 7c), pero ningún
  job publica imágenes ni despliega: el destino (VPS, ECS, Cloud Run...)
  sigue sin decidirse (ver `CI_CD.md`).
- **Producción no se probó en un servidor real** — ver qué se probó en local en
  la sección 7c.
- **`infrastructure/postgres/init/` y `infrastructure/docker/` están
  vacías** (`.gitkeep`) — reservadas para scripts de inicialización de
  Postgres o configuración Docker adicional que todavía no hizo falta.
