---
globs: projects/backend/**/*
---

# Seguridad y Autenticación — Backend

> **Base de seguridad previa (ya aplicada):** el perfil `dev` separa lo
> cómodo de lo seguro, Redis exige password y los puertos de infra están en
> `127.0.0.1`. La autenticación real **aún no está implementada** y se
> hace junto con RLS (rol `app_runtime`, contexto `app.current_user`, ver
> [BASE_DE_DATOS_PRIVADA_RLS.md](BASE_DE_DATOS_PRIVADA_RLS.md)) y las reglas
> de [SEGURIDAD_OWASP.md](../SEGURIDAD_OWASP.md) (A07).
> `server.forward-headers-strategy=native` ya está activo para que el rate
> limit use la IP real del cliente.

## Estado actual (punto de partida)

`SecurityConfig.java` hoy usa el `InMemoryUserDetailsManager` autogenerado
por Spring Boot (usuario/password aleatorio impreso en el log en cada
arranque). Esto es **solo un placeholder de desarrollo** y debe
reemplazarse por la implementación real descrita abajo antes de considerar
la autenticación "hecha" — no es un requisito opcional.

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
  uso. Se guarda **solo su hash** en la tabla `sesiones` (Postgres es la fuente
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
- **Clientes mobile (excepción)**: una app no tiene cookies del navegador.
  Con el header `X-Client-Type: mobile`, login y refresh devuelven ambos
  tokens en el **body** y la app los guarda en `flutter_secure_storage`
  (Keystore/Keychain, MASVS-STORAGE); refresh y logout reciben el refresh
  token en el body. Sin ese header se usa siempre cookie.
- **Endpoints** (`/api/v1/auth/...`):
  - `POST /api/v1/auth/login` → valida credenciales, responde
    `{ accessToken, usuario }` en el body y setea la cookie `refreshToken`.
  - `POST /api/v1/auth/refresh` → lee el refresh token de la cookie, busca
    su hash en `sesiones`, valida que la sesión esté `ACTIVA` y no vencida,
    rota el hash (guardando el anterior para detectar reutilización) y
    responde con un access token nuevo y la cookie nueva.
  - `POST /api/v1/auth/logout` → revoca la sesión (`REVOCADA`), la agrega a la
    lista de revocadas de Redis y borra las cookies.
- **Reutilización**: si llega un refresh token que ya fue rotado (coincide con
  el hash anterior de una sesión), se asume robo: la sesión se revoca con motivo
  `REUSO` y se registra en la bitácora ([BITACORA_BACKEND.md](BITACORA_BACKEND.md)).
- Redis se sigue usando para el bloqueo por intentos de login, los tokens de
  recuperación de contraseña, el rate limit y la lista de sesiones revocadas;
  **no** guarda refresh tokens (cada refresh ya escribe en la base, así que una
  caché no aportaría nada).

## Reemplazo del `InMemoryUserDetailsManager`

- Model `Usuario` (tabla `users`, `features/usuarios/model/Usuario.java`,
  extiende `BaseModel` — ver [ARQUITECTURA_BACKEND.md](../ARQUITECTURA_BACKEND.md))
  con al menos: `username` (único), `correo`/`email` (único), `passwordHash`
  (BCrypt, nunca texto plano), `estado` (enum de negocio — ej.
  `EstadoUsuario { ACTIVO, INACTIVO, ... }`, ver
  [Roles](#roles-y-permisos) para la distinción con el bloqueo temporal
  por intentos fallidos). Todavía no existe — es un requisito de esta
  regla, no algo que ya esté implementado. El identificador público es
  `codigo` (heredado de `BaseModel`), nunca se expone el detalle interno
  de la fila.
- Login puede aceptar `username` o `correo`/`email` indistintamente en el
  mismo campo de entrada si el proyecto lo requiere — el repositorio
  resuelve contra cualquiera de las dos columnas; si no hace falta esa
  flexibilidad, un solo campo de login (`email` o `username`) alcanza.
- `UserDetailsServiceImpl implements UserDetailsService` en `security/`, que
  busca el usuario por su campo de login vía `UsuarioRepository` y arma el
  `UserDetails`/principal con sus roles activos (ver
  [Roles](#roles-y-permisos)) y su `estado` (mapeado a `enabled` de
  `UserDetails` — `enabled = true` solo si `estado == ACTIVO`, para que
  Spring Security dispare `DisabledException` automáticamente en cualquier
  otro estado).
- `SecurityConfig` pasa a usar este `UserDetailsService` + un
  `PasswordEncoder` (`BCryptPasswordEncoder`) en vez del in-memory, vía un
  `AuthenticationManager` (vía `AuthenticationConfiguration`) que el
  `AuthService` del módulo `auth` usa para validar credenciales. El filtro
  de autenticación por JWT (`OncePerRequestFilter` custom) valida el access
  token en cada request y setea el `Authentication` en el
  `SecurityContext`.

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
  datos (filas de la tabla `ROL`), administrables sin desplegar código
  nuevo.
- El JWT lleva la lista de nombres de roles activos como claim (array de
  strings, ver [Flujo JWT](#flujo-jwt--refresh-token)) — `UserDetailsServiceImpl`
  arma las `authorities` como `ROLE_{nombre}` por cada rol activo.
- Se protege por rol con `@PreAuthorize("hasRole('SUPER_ADMIN')")` (o
  `hasAnyRole(...)` si aplica a más de uno) a nivel de método de `service`
  (no en el controller — mantiene la regla de controllers delgados de
  [ENDPOINTS_BACKEND.md](ENDPOINTS_BACKEND.md)).
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
> `spring-boot-starter-data-redis` ya está en el `pom.xml`, agregada para el
> storage de refresh tokens de más arriba) — no se implementa hasta que haya
> más de una instancia real corriendo.

> **Esto no reemplaza protección a nivel de infraestructura.** Un rate
> limit en la app (como este) es la última línea de defensa, no la única —
> nginx (que ya está en `docker-compose.yml` como proxy) también podría
> tener su propio `limit_req_zone`/`limit_req`, más barato en recursos
> porque corta el tráfico antes de que llegue a la JVM. No está configurado
> todavía; si se agrega, va en una regla de infraestructura aparte, no acá.

## Endpoints públicos

Solo quedan sin autenticación los ya definidos en `SecurityConfig`
(`/health`, `/health/**`, `/prometheus`, `/swagger-ui/**`, `/v3/api-docs/**`)
más los de `/api/v1/auth/login`, `/api/v1/auth/refresh` (obviamente, todavía
no hay token) y `/api/v1/auth/register` si el proyecto lo requiere.
Cualquier otro endpoint nuevo es privado por default — si necesita ser
público, es una decisión explícita, no un olvido de agregar la regla en
`SecurityConfig`.
