# CI/CD

Aplica a todo el repositorio (el pipeline vive en `.github/workflows/` y
cubre backend, frontend y mobile a la vez, con un job independiente por
proyecto).

## Qué hay implementado y qué no

- **CI (Integración Continua): sí, implementado.**
  [`.github/workflows/ci.yml`](../../.github/workflows/ci.yml) — corre
  lint + tests + build de cada proyecto, más los controles de seguridad
  de abajo, en cada Pull Request contra `main` y en cada push directo a
  `main`.
- **CD (Entrega/Despliegue Continuo): todavía no.** No existe ningún job
  de deploy, ni de build/push de imágenes Docker a un registro. Ver
  [Por qué CD no está implementado todavía](#por-qué-cd-no-está-implementado-todavía)
  más abajo — no es un olvido, es una decisión pendiente de otra decisión
  que este repo no tomó todavía.

## Controles de seguridad en CI (A03 Supply Chain)

**Implementado** en `ci.yml` (ver [SEGURIDAD_OWASP.md](SEGURIDAD_OWASP.md)):

- **`pnpm audit --audit-level moderate`** al final del job `frontend`:
  falla con cualquier vulnerabilidad conocida, moderada o peor.
- **Job `security`** (corre siempre, no depende de qué carpeta cambió):
  `gitleaks` (secretos en todo el historial del PR) y `Trivy` en modo
  filesystem (dependencias vulnerables de `pom.xml`, `pnpm-lock.yaml` y
  `pubspec.lock`, más misconfiguraciones de los `Dockerfile`), con
  `HIGH,CRITICAL` y `ignore-unfixed`.
- **Job `docker`**: construye las imágenes de producción (backend, web y proxy),
  las escanea con Trivy, valida `docker-compose.prod.yml` y **levanta el stack
  completo** (`scripts/produccion/prod-smoke.mjs`, `pnpm test:prod` en local). Corre si
  cambian el backend, el frontend, `infrastructure/`, el compose de producción,
  `.env.example`, `ci.yml` o los scripts que usa la prueba. No publica nada.
- **`permissions: contents: read`** a nivel de workflow: ningún job
  escribe en el repo.
- **Dependabot** (`.github/dependabot.yml`): semanal (Maven, npm, pub, Docker,
  GitHub Actions). Agrupa versiones menores y parches por ecosistema (Docker no
  se agrupa); cada salto mayor llega solo. Ignora el major de `typescript` (hasta
  que `typescript-eslint` lo soporte), de `eclipse-temurin` y de `node`, y la
  imagen `maven` entera (la versión de Java va en su etiqueta). **Cambiar de Java
  o de Node es una decisión manual** que alinea Dockerfile, `pom.xml` o
  `package.json`, compose y CI.

> Estos jobs **no se pudieron ejecutar localmente** al escribirlos: la
> primera corrida real en GitHub puede pedir ajustes (versiones de las
> acciones, falsos positivos de Trivy). Un falso positivo se documenta y se
> agrega a un `.trivyignore`, no se baja la severidad del escaneo.
> `scripts/dependencias/update-deps.*` hace la misma auditoría en local.

## Contrato de la API

`docs/api/openapi.json` y `shared/api/schema.d.ts` son **generados** y están
marcados `linguist-generated` en `.gitattributes`: GitHub muestra sus cambios
plegados en el PR. Plegado no es lo mismo que revisado: el diff del contrato se
abre cuando un cambio toca la API.

- La prueba `OpenApiContractTest` (job `backend`) falla si lo que publica la API
  no coincide con `openapi.json`.
- El job `frontend` regenera `schema.d.ts` y falla si difiere del commiteado.
- Por eso un cambio en `openapi.json` activa los jobs `backend` y `frontend`
  (y, por el primero, también `docker`).

## Prueba de producción de punta a punta

`scripts/produccion/prod-smoke.mjs` comprueba arranque, puertos publicados, redirección y
cabeceras (incluido `no-referrer` en `/reset-password`), rutas internas, sesiones
web y mobile, la recuperación de contraseña y el alta de un usuario con el
correo caído (la app sigue sana), el cambio de roles, bucket de MinIO privado, y
respaldo restaurado en un servidor vacío (incluido que una sesión anterior no
reviva). **Todo cambio que altere ese comportamiento actualiza la prueba.**

Borra volúmenes, así que antes de tocar Docker exige que todos los volúmenes y
redes lleven su prefijo y que no haya contenedores con los mismos nombres (en
local hay que bajar el stack propio antes). Esas protecciones tienen tests en
`prod-smoke.test.mjs`: **no se relajan**. Usa un certificado autofirmado que sí
verifica, publica solo en `127.0.0.1:18080/18443` y no lee el `.env` de la raíz.

No cubre: un servidor real, un certificado de una autoridad, carga, ni
actualizar un despliegue que ya tiene datos de una versión anterior.

## Descarga de Maven

`mvnw` baja Maven de Maven Central y esa descarga puede fallar en el runner. En
el job `backend`, `setup-java` (`cache: maven`) guarda `~/.m2/wrapper/dists`; el
paso «Preparar Maven» reintenta mostrando el error y, si no hay caso, usa el
Maven preinstalado del runner y deja una advertencia. Con esa
advertencia, la versión de Maven puede no ser la de `maven-wrapper.properties`.

## CVEs en las imágenes base

Si Trivy marca paquetes del sistema con arreglo disponible, se actualizan en el
`Dockerfile`; no se ignoran ni se baja la severidad. La imagen web y la del proxy
de producción (`infrastructure/nginx/Dockerfile`) usan
`nginx-unprivileged:stable-alpine-slim`, corren `apk upgrade` y exigen un mínimo
de nginx (`nginx>=X.Y.Z`), porque la base fija su versión y `apk upgrade` solo
no la sube. Ante un CVE nuevo de nginx se sube ese mínimo **en las dos** (un test
de `scripts/` exige que coincidan).

## Versión de Flutter fija

El job `mobile` fija `flutter-version` (la misma del desarrollo local). Con
`channel: stable` a secas, una versión nueva marcó como obsoleto
`package:flutter/material.dart` y rompió el CI sin cambios en el código.
Dependabot no la actualiza: al subir de Flutter se cambia en `ci.yml` en el
mismo cambio. **Pendiente:** a partir de la 3.47 hay que migrar a `material_ui`.

## Falsos positivos de gitleaks

**Se previenen al escribir la prueba:** una contraseña o un token inventado
asignado a una propiedad o variable con nombre de secreto
(`passwordActual: '...'`, `PASSWORD = "..."`) lleva `// gitleaks:allow` **en el
mismo commit que la crea**. gitleaks no se ejecuta en local ni en el pre-commit,
así que si se olvida solo aparece en el CI, y entonces hace falta además el
fingerprint. Ya pasó varias veces.

Se anotan uno por uno: `gitleaks:allow` en la línea y, si el hallazgo está en un
commit ya hecho, su fingerprint en `.gitleaksignore` con el motivo. Nunca se
desactiva una regla ni se excluye una carpeta. gitleaks revisa cada commit del
PR, así que rebasear cambia los hashes anotados. **Un secreto real no se ignora:
se rota.**

## Acciones fijadas por SHA

Toda acción externa se referencia por el SHA completo de su release, con la
versión como comentario: `uses: owner/accion@<sha-de-40-caracteres> # vX.Y.Z`
(un tag como `@v4` es mutable: el incidente de `trivy-action` de marzo de 2026
mostró el riesgo). `scripts/seguridad/ci-seguridad.test.mjs` falla si alguna no lo cumple.
Dependabot (`github-actions`) actualiza SHA y comentario. Para agregar o subir una
a mano, el SHA se toma de `git ls-remote https://github.com/owner/accion
'refs/tags/vX.Y.Z*'` (si el tag es anotado, el de `^{}`), nunca de memoria, y
antes de un salto mayor se leen sus notas de versión.

No cubre lo que cada acción descarga o usa por dentro (el binario de gitleaks, la
base de Trivy, sus propias dependencias).

## Pruebas de navegador

El job `e2e` corre `scripts/e2e/e2e.mjs` (en local, `pnpm test:e2e`) cuando cambian la
web, el backend, el propio script o las dependencias. Detalle de lo que cubre en
[TESTING_FRONTEND.md](frontend/TESTING_FRONTEND.md#pruebas-de-navegador).

- Entorno desechable: Postgres, Redis y Mailpit con prefijo `e2e-nav-`, puertos
  propios en `127.0.0.1`, secretos generados en cada corrida y sin leer ningún
  `.env`.
- **Solo borra lo que él creó**, por identificador y por una etiqueta única de la
  corrida (cubre una interrupción mientras Docker crea un contenedor); si ya hay un
  contenedor con sus nombres o un puerto ocupado, se detiene sin tocar nada. Esas
  protecciones tienen tests en `e2e.test.mjs`: **no se relajan**.
- **Interrumpirlo no deja restos**, tampoco con un segundo Ctrl+C durante la
  limpieza. `e2e-interrupcion.test.mjs` lo comprueba con señales reales sobre el
  script, en Linux (job `scripts`; en Windows queda omitido).
- Sin reintentos. Una prueba inestable se corrige.
- El informe y las trazas se suben solo si falla (7 días); traen datos desechables.

## Un job por proyecto, no uno solo para todo

`paths-filter` no avisa si una ruta ya no existe: el job deja de correr y el CI
sigue en verde. `scripts/seguridad/rutas-scripts.test.mjs` falla si `ci.yml` o
`package.json` nombran un script que no está. No detecta un filtro al que le
falte una ruta: eso se revisa al cambiar qué importa un script.

El pipeline usa un job `changes` (con la acción `dorny/paths-filter`) que
detecta qué carpetas cambiaron en el PR/push, y cada job de proyecto
(`backend`, `frontend`, `mobile`, `scripts`, `e2e`) solo corre si `changes` marcó
que sus rutas tuvieron cambios (un cambio en `ci.yml` los corre todos, para que
el workflow no se fusione sin haberse probado; `backend` también con `infrastructure/redis/**`,
porque un test verifica `redis.conf`; `scripts` también con
`docker-compose*.yml` y los `Dockerfile` de nginx, que
`compose-seguridad.test.mjs` revisa, y con cualquier cambio en `projects/` o
`infrastructure/`, porque `init-project.test.mjs` recorre la plantilla entera).
Esto es deliberado por dos razones:

- **Este es un monorepo con 3 proyectos independientes** (ver
  [`README.md`](../../README.md)) que no comparten build ni runtime — no
  tiene sentido esperar a que Flutter compile si el cambio solo tocó una
  migración de Flyway en el backend.
- **Minutos de CI no son gratis** (tienen un límite mensual incluso en
  cuentas gratuitas de GitHub) — correr los 3 jobs completos por cada
  cambio, sin importar qué tocó, desperdicia esa cuota sin necesidad.

Si un cambio toca más de un proyecto a la vez (lo cual, según
[CONVENCIONES_GIT.md](CONVENCIONES_GIT.md#regla-estricta-un-commit-un-scope),
va a venir en commits separados por scope de todos modos), simplemente
corren los jobs de ambos proyectos en paralelo — no hay orden de
dependencia entre `backend`, `frontend` y `mobile`.

## Qué corre cada job

Los mismos comandos que ya están documentados como el flujo local de cada
proyecto (ver [`README.md`](../../README.md#puesta-en-marcha-rápida)),
sin nada adicional inventado solo para CI:

| Job | Pasos |
| --- | --- |
| `backend` | `mvnw checkstyle:check` → `mvnw test` → `mvnw clean package` |
| `frontend` | `pnpm install --frozen-lockfile` (desde la raíz, ver [docs/README.md](../../docs/README.md)) → tipos al día con el contrato → `pnpm lint` → `pnpm test` → `pnpm build` → `pnpm audit --audit-level moderate` |
| `mobile` | `flutter pub get` → `flutter analyze` → `flutter test` |
| `scripts` | `node --test "scripts/**/*.test.mjs"` |
| `e2e` | `pnpm install --frozen-lockfile` → `playwright install --with-deps chromium` → `node scripts/e2e/e2e.mjs` |

- El job de `backend` levanta `postgres:18-alpine` y `redis:8-alpine` como
  `services:` del job (mismas versiones que `docker-compose.yml`) y le pasa
  `SPRING_DATASOURCE_*`/`SPRING_DATA_REDIS_HOST` al paso de tests. Hoy los
  tests de contexto completo levantan su propio Postgres con Testcontainers; el
  Redis del job lo usan los que extienden solo `PostgresIntegrationTest`. Si se
  renombra la base (`POSTGRES_DB`), se actualiza también en `ci.yml`.
- El job de `backend` corre en un runner de Ubuntu, que trae Docker nativo
  disponible — a diferencia del setup local en Windows (ver
  [TESTING_BACKEND.md](backend/TESTING_BACKEND.md)), acá los tests que
  usan Testcontainers corren de verdad en vez de quedar `SKIPPED`. Si un
  test con Testcontainers falla en CI pero pasaba "SKIPPED" en local, es
  información real sobre ese test, no un falso positivo del pipeline.
- `pnpm install` se corre con `--frozen-lockfile`: si `pnpm-lock.yaml` no
  refleja exactamente lo que pide algún `package.json` del workspace, el
  job falla en vez de instalar versiones distintas a las que se usan en
  desarrollo — evita el clásico "en mi máquina funciona" por un lockfile
  desactualizado que nadie commiteó.
- Ninguno de esos jobs necesita secretos/credenciales — son build +
  lint + test contra código propio, sin tocar servicios externos reales
  (Postgres/Redis/MinIO de verdad, un backend desplegado, etc.).

## Por qué CD no está implementado todavía

CD (llevar el build a un servidor real, staging o producción) necesita un
**destino**: dónde corre el backend/frontend en producción — un VPS
propio, un servicio administrado (ECS, Cloud Run, un droplet con Docker
Compose, etc.), o alguna otra plataforma. Ese destino **no está decidido
todavía** en este proyecto — hoy `docker-compose.yml` está pensado
explícitamente para desarrollo local (bind mounts del código fuente, hot
reload, `spring-boot:run` en vez de un JAR empaquetado), no para correr en
producción tal cual.

Armar un job de CD sin esa decisión tomada significaría inventar una
plataforma de destino de forma especulativa y después tener que
rehacerlo cuando se decida la real — exactamente el tipo de anticipación
que este proyecto evita en otras áreas (ver el criterio de "no crear
`shared/` hasta que haga falta" en
[TANSTACK_QUERY_FRONTEND.md](frontend/TANSTACK_QUERY_FRONTEND.md)). Por
eso CD queda pendiente de una decisión de infraestructura explícita, no
de una tarea de pipeline.

### Cómo se vería, en términos generales, una vez que exista un destino

Documentado acá como referencia conceptual, **no como algo a implementar
todavía**:

1. **Build de artefacto desplegable.** Backend: una imagen Docker
   construida con un `Dockerfile` multi-stage (compila con Maven, corre
   con solo el JRE — no la imagen `maven:...` de desarrollo que usa
   `docker-compose.yml` hoy). Frontend: un build estático de Vite
   (`pnpm build`) servido por nginx, tampoco la imagen `node:22-alpine`
   con `pnpm run dev` que se usa en desarrollo.
2. **Publicar el artefacto.** Subir la imagen a un registro de
   contenedores (ej. GitHub Container Registry, gratis para repos de
   GitHub) con un tag identificable (el SHA del commit, o la versión).
3. **Desplegar.** El paso que de verdad depende de la plataforma elegida:
   desde un simple `ssh` a un VPS que hace `docker compose pull && docker
   compose up -d`, hasta una acción específica de un proveedor cloud
   (ej. `aws-actions/amazon-ecs-deploy-task-definition` para ECS en AWS,
   o el CLI de Cloud Run en GCP). Esta pieza **sí** necesita secretos
   (credenciales del proveedor cloud, guardados como GitHub Secrets, no
   en el repo) — algo que ningún job de CI actual necesita.
4. **Gate de aprobación (opcional pero recomendado para producción).** Que
   el job de deploy a producción no dispare solo con cualquier push a
   `main`, sino que requiera una aprobación manual en GitHub
   (`environment: production` con reviewers configurados) — así un merge
   no despliega solo a nadie mirando.

Cuando se tome la decisión de dónde se despliega este proyecto, se agrega
un job de deploy nuevo (o un workflow separado,
`.github/workflows/cd.yml`) que reusa el mismo build que ya valida `ci.yml`
— no se duplica la lógica de build entre los dos workflows.
