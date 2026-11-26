# Historia de usuario

| Id | Nombre corto de HU | Prioridad | PHU | Estado |
| --- | --- | --- | --- | --- |
| HU-INICIAL-LOGIN | Iniciar sesión | Alta |  | Hecha |

**Como:** usuario registrado (web o mobile)
**Quiero:** iniciar sesión con mi correo y contraseña
**Para:** acceder a las funciones del sistema según mis roles

**Descripción:**
El usuario envía correo y contraseña. Si son correctos, el sistema responde con los datos del usuario con sus roles, crea una sesión (fila en `sesion` con tipo de cliente, IP y user agent) y emite un access token (JWT, 15 minutos, con el identificador de la sesión) y un refresh token opaco (7 días). En web ambos tokens viajan solo en cookies `HttpOnly` (nada en `localStorage` ni en memoria de JavaScript); en mobile (header `X-Client-Type: mobile`) viajan en el body y la app los guarda en almacenamiento seguro. Si las credenciales son incorrectas, el sistema responde con un mensaje genérico que no revela si el correo existe. Tras 5 intentos fallidos consecutivos desde un mismo origen (IP), ese origen queda bloqueado 15 minutos para esa cuenta; además hay un tope amplio por cuenta que suma todos los orígenes. Una cuenta inactiva no puede iniciar sesión.

Depende de [HU-INICIAL-Base](HU-INICIAL-Base.md).

---

## Conversación/Reglas

**Flujo general**

1. El usuario abre la pantalla de login e ingresa correo y contraseña.
2. El cliente valida formato (correo válido, contraseña no vacía) y envía la solicitud.
3. El sistema valida credenciales, estado de la cuenta y bloqueo.
4. El sistema responde con los datos de usuario y roles, y entrega los tokens según el tipo de cliente (cookies en web, body en mobile).
5. El cliente web no guarda ningún token y redirige a la pantalla principal de su rol.

**Reglas de negocio**

- Mensaje de error idéntico para correo inexistente y contraseña incorrecta (anti enumeración de usuarios).
- Bloqueo temporal en Redis, independiente del estado del usuario:
  - **Por correo e IP:** 5 fallos → 15 minutos. Un tercero desde otra IP no bloquea al usuario. Un login correcto reinicia este contador.
  - **Por cuenta (todas las IP):** tope de 100 fallos en el mismo período, configurable. Frena el ataque repartido y se registra como alerta. Un login correcto no borra los fallos ajenos.
- El intento se **reserva de forma atómica antes** de comprobar la contraseña: las peticiones simultáneas no pueden saltarse el límite. Como consecuencia, más de 5 logins correctos exactamente a la vez desde el mismo origen pueden recibir 423.
- El login no mantiene una conexión de base de datos mientras compara la contraseña ni pide una segunda conexión mientras tiene otra tomada.
- Límite conocido: con 20 o más IP todavía se puede alcanzar el tope por cuenta y bloquearla. Quienes comparten IP comparten el límite de una misma cuenta.
- Los intentos fallidos y bloqueos se registran como `WARN` sin contraseña ni token.
- Aplica el rate limit por IP existente.
- Mismo tiempo de respuesta para correo inexistente y contraseña incorrecta: si el correo no existe se compara igual contra un hash BCrypt ficticio.
- Los eventos de seguridad (login exitoso en `INFO`, fallido y bloqueo en `WARN`) llevan `errorCode` cuando aplica y nunca la contraseña ni tokens.
- El body de login solo acepta `email` y `password` (sin campos extra: no hay mass assignment).
- Web: toda petición que cambia estado exige el header `X-XSRF-TOKEN`; el cliente obtiene la cookie `XSRF-TOKEN` con `GET /api/v1/auth/csrf` al iniciar. Mobile queda exento por usar `Authorization: Bearer` y el header `X-Client-Type`, que obliga a preflight CORS.
- `GET /api/v1/auth/me` devuelve el usuario y sus roles a partir de la sesión vigente; el frontend lo usa para saber si hay sesión al recargar.
- Transporte del refresh token y detalles del JWT: [SEGURIDAD_AUTH_BACKEND](../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md).

---

## Criterios de aceptación

- **CA01:** Dado que el usuario existe, está activo y envía credenciales correctas, el sistema debe responder 200 con usuario y roles, y entregar access y refresh token (cookies `HttpOnly` en web, body en mobile).
- **CA02:** Dado que la contraseña es incorrecta o el correo no existe, el sistema debe responder 401 con el mismo código y mensaje en ambos casos.
- **CA03:** Dado que un origen acumuló 5 intentos fallidos contra una cuenta, el sistema debe responder 423 a ese origen aunque la contraseña sea correcta, hasta que pasen 15 minutos, sin afectar a otros orígenes.
- **CA04:** Dado que la cuenta está inactiva, el sistema debe responder 403 sin revelar más detalle.
- **CA05:** Dado que el correo o la contraseña vienen vacíos o con formato inválido, el sistema debe responder 400 y el cliente debe mostrar el error bajo el campo.
- **CA06:** Dado un login exitoso, el cliente web no debe guardar ningún token en `localStorage`, `sessionStorage` ni variables de JavaScript (solo cookies `HttpOnly`), y mobile debe guardar los tokens en almacenamiento seguro.
- **CA07:** Dado un login exitoso, el cliente debe redirigir a la pantalla correspondiente a su rol.
- **CA08:** Dado que el usuario tiene sesión vigente y recarga la página web, el sistema debe mantenerlo autenticado consultando `GET /me`; sin sesión vigente debe responder 401 y el cliente debe ir al login.
- **CA09:** Dado un `POST` web sin header `X-XSRF-TOKEN` válido, el sistema debe responder 403 sin ejecutar la acción.
- **CA10:** Dado que el correo no existe y que existe con contraseña incorrecta, el tiempo de respuesta debe ser equivalente (ambos pasan por una comparación BCrypt).
- **CA11:** Dado un login exitoso, el sistema debe crear una fila en `sesion` con estado `ACTIVA`, sin guardar el refresh token en claro, y registrar `AUTH_LOGIN_OK` en la bitácora.
- **CA12:** Dado un login fallido, un bloqueo o una cuenta inactiva, el sistema debe registrarlo en la bitácora (`AUTH_LOGIN_FALLO`, `AUTH_CUENTA_BLOQUEADA`) sin contraseña y con el correo solo como hash corto.
- **CA13:** Dado que llegan muchos intentos simultáneos contra una cuenta desde un origen, el sistema debe evaluar como máximo 5 contraseñas.
- **CA14:** Dado que la cuenta alcanza el tope de fallos sumando todos los orígenes, el sistema debe responder 423 a cualquier origen y registrar el bloqueo con alcance `cuenta`.
- **CA15:** Dado que llegan más logins simultáneos que conexiones tiene el pool, el sistema debe responderlos sin errores 500.

## Seguridad

- No se registran contraseñas ni tokens (OWASP A09). Respuesta uniforme anti-enumeración (A07). Contraseñas comparadas con BCrypt.

## Desarrollador

Pendiente de asignar.
