# Tutorial: cómo usar la plantilla

Recorrido completo: crear tu proyecto a partir de DevelopLayout, levantarlo,
probar lo que ya trae y agregar tu primera función. Los comandos están en
PowerShell; donde cambian en Linux o macOS, se indica.

1. [Qué trae](#1-qué-trae)
2. [Qué necesitas](#2-qué-necesitas)
3. [Crear tu proyecto](#3-crear-tu-proyecto)
4. [Levantarlo por primera vez](#4-levantarlo-por-primera-vez)
5. [Recorrido por lo que ya funciona](#5-recorrido-por-lo-que-ya-funciona)
6. [El día a día](#6-el-día-a-día)
7. [Agregar una función](#7-agregar-una-función)
8. [Guardar y publicar los cambios](#8-guardar-y-publicar-los-cambios)
9. [Producción](#9-producción)
10. [Problemas comunes](#10-problemas-comunes)

## 1. Qué trae

| Ya funciona | Queda preparado, sin usar todavía |
| --- | --- |
| Inicio y cierre de sesión, renovación automática y bloqueo por intentos | MinIO para archivos (ningún código lo usa) |
| Recuperar y cambiar la contraseña, con aviso por correo | JasperReports para reportes (no hay reportes) |
| Usuarios con invitación por correo y roles `SUPER_ADMIN` y `USUARIO` | Grafana y Prometheus (sin paneles ni alertas) |
| Mis sesiones, sesiones de usuarios y bitácora | La campana de notificaciones (oculta hasta que tenga datos) |
| Español e inglés en web, app, respuestas y correos | Despliegue automático (no hay CD) |
| Base de datos privada con seguridad por fila (RLS) | |
| Pruebas, CI y `docker-compose` de producción | |

La administración de usuarios, las sesiones de otras personas y la bitácora están
**en la web**. La app mobile tiene sesión, recuperación, cambio de contraseña,
Mis sesiones e idiomas; tiene pruebas automáticas, pero **todavía no se ha
validado en un emulador o dispositivo**.

## 2. Qué necesitas

| Herramienta | Versión | Para qué |
| --- | --- | --- |
| Git | reciente | El código |
| Docker Desktop | reciente, encendido | Levantar el sistema y las pruebas |
| Node y pnpm | Node 22 o superior, pnpm 11 | Web y comandos de la raíz |
| JDK | 21, con `JAVA_HOME` apuntando a él | Backend |
| Flutter | 3.44.8 (la del CI) | App mobile |

Para solo levantarlo con Docker bastan Git, Docker, Node y pnpm. Comprueba lo
que tienes:

```powershell
node -v; pnpm -v; docker --version; & "$env:JAVA_HOME\bin\java" -version; flutter --version
```

> `java -version` puede mostrar otro Java instalado. El proyecto (Maven y las
> pruebas de navegador) usa el de `JAVA_HOME`.

## 3. Crear tu proyecto

> **Hazlo en una copia nueva.** El inicializador transforma la carpeta donde se
> ejecuta: no lo corras en el repositorio de la plantilla.

### 3.1 Copiar la plantilla

En GitHub, «Use this template» (si el repositorio está marcado como plantilla) y
clona el repositorio nuevo. O clona la plantilla y empieza un historial limpio:

```powershell
git clone <url-de-la-plantilla> acme-tienda
```

```powershell
cd acme-tienda
```

```powershell
Remove-Item -Recurse -Force .git
```

(En Linux o macOS: `rm -rf .git`.)

```powershell
git init -b main
```

```powershell
git add -A
```

```powershell
git commit -m "chore[REPO]: Copiar la plantilla"
```

Si usaste «Use this template», ya tienes ese primer commit: no borres `.git`.

### 3.2 Ponerle nombre

Primero mira qué va a cambiar, sin escribir nada:

```powershell
node scripts/init-project.mjs --nombre "Acme Tienda" --slug acme-tienda --paquete com.acme --dry-run
```

Si está bien, aplícalo:

```powershell
node scripts/init-project.mjs --nombre "Acme Tienda" --slug acme-tienda --paquete com.acme
```

| Dato | Qué es |
| --- | --- |
| `--nombre` | El nombre que ve la gente en la web y la app |
| `--slug` | Para contenedores, volúmenes, base de datos y paquetes. **Distinto en cada proyecto de la misma máquina** |
| `--paquete` | Tu dominio al revés, en minúsculas, sin guiones (`com.acme`) |

Se usa una sola vez. Revisa con `git status`; para deshacerlo antes del commit:
`git checkout -- . && git clean -fd`.

Guarda el resultado **antes** de instalar nada:

```powershell
git add -A
```

```powershell
git commit -m "chore[REPO]: Inicializar el proyecto"
```

Lo que queda a mano (íconos, colores, licencia) está en
[la guía de puesta en marcha](README.md#4-lo-que-queda-a-mano).

## 4. Levantarlo por primera vez

Si tienes encendido otro proyecto de esta plantilla, deténlo primero
(`docker compose down` en su carpeta): usan los mismos puertos.

```powershell
pnpm install
```

```powershell
pnpm setup:env
```

`setup:env` crea los cuatro `.env` con contraseñas y claves aleatorias. Abre
`.env` y `projects/backend/.env` y cambia **`INITIAL_ADMIN_EMAIL`** por tu
correo, igual en los dos. Comprueba que no falte nada:

```powershell
pnpm setup:env:check
```

**Si ya tienes un PostgreSQL en tu computadora** (puerto 5432), antes de seguir
descomenta `POSTGRES_HOST_PORT=5433` en `.env`. Si además vas a correr el
backend fuera de Docker, cambia `5432` por `5433` en `SPRING_DATASOURCE_URL`, en
`projects/backend/.env`. `setup:env:check` avisa si no coinciden.

```powershell
docker compose up
```

La primera vez tarda: descarga dependencias y compila. Está listo cuando
http://localhost:8080/health responde `"status":"UP"`. Para dejarlo en segundo
plano usa `docker compose up -d`.

| Abre | Para |
| --- | --- |
| http://localhost:5173 | La web |
| http://localhost:8025 | El correo de prueba: **aquí llegan todos los correos** |
| http://localhost:8080/swagger-ui/index.html | La documentación de la API |

**Tu contraseña de administrador** es `INITIAL_ADMIN_PASSWORD` del `.env` de la
raíz. El administrador se crea solo la primera vez, con la base vacía: si
cambias esas variables después, no cambia la cuenta.

## 5. Recorrido por lo que ya funciona

1. **Entra** en la web con tu correo y la contraseña del `.env`.
2. **Cambia el idioma** con el botón de la esquina (también en el login).
3. **Crea un usuario:** «Usuarios» → «Crear usuario», con el rol «Usuario».
4. **Abre su invitación** en http://localhost:8025 y pulsa el enlace **en una
   ventana privada** (para no mezclar las dos sesiones). Elige su contraseña.
5. **Entra con esa cuenta.** No ve las pantallas de administración.
6. **Mis sesiones:** entra con esa misma cuenta en un segundo navegador (o
   perfil). En «Mis sesiones» verás los dos accesos: cierra el otro.
7. **Como administrador:** cambia sus roles o desactiva la cuenta. Su sesión se
   corta.
8. **Sesiones de usuarios:** reactiva la cuenta, entra con ella y después, como
   administrador, búscala por correo y cierra sus sesiones.
9. **Bitácora:** verás lo que acabas de hacer.
10. **Cambiar contraseña** (menú de usuario): cierra todas tus sesiones y llega
    el aviso al correo de prueba.
11. **«¿Olvidaste tu contraseña?»:** llega el enlace y, al usarlo, el aviso.

## 6. El día a día

### Con Docker

```powershell
docker compose up -d
```

```powershell
docker compose logs -f backend
```

```powershell
docker compose down
```

La web recarga sola al guardar. Para que el backend tome un cambio de Java:
`docker compose restart backend`.

> `docker compose down -v` **borra la base de datos** y los demás volúmenes del
> proyecto. Úsalo solo si quieres empezar de cero.

Con Docker, la web usa sus propios `node_modules` dentro del contenedor. Si
agregas una dependencia: `docker compose restart frontend`.

### Backend fuera de Docker

Útil para depurar desde el IDE. Levanta solo lo que necesita:

```powershell
docker compose up -d postgres redis mailpit
```

**Spring Boot no lee `projects/backend/.env` por sí solo.** Cárgalo en la
terminal (o configúralo en tu IDE) y arranca:

```powershell
cd projects/backend
```

```powershell
Get-Content .env | Where-Object { $_ -match '^[A-Z_]+=' } | ForEach-Object { $nombre, $valor = $_ -split '=', 2; Set-Item "env:$nombre" $valor }
```

```powershell
./mvnw spring-boot:run "-Dspring-boot.run.profiles=dev"
```

En Linux o macOS:

```bash
set -a; . ./.env; set +a
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

Si cambiaste `POSTGRES_HOST_PORT`, usa ese puerto en `SPRING_DATASOURCE_URL`.
No lo corras a la vez que el contenedor `backend`: los dos usan el puerto 8080.

### Web fuera de Docker

Desde la raíz, con el contenedor `frontend` detenido:

```powershell
pnpm --dir projects/frontend dev
```

### App mobile

Con el backend encendido y un emulador o dispositivo conectado:

```powershell
cd projects/mobile
```

```powershell
flutter pub get
```

```powershell
flutter run
```

`.env` trae `10.0.2.2`, que desde el emulador de Android es tu computadora. En un
teléfono físico necesita una dirección que el teléfono pueda alcanzar; ten en
cuenta que el backend de desarrollo solo se publica en `127.0.0.1`.

### Pruebas

| Qué | Desde | Comando |
| --- | --- | --- |
| Backend (necesita Docker) | `projects/backend` | `./mvnw test` y `./mvnw checkstyle:check` |
| Web | `projects/frontend` | `pnpm test`, `pnpm lint`, `pnpm build` |
| Mobile | `projects/mobile` | `flutter analyze`, `flutter test` |
| Scripts | raíz | `pnpm test:scripts` |
| Navegador | raíz | `pnpm test:e2e` |
| Producción | raíz | `pnpm test:prod` (baja antes tu stack) |

Antes de la primera prueba de navegador:

```powershell
pnpm --dir projects/frontend exec playwright install chromium
```

## 7. Agregar una función

El orden es siempre el mismo: **historia → tareas → código → pruebas**.

### 7.1 Escribir la historia

Crea `docs/HistoriasDeUsuario/HU01 - Notas.md` con la plantilla de
[HISTORIAS_DE_USUARIO.md](../.claude/rules/HISTORIAS_DE_USUARIO.md): para quién,
qué quiere, reglas, **seguridad** y criterios de aceptación que se puedan
comprobar («Dado que…, el sistema debe…»). Las `HU-INICIAL-*` son de la
plantilla; las tuyas empiezan en `HU01`.

### 7.2 Dividirla en tareas

Una carpeta por historia en `docs/UnidadesTrabajo/`, separada por backend, web y
mobile, según [UNIDADES_DE_TRABAJO.md](../.claude/rules/UNIDADES_DE_TRABAJO.md).
Cada tarea dice qué archivos toca, qué **no** hace y cómo se comprueba.

### 7.3 Si trabajas con Claude Code

Las reglas de `.claude/rules/` se aplican solas según la carpeta. Un flujo que
funciona, revisando cada paso antes de pedir el siguiente:

1. «Quiero que cada usuario guarde notas. Redacta la historia para que la revise.»
2. «Apruebo la historia. Escribe las tareas.»
3. «Implementa las tareas.»

Revisa tú los cambios: las pruebas que escribe quien escribe el código pueden
compartir sus mismos errores.

### 7.4 Backend

Ejemplo: un módulo `notas`.

**Migración** en `projects/backend/src/main/resources/db/migration/`, con nombre
`V{yyyyMMddHHmmss}__descripcion.sql`. La fecha tiene que ser **posterior** a la
de la última migración que haya. Un módulo con identidad propia puede tener su
schema (en inglés); la regla para decidirlo está en
[SCHEMAS_BD_BACKEND.md](../.claude/rules/backend/SCHEMAS_BD_BACKEND.md).

```sql
CREATE SCHEMA IF NOT EXISTS notes;

GRANT USAGE ON SCHEMA notes TO app_runtime;
ALTER DEFAULT PRIVILEGES IN SCHEMA notes
    GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO app_runtime;
ALTER DEFAULT PRIVILEGES IN SCHEMA notes GRANT USAGE ON SEQUENCES TO app_runtime;

CREATE TABLE notes.nota (
    id             BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    codigo_nota    UUID         NOT NULL UNIQUE,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    deleted_at     TIMESTAMPTZ,
    -- Hacia otro schema se referencia el código, nunca el id.
    usuario_codigo UUID         NOT NULL REFERENCES authentication.usuario (codigo_usuario),
    titulo         VARCHAR(150) NOT NULL
);

CREATE INDEX idx_nota_usuario ON notes.nota (usuario_codigo);

ALTER TABLE notes.nota ENABLE ROW LEVEL SECURITY;
ALTER TABLE notes.nota FORCE ROW LEVEL SECURITY;

CREATE POLICY nota_aislamiento ON notes.nota
    USING (usuario_codigo = nullif(current_setting('app.current_user', true), '')::uuid)
    WITH CHECK (usuario_codigo = nullif(current_setting('app.current_user', true), '')::uuid);
```

En el mismo cambio actualiza [`BASE_DE_DATOS.mermaid`](BASE_DE_DATOS.mermaid) y,
si creaste un schema, la tabla de la regla de schemas.

**Código** en `features/notas/`:

| Pieza | Qué hace |
| --- | --- |
| `model/Nota` | Extiende `BaseModel`; `@Table(name = "nota", schema = "notes")` y `@AttributeOverride` para `codigo_nota` |
| `repository/` | `JpaRepository<Nota, Long>` |
| `dto/` | `record` de entrada (sin campos que el cliente no deba decidir) y de salida |
| `service/` | La lógica. Dentro de la transacción, primero `rlsContext.usarUsuario(codigo)`; sin eso no se ve ninguna fila |
| `controller/` | Delgado: valida, llama al service y responde `ApiResponse`. El usuario sale de `@CurrentUserId` |
| `constant/` | `NotaErrorCodes` y `NotaAcciones` (los eventos de la bitácora) |

- Los listados van paginados (`common/util/Paginacion`).
- Cada error tiene código (`ERR_NOT_01`) y se documenta en
  [`docs/errors/`](errors/README.md).
- Los cambios que crean, editan o borran se registran en la bitácora.
- **Textos:** crea `resources/i18n/notas/messages.properties` y
  `messages_en.properties`, y **registra el módulo en dos sitios**: la lista de
  `common/idioma/Mensajes.java` y la expresión `CLAVE_USADA` de
  `ArchivosDeIdiomaTest`. Las pruebas fallan si falta alguno.
- **Pruebas** con `app_runtime` (`support/PostgresRuntimeIntegrationTest`),
  incluida la de que **una persona no ve ni cambia las notas de otra**.

Si cambiaste la API, actualiza el contrato con el backend de desarrollo
encendido, y revisa el cambio:

```powershell
pnpm openapi:sync
```

```powershell
pnpm openapi:types
```

Guíate por `features/usuarios/`.

### 7.5 Web

En `projects/frontend/src/features/notas/`: `api/`, `components/`, `language/`,
`schemas/`, `screens/`, `tests/`, `types/` y un `index.ts` en cada carpeta de
código (menos `tests/` y `language/`). Después:

1. **Idiomas:** `language/es.json` y `en.json`, y decláralos en
   `src/i18next.d.ts`. Para que la prueba compruebe también las claves escritas
   como `notas:...`, suma `notas` a la expresión de
   `shared/language/sinTextoFijo.test.ts`.
2. **Ruta**, con nombre en inglés: `routes/_authenticated/notes.tsx` (para todos
   los roles, con `LayoutPorRol`) o dentro de `_authenticated/_admin/` (solo
   administradores).
3. **Menú:** agrega el enlace en `layouts/{rol}/{rol}NavItems.ts`, su clave en
   `ClaveDeMenu` (`layouts/navegacion/navegacion.ts`) y el texto en
   `shared/language/`.
4. Ejecuta `pnpm dev` o `pnpm exec vite build` una vez para regenerar
   `routeTree.gen.ts` antes de `pnpm build`.

Las peticiones van por `apiClient` y TanStack Query; los formularios, con Zod
(sus mensajes son claves de idioma). Una pantalla nunca decide la seguridad:
eso lo hace el backend.

### 7.6 Mobile

En `projects/mobile/lib/features/notas/`: `models/`, `services/`, `providers/`,
`screens/` y `language/`.

1. Declara `es.json` y `en.json` en `pubspec.yaml` y la carpeta en
   `carpetasDeTextos` (`lib/shared/language/app_texts.dart`).
2. Agrega la ruta, en inglés, en `lib/core/routing/app_router.dart` y cómo se
   llega desde el shell del rol.
3. Las llamadas usan el `ApiClient` común.

### 7.7 Antes de darlo por terminado

- Las pruebas de cada capa que tocaste, en verde.
- Casos de acceso **denegado**, no solo el camino feliz.
- Textos en los dos idiomas.
- Documentos al día: diagrama, errores, contrato y estado de la historia.
- Si es un flujo clave, una prueba de navegador en `projects/frontend/e2e/`.

## 8. Guardar y publicar los cambios

Rama nueva desde `main` actualizado:

```powershell
git switch -c feature/notas
```

Cada commit toca **una sola parte** y sigue `tipo[SCOPE]: descripción`:

| Scope | Qué incluye |
| --- | --- |
| `BACK` / `FRONT` / `MOBILE` | `projects/backend`, `frontend`, `mobile` |
| `INFRA` | `infrastructure/` y los compose |
| `DOCS` | `docs/` y documentación suelta de los proyectos |
| `REPO` | Raíz, `scripts/`, `.claude/`, `.github/` |

```powershell
git add projects/backend
```

```powershell
git commit -m "feat[BACK]: Guardar y listar las notas propias"
```

Al hacer commit, los hooks revisan el mensaje y ejecutan el linter de lo que
cambiaste (necesitan las herramientas de esa parte instaladas). **No ejecutan
las pruebas.**

El **CI corre al abrir un Pull Request hacia `main`** (y al subir a `main`); un
push a otra rama no lo dispara. Fusiona solo con todo en verde.

## 9. Producción

`docker-compose.prod.yml` está preparado (HTTPS, sin root, solo el proxy publica
puertos) y se prueba en local con `pnpm test:prod`. **No se ha probado en un
servidor ni con un proveedor de correo reales.** Necesitarás servidor, dominio,
certificado y su renovación, un SMTP (con STARTTLS) con SPF y DKIM, programar
los respaldos y llevarlos fuera del servidor. Todo está en
[INFRAESTRUCTURA.md](INFRAESTRUCTURA.md#7c-docker-composeprodyml).

## 10. Problemas comunes

| Síntoma | Causa y solución |
| --- | --- |
| `docker compose` dice que falta una variable | Falta el `.env` o quedó un `CAMBIAME`: `pnpm setup:env` y `pnpm setup:env:check` |
| Puerto 5432 ocupado | Ya tienes un Postgres: descomenta `POSTGRES_HOST_PORT` en `.env` |
| Cambié una contraseña de la base y ya no conecta | El `.env` no cambia un Postgres ya creado. Restaura el valor anterior o, si los datos no importan, `docker compose down -v` |
| No puedo entrar como administrador | Se creó con los valores del primer arranque. Usa «¿Olvidaste tu contraseña?» y el correo de prueba |
| No llega ningún correo en desarrollo | No salen a internet: míralos en http://localhost:8025 |
| `UnsupportedClassVersionError` o Maven no compila | Se está usando otro Java: apunta `JAVA_HOME` al JDK 21 |
| El backend suelto no arranca por una variable | No cargaste su `.env` (ver [Backend fuera de Docker](#backend-fuera-de-docker)) |
| `flutter analyze`: `asset_does_not_exist` | Falta `projects/mobile/.env`: `pnpm setup:env` |
| Las pruebas de backend salen «skipped» | Docker no está accesible; en Windows, revisa «Allow the default Docker socket to be used» en Docker Desktop |
| `pnpm test:e2e` se niega a arrancar | Hay un puerto ocupado o quedaron restos: el mensaje dice cuál. Ver [scripts/README.md](../scripts/README.md) |
| `pnpm test:prod` se niega a arrancar | Tu stack está encendido: `docker compose down` (sin `-v`) |
| El CI falla en `gitleaks` por un texto de prueba | Si de verdad es inventado, márcalo como dice [CI_CD.md](../.claude/rules/CI_CD.md#falsos-positivos-de-gitleaks). **Si es un secreto real, cámbialo** |
| Quiero quitar MinIO o JasperReports | Ningún código los usa. MinIO: sus servicios en los dos compose, sus variables y las pruebas de `scripts/` que revisan los compose. JasperReports: sus tres dependencias en `pom.xml`. Vuelve a ejecutar las pruebas |
