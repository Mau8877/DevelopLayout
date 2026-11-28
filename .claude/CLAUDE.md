# DevelopLayout

Plantilla base de arquitectura full-stack. Monorepo con pnpm workspaces
(`pnpm-workspace.yaml`: `projects/*`) que contiene tres
proyectos independientes más la infraestructura de desarrollo:

- **`projects/backend`** — API REST en Java 21 + Spring Boot 4.2 (Maven),
  PostgreSQL + Flyway para persistencia. Redis guarda límites de intentos,
  sesiones revocadas y tokens de un solo uso. MinIO (archivos) y JasperReports
  (reportes) están preparados, pero **ningún código los usa todavía**.
- **`projects/frontend`** — Panel web en React 19 + TypeScript + Vite,
  TanStack (Query/Router/Form/Table), Tailwind CSS 4 + shadcn.
- **`projects/mobile`** — App en Flutter/Dart, Riverpod para estado,
  `go_router` para navegación, `dio` como cliente HTTP.
- **`scripts/`** — inicializador, `.env`, respaldos, contrato de la API, pruebas
  de navegador y de producción, y `update-deps` (ver
  [scripts/README.md](../scripts/README.md)).
- **`infrastructure/`** — configuración de Docker Compose: nginx (proxy),
  postgres, redis, minio, prometheus, grafana. `docker-compose.yml` es solo
  desarrollo; `docker-compose.prod.yml` es la base de producción (TLS, sin
  `docker.sock`, solo el proxy publica puertos).

## Qué trae ya

Autenticación completa (cookies en web, tokens en mobile), recuperación y cambio
de contraseña, sesiones propias y de administrador, usuarios, invitaciones y
roles, bitácora, dos idiomas, y CI con pruebas de navegador y de producción. El
estado de cada flujo está en `docs/HistoriasDeUsuario/`. CD **no** existe: falta
decidir dónde se despliega.

## Comandos básicos

### Backend (`projects/backend`)

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev   # correr en modo dev
./mvnw test                                              # tests
./mvnw clean package                                     # build
```

### Frontend (`projects/frontend`)

```bash
pnpm dev       # servidor de desarrollo (Vite, puerto 5173)
pnpm build     # build de producción
pnpm lint      # ESLint
pnpm test      # Vitest
```

Desde la raíz: `pnpm test:e2e` (navegador), `pnpm test:prod` (producción),
`pnpm test:scripts`, y `pnpm openapi:sync` + `pnpm openapi:types` (contrato).

### Mobile (`projects/mobile`)

```bash
flutter pub get     # instalar dependencias
flutter run          # correr en un emulador/dispositivo conectado
flutter test          # tests
flutter analyze       # análisis estático
```

### Stack completo (Docker Compose)

Desde la raíz del repo, con los `.env` ya creados (`pnpm setup:env` los crea
con secretos aleatorios; ninguna contraseña tiene valor por defecto):

```bash
docker compose up
```

Levanta proxy (puerto 80), frontend (5173), backend (8080), postgres (5432),
redis (6379), minio (9000/9001), mailpit (8025/1025), prometheus (9090) y grafana (3000), todos
publicados solo en `127.0.0.1`. Ver [docs/INFRAESTRUCTURA.md](../docs/INFRAESTRUCTURA.md)
para el detalle de servicios y variables de entorno, y
[docs/README.md](../docs/README.md) para el checklist de puesta en marcha de
un proyecto nuevo.

## Seguridad — regla transversal obligatoria

Todo el proyecto cumple **OWASP Top 10:2025**, **API Security Top 10:2023**
y **MASVS 2.1.0**. Antes de implementar cualquier cosa, leer
[SEGURIDAD_OWASP.md](rules/SEGURIDAD_OWASP.md). La base de datos es siempre
privada y con RLS por defecto
([BASE_DE_DATOS_PRIVADA_RLS.md](rules/backend/BASE_DE_DATOS_PRIVADA_RLS.md));
los reportes usan JasperReports 7
([REPORTES_JASPERREPORTS.md](rules/backend/REPORTES_JASPERREPORTS.md)).
Toda HU y UT lleva su sección de Seguridad.

## Idiomas y direcciones

Ningún texto visible va escrito en el código: web, mobile y backend usan
archivos de idioma en español e inglés neutros
([IDIOMAS.md](rules/IDIOMAS.md)). Las URL van en inglés. El recorrido de uso de
la plantilla está en [docs/TUTORIAL.md](../docs/TUTORIAL.md).

## Cómo se agrega una función

1. Historia de Usuario con sus criterios
   ([HISTORIAS_DE_USUARIO.md](rules/HISTORIAS_DE_USUARIO.md)).
2. Unidades de Trabajo por capa
   ([UNIDADES_DE_TRABAJO.md](rules/UNIDADES_DE_TRABAJO.md)).
3. Código con sus pruebas, incluido el caso de acceso **denegado**.
4. En el mismo cambio, lo que corresponda de la tabla de abajo.

| Si cambia... | Se actualiza también |
| --- | --- |
| Un endpoint o un DTO | `pnpm openapi:sync` y `pnpm openapi:types` |
| La base (migración nueva, nunca una ya aplicada) | `docs/BASE_DE_DATOS.mermaid` |
| Un código de error | `docs/errors/` |
| Una excepción o política abierta de RLS | `docs/seguridad/RLS_EXCEPCIONES.md` |
| Un texto visible | Los archivos `es` y `en`, y las pruebas de navegador que lo busquen |
| Un módulo con textos | Sus registros (ver la regla de idiomas de cada capa) |
| La instalación, los comandos o el uso | `docs/TUTORIAL.md`, `scripts/README.md` y el README que toque |

Una historia pasa a `Hecha` solo con todos sus criterios comprobados. Lo que no
se pudo probar se dice.

## Trampas conocidas

- **Spring no lee los `.env`.** Con Docker Compose llegan solos; para correr el
  backend fuera de Docker hay que cargar las variables (ver el tutorial).
- **El backend necesita JDK 21.** `mvnw` usa `JAVA_HOME`; el `java` del `PATH`
  puede ser otra versión.
- **Sin Docker, las pruebas con Testcontainers quedan omitidas:** un resultado
  verde con omitidas no probó la base ni RLS.
- **Contraseñas o tokens inventados en una prueba** llevan `gitleaks:allow` en el
  mismo commit, o falla el CI ([CI_CD.md](rules/CI_CD.md#falsos-positivos-de-gitleaks)).
- **`project:init` renombra la plantilla** con una lista cerrada de nombres: no se
  arman nombres juntando trozos (ver [scripts/README.md](../scripts/README.md)).

## Reglas del proyecto (`.claude/rules/`)

**Antes de trabajar en cualquier archivo del proyecto, hay que respetar las
reglas correspondientes en `.claude/rules/`** según la carpeta/proyecto en
el que se esté trabajando (backend, frontend, mobile, o transversal a todo
el repo). No es opcional ni una sugerencia — son las decisiones ya tomadas
sobre cómo se construye este proyecto específico, y anulan cualquier
default genérico de "buenas prácticas" cuando entran en conflicto.

El propio frontmatter `globs:` de cada archivo de regla determina en qué
carpetas aplica automáticamente — no hace falta ir a buscarlas a mano, pero
si algo no queda claro para el archivo que se está por tocar, se revisa la
carpeta `.claude/rules/` correspondiente antes de asumir un criterio propio.

Los archivos sin `globs:` aplican a todo el repositorio. Si una regla y el código
no coinciden, **no se elige en silencio**: se avisa a la persona y se corrige la
que esté mal en el mismo cambio. Una desviación aceptada se escribe en la regla,
con su motivo.

## Commits de Git — Claude no commitea el trabajo final, salvo pruebas que deshace

**Claude nunca deja un commit permanente sin que la persona lo haya
pedido.** Decidir cuándo y qué commitear como entrega final del trabajo es
responsabilidad exclusiva de la persona que está usando Claude.

Cuando el trabajo esté listo para commitear, Claude debe:

1. Seguir [CONVENCIONES_GIT.md](rules/CONVENCIONES_GIT.md) al armar el
   mensaje: formato `tipo[SCOPE]: descripción concreta`, y **un commit por
   scope** (si el cambio toca backend y frontend, son dos mensajes
   separados, no uno).
2. Entregar los comandos **siempre en el formato de abajo**, explicando
   brevemente qué agrupa cada commit si hay más de uno.
3. Dejar que la persona misma corra el bloque — Claude no ejecuta
   `git add`/`git commit`/`git push` como entrega final salvo que se lo pidan
   explícitamente.

### Formato obligatorio de entrega

**Un solo bloque `powershell`** con todo, para copiar y ejecutar de una vez. No
se dan los comandos sueltos ni un bloque por commit.

```powershell
& {
  function Commit($mensaje, [string[]]$archivos) {
    git add -- $archivos
    if ($LASTEXITCODE -ne 0) { throw "Falló git add: $mensaje" }
    git commit -m $mensaje
    if ($LASTEXITCODE -ne 0) { throw "Falló git commit: $mensaje" }
  }
  if ((git branch --show-current) -ne 'feature/modulo') { throw 'No estás en feature/modulo' }
  Commit 'feat[BACK]: descripción concreta' @('projects/backend')
  Commit 'docs[DOCS]: descripción concreta' @('docs/archivo.md', 'docs/otro.md')
  git push origin feature/modulo
  if ($LASTEXITCODE -ne 0) { throw 'Falló git push' }
}
```

- Comprueba la rama y **se detiene en el primer fallo** (también si el
  pre-commit o `commitlint` rechazan un commit).
- Una llamada a `Commit` por commit. Sus rutas cubren **todos** los cambios
  pendientes y solo los de su scope; nunca `git add .` ni `-A`.
- Mensajes y rutas entre comillas simples.
- **Sin línea `Co-Authored-By`** ni otras firmas.
- Antes de entregarlo, Claude valida cada mensaje con `commitlint` y contrasta
  las rutas con `git status`.
- Si un bloque falló a medias, se entrega uno nuevo solo con lo que falta.

**Excepción: probar que un hook de git dispara de verdad.** Claude sí puede
hacer un `git commit` puntual con el único fin de confirmar que
`pre-commit`/`commit-msg` disparan en un commit real (no simulado) —
**siempre y cuando lo deshaga inmediatamente después** con
`git reset --soft HEAD~1` (deja los cambios de vuelta como estaban,
staged, sin perder nada) y avise a la persona que hizo esto. Nunca se deja
como commit definitivo sin que se lo pidan.

Para todo lo demás que no necesite un commit real de por medio (probar que
un mensaje cumple el formato, o que `lint-staged` corre el linter
correcto), usar los mecanismos que ni siquiera tocan el historial de git:

```bash
pnpm exec lint-staged                     # simula el pre-commit sobre lo que esté staged
sh .husky/commit-msg archivo_con_mensaje.txt  # simula el commit-msg sin commitear
echo "feat[BACK]: mensaje de prueba" | pnpm exec commitlint  # valida un mensaje suelto
```
