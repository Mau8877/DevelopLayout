---
globs: projects/backend/**/*
---

# Seguridad y Autenticación — Backend

## Estado actual

| Pieza | Estado |
| --- | --- |
| Login, `/me`, refresh, logout y token CSRF | Implementado (HU Base, Login, Refresh) |
| Cierre de sesión | Backend y web probados; falta la validación mobile en emulador (HU Logout) |
| Recuperación y cambio de contraseña, envío de correo | **No implementado** |
| Gestión de usuarios y roles | **No implementada**. Antes necesita políticas RLS de administrador: `usuario` y `usuario_rol` solo permiten la fila propia o la del login, y `RlsContext` no fija `app.is_admin` |
| Administrar sesiones y consultar la bitácora | **No implementado** |

El estado de cada flujo vive en `docs/HistoriasDeUsuario/`.

> **Riesgo pendiente:** `sesion` guarda un solo hash anterior. Un token robado
> y rotado dos veces antes de que vuelva el usuario legítimo ya no se detecta
> como reutilización (ver la HU de Refresh). Resolverlo antes de manejar datos
> sensibles.

## Flujo: JWT + refresh token (web: solo cookies)

> **Decisión de diseño (revisada): en web, TODO viaja en cookies
> `HttpOnly`; nada de tokens en `localStorage`/`sessionStorage` ni en
> memoria de JavaScript.** Antes se había elegido un esquema híbrido
> (access token en memoria + refresh en cookie). Se cambió por pedido
> explícito del proyecto: ningún token es legible por JavaScript, lo que
> reduce el robo de credenciales por XSS. El costo es que una cookie se
> adjunta sola, así que **todos los endpoints de negocio quedan expuestos
> a CSRF y necesitan protección anti-CSRF** (ver abajo). No se vuelve a
> guardar un token en el storage del navegador.

- **Access token**: JWT firmado de corta duración (15 minutos) con el
  `codigo` del usuario (`sub`) y la lista de roles activos como claims,
  nada más. Viaja en la cookie `accessToken`; el backend también lo acepta
  en `Authorization: Bearer` para clientes que no son navegador (mobile).
- **Refresh token**: opaco (UUID aleatorio, no JWT), 7 días, rotado en cada
  uso. Se guarda **solo su hash** en la tabla `sesion` (Postgres es la fuente
  de verdad: una fila por inicio de sesión, con tipo de cliente, IP, user
  agent, estado y fechas). Viaja en la cookie `refreshToken` y solo se usa
  contra `/api/v1/auth/refresh` y `/logout`. La sesión dura 7 días desde su
  último uso, con un máximo absoluto de 30 días desde su creación.
- **Identificador de sesión (`sid`)**: el access token lleva el `codigo` de su
  sesión. Al revocar una sesión, su `sid` entra en una lista de revocadas en
  Redis (TTL = vida del access token) que el filtro JWT consulta en cada
  request: una sesión cerrada deja de valer **de inmediato**. Si Redis falla,
  se deniega (falla cerrado).
- **Cookies (web)**:

  | Cookie | `accessToken` | `refreshToken` |
  | --- | --- | --- |
  | Atributos comunes | `HttpOnly`; `SameSite=Strict`; `Secure` en producción (`app.cookies.secure`, `false` en `dev`) | igual |
  | `Path` | `/api` | `/api/v1/auth` |
  | `Max-Age` | 15 minutos | 7 días |
  | La setea | login y refresh | login y refresh (rota) |
  | La borra | logout (`Max-Age=0`) | logout (`Max-Age=0`) |

  Frontend y backend son same-site (puertos distintos en dev, mismo origen
  tras nginx en producción), por eso `SameSite=Strict` funciona con
  `fetch`/axios. El cliente HTTP usa `withCredentials: true` y
  `SecurityConfig` ya tiene `allowCredentials(true)` en CORS.
- **Protección CSRF (obligatoria con cookies)**: patrón *double submit*
  de Spring Security (`CookieCsrfTokenRepository.withHttpOnlyFalse()` +
  `CsrfTokenRequestAttributeHandler`). El backend setea la cookie
  `XSRF-TOKEN` (legible por JS, no es una credencial) y el cliente la
  devuelve en el header `X-XSRF-TOKEN` en todo `POST/PUT/PATCH/DELETE`
  (axios lo hace solo con `xsrfCookieName`/`xsrfHeaderName`). Las
  peticiones con `Authorization: Bearer` (mobile) no usan cookies y quedan
  exentas. Además, `CORS` limita los orígenes a `CORS_ALLOWED_ORIGINS`.
  `SecurityConfig` desactiva la rotación del token CSRF por autenticación
  (`NullAuthenticatedSessionStrategy`): sin sesión de servidor, cada request
  con JWT cuenta como un inicio de sesión y borraba la cookie `XSRF-TOKEN`.
  MockMvc no lo detecta; se prueba contra un servidor real
  (`AuthCsrfCookieTest`).
- **Clientes mobile (excepción)**: una app no tiene cookies del navegador.
  Con el header `X-Client-Type: mobile`, login y refresh devuelven ambos
  tokens en el **body** y la app los guarda en `flutter_secure_storage`
  (Keystore/Keychain, MASVS-STORAGE); refresh y logout reciben el refresh
  token en el body. Sin ese header se usa siempre cookie.
- **Endpoints** (`/api/v1/auth/...`):
  - `GET /csrf` → entrega la cookie `XSRF-TOKEN` (204).
  - `POST /login` → valida credenciales y responde `data.usuario`
    (`codigo`, `email`, `roles`). Web: los dos tokens van en cookies y no en
    el body. Mobile: además `accessToken` y `refreshToken` en el body.
  - `GET /me` → usuario y roles de la sesión vigente (privado).
  - `POST /refresh` → web lee **solo** la cookie y mobile **solo** el body
    (mobile está exento de CSRF, así que nunca se autentica con una cookie).
    Rota el hash bloqueando la fila y responde igual que el login. Una sesión
    vencida pasa a `EXPIRADA`. Si Redis o la base fallan, no entrega tokens.
  - `POST /logout` → revoca la sesión (`REVOCADA`), la agrega a la lista de
    revocadas y, en web, borra las cookies. Siempre 200 (idempotente).
- **Reutilización**: si llega un refresh token que ya fue rotado (coincide con
  el hash anterior de una sesión activa), se asume robo: la sesión se revoca con
  motivo `REUSO` y se registra en la bitácora ([BITACORA_BACKEND.md](BITACORA_BACKEND.md)).
  La revocación se confirma aunque la respuesta sea 401 (`noRollbackFor`).
- **Clientes**: renuevan de a una vez y solo cierran la sesión local si el
  refresh responde 401 o 403; sin red, con 429 o con 500 la conservan. En web
  las pestañas se coordinan con un candado compartido (ver
  [TANSTACK_QUERY_FRONTEND.md](../frontend/TANSTACK_QUERY_FRONTEND.md#renovación-de-sesión)).
- Redis guarda el contador de intentos de login y la lista de sesiones
  revocadas (a futuro, los tokens de recuperación de contraseña). **No**
  guarda refresh tokens. Corre con `maxmemory-policy noeviction`: no puede
  desalojar esos datos. El rate limit vive en memoria, no en Redis.

## Usuarios y validación de credenciales

- `Usuario` (`features/usuarios/model/Usuario.java`, tabla
  `authentication.usuario`, extiende `BaseModel`) tiene `email` (identificador
  de acceso, único entre filas no borradas e insensible a mayúsculas),
  `passwordHash` (BCrypt) y `estado` (`ACTIVO` | `INACTIVO`). No hay
  `username`. El identificador público es `codigo`.
- `AuthService.login()` busca el usuario con RLS (`app.login_email`) y compara
  con `PasswordEncoder` (`BCryptPasswordEncoder`). **No usa
  `AuthenticationManager`.** Si el correo no existe compara contra un hash
  ficticio, para igualar tiempos.
- `UserDetailsServiceImpl` existe en `security/` y evita que Spring Boot cree
  su usuario en memoria; el flujo de login no lo invoca.
- `JwtAuthenticationFilter` (`OncePerRequestFilter`, creado en
  `SecurityConfig`, no es `@Component`) valida el access token, consulta la
  lista de sesiones revocadas y puebla el `SecurityContext`.
- El primer `SUPER_ADMIN` lo crea `AdminInicialInitializer` con
  `INITIAL_ADMIN_EMAIL` e `INITIAL_ADMIN_PASSWORD`.

## Roles y permisos

**Modelo relacional, no un enum fijo** — un `Usuario` puede tener **varios
roles activos** a la vez, vía las entidades `Rol` (catálogo de roles, con
`nombre`, `descripcion`, `isActive`) y `UsuarioRol` (tabla de asociación
con su propio `codigo`, `isActive` y timestamps — **no** es un
`@ManyToMany` simple de JPA, porque esa fila de asociación tiene datos
propios más allá de las dos FK, así que es una entidad propia con sus dos
`@ManyToOne`). Ambas entidades viven en `features/usuarios/model/` (mismo
dueño del dato que `Usuario`). Si el alcance del proyecto no necesita
varios roles por usuario, un esquema más simple (un enum fijo de 2-3
roles, columna `role` en `Usuario`) sigue siendo válido — este modelo
relacional es el punto de partida recomendado cuando se sabe de antemano
que va a hacer falta esa flexibilidad.

- Un rol solo cuenta como activo para un usuario si tanto la fila
  `UsuarioRol.isActive` como el propio `Rol.isActive` son `true` — un rol
  desactivado globalmente (`Rol.isActive = false`) deja de otorgar acceso
  a todos los usuarios que lo tengan asignado, sin tener que tocar cada
  fila de `UsuarioRol`.
- Los nombres de rol conocidos por el sistema (ej. `"SUPER_ADMIN"`, `"USUARIO"`)
  se validan/usan como constantes de texto (`RolNombres` o similar, en
  `features/usuarios/`), **no** como un enum Java cerrado — los roles son
  datos (filas de la tabla `rol`), administrables sin desplegar código
  nuevo.
- El JWT lleva la lista de nombres de roles activos como claim (array de
  strings, ver [Flujo JWT](#flujo-jwt--refresh-token-web-solo-cookies)): lo arma
  `AuthService` con los roles leídos de la base en el login y en cada refresh, y
  `JwtAuthenticationFilter` lo convierte en `authorities` `ROLE_{nombre}`.
- Se protege por rol con `@PreAuthorize("hasRole('SUPER_ADMIN')")` (o
  `hasAnyRole(...)` si aplica a más de uno) a nivel de método de `service`
  (no en el controller — mantiene la regla de controllers delgados de
  [ENDPOINTS_BACKEND.md](ENDPOINTS_BACKEND.md)). Funciona gracias a
  `@EnableMethodSecurity` en `SecurityConfig`: sin esa anotación todo
  `@PreAuthorize` se ignora en silencio (`ErroresYPermisosTest` lo cubre).
- **No confundir con el bloqueo temporal por intentos fallidos de login**:
  ese bloqueo es un estado transitorio (ej. en Redis), no un valor de
  `Usuario.estado` ni de ningún rol — son dos mecanismos independientes.

Si en algún momento un módulo necesita permisos más finos que roles
(permisos granulares por acción/recurso), se define en su propia regla
puntual — no se expande este esquema global de forma especulativa.

## Obtener el usuario autenticado en los controllers

No se extrae el JWT manualmente en cada endpoint (`request.getHeader("Authorization")`,
parsear, etc.). Se usa una anotación custom que resuelve el dato directo
desde el `SecurityContext`:

```java
@GetMapping("/{id}/pedidos")
public ResponseEntity<ApiResponse<List<PedidoResponse>>> misPedidos(@CurrentUserId UUID userId) {
    ...
}
```

`UUID`, no `Long` — es el `codigo` del usuario (el mismo valor del claim
`sub` del JWT), el identificador público que expone `BaseModel` (ver
[CONVENCIONES_JAVA_BACKEND.md](CONVENCIONES_JAVA_BACKEND.md)).

La anotación `@CurrentUserId` (y su `HandlerMethodArgumentResolver`) se
define una sola vez en `security/` — ver
[CONVENCIONES_JAVA_BACKEND.md](CONVENCIONES_JAVA_BACKEND.md) para dónde vive
exactamente el archivo, para no terminar con dos definiciones de la misma
anotación en dos lugares distintos.

## Rate limit

**Ya implementado** — `RateLimitFilter` (`security/RateLimitFilter.java`),
registrado en `SecurityConfig` con `addFilterBefore(rateLimitFilter,
UsernamePasswordAuthenticationFilter.class)` (corre antes que la
autenticación: un cliente sin token igual consume su cupo, no es gratis
intentar de más).

- **Alcance**: solo `/api/v1/**` (`shouldNotFilter` excluye todo lo demás).
  Explícitamente **no** aplica a `/health`, `/prometheus` ni
  `/swagger-ui/**`/`/v3/api-docs/**` — `/prometheus` en particular recibe
  scrape de Prometheus cada 5s (ver `infrastructure/prometheus/prometheus.yml`)
  y lo bloquearía por error si estuviera dentro del alcance.
- **Corre por prefijo de URL, no por endpoint real.** El filtro se ejecuta
  a nivel servlet, **antes** de que Spring resuelva si existe un
  `@RestController` que atienda esa ruta. Esto significa que una IP
  consume cupo igual pegándole a un endpoint real que a una URL bajo
  `/api/v1/**` que ni siquiera existe todavía (un 404) — no hace falta que
  el feature esté implementado para que el rate limit ya esté activo sobre
  su futura ruta.
- **Algoritmo**: token bucket vía **Bucket4j** (`com.bucket4j:bucket4j_jdk11-core`,
  no `bucket4j-core` — esa coordinada vieja ya no es la que resuelve Maven
  Central; se verificó contra el índice de Maven Central antes de fijar la
  versión). Un bucket por IP (`ConcurrentHashMap<String, Bucket>`), refill
  "greedy" (los tokens se regeneran continuo, no en ráfagas cada N segundos).
- **Límites propios por IP**, además del global: login 10/min
  (`app.ratelimit.login-*`) y refresh 30/min (`app.ratelimit.refresh-*`).
- **Config** (`application.properties`, sobreescribible por env var):
  `app.ratelimit.capacity` (default `100`) y
  `app.ratelimit.refill-per-minute` (default `100`) → `RATE_LIMIT_CAPACITY` /
  `RATE_LIMIT_REFILL_PER_MINUTE`.
- **Respuesta al superar el límite**: `429`, con el mismo sobre de
  [RESPONSES_BACKEND.md](RESPONSES_BACKEND.md), código `ERR_SYS_02` (ver
  [EXCEPCIONES_BACKEND.md](EXCEPCIONES_BACKEND.md)). Se escribe **a mano**
  (`response.getWriter().write(...)`), no vía `ApiResponse.error(...)` ni
  `GlobalExceptionHandler` — el filtro corre a nivel servlet, antes de que
  exista un `DispatcherServlet`/`@RestControllerAdvice` que pueda intervenir.
  Igual respeta el contrato exacto del sobre para que el cliente (frontend/
  mobile) lo trate igual que cualquier otro error de la API.
- **Identificación del cliente**: `request.getRemoteAddr()`. Alcanza hoy
  porque nginx es el único proxy delante del backend dentro de la red de
  Docker. Si en el futuro hay más de un proxy en la cadena, hay que leer
  `X-Forwarded-For` con cuidado (el primer valor, no el último — un cliente
  puede falsificar ese header).

> **Limitación conocida, aceptada a propósito:** el bucket vive en memoria
> del proceso (`ConcurrentHashMap`), no es distribuido. Con una sola
> instancia del backend (el caso de este proyecto) funciona perfecto. Si en
> algún momento se corre más de una réplica, cada una tendría su propio
> límite independiente (un atacante repartiendo requests entre instancias
> efectivamente multiplica su cupo). Para eso existe
> `bucket4j_jdk11-redis` (Redis ya está en el stack y
> `spring-boot-starter-data-redis` ya está en el `pom.xml`) — no se implementa hasta que haya
> más de una instancia real corriendo.

> **Esto no reemplaza protección a nivel de infraestructura.** Un rate
> limit en la app (como este) es la última línea de defensa. nginx
> (`nginx.conf` y `nginx.prod.conf`) aplica además `limit_req` sobre `/api/`
> (20 r/s, ráfaga de 40) y corta antes de llegar a la JVM.

## Endpoints públicos

Sin autenticación solo quedan los declarados en `SecurityConfig`: `/health`,
`/health/**`, `/prometheus`, `/swagger-ui/**`, `/v3/api-docs/**`,
`GET /api/v1/auth/csrf` y `POST /api/v1/auth/login`, `/refresh` y `/logout`
(en los dos últimos la credencial es el refresh token y en web el CSRF sigue
activo). No hay registro público. Cualquier otro endpoint nuevo es privado por
default: hacerlo público es una decisión explícita.
