# Historia de usuario

| Id | Nombre corto de HU | Prioridad | PHU | Estado |
| --- | --- | --- | --- | --- |
| HU-INICIAL-LOGIN | Iniciar sesión | Alta |  | Pendiente |

**Como:** usuario registrado (web o mobile)
**Quiero:** iniciar sesión con mi correo y contraseña
**Para:** acceder a las funciones del sistema según mis roles

**Descripción:**
El usuario envía correo y contraseña. Si son correctos, el sistema responde con los datos del usuario con sus roles, y emite un access token (JWT, 15 minutos) y un refresh token opaco (7 días). En web ambos tokens viajan solo en cookies `HttpOnly` (nada en `localStorage` ni en memoria de JavaScript); en mobile (header `X-Client-Type: mobile`) viajan en el body y la app los guarda en almacenamiento seguro. Si las credenciales son incorrectas, el sistema responde con un mensaje genérico que no revela si el correo existe. Tras 5 intentos fallidos consecutivos la cuenta se bloquea 15 minutos. Una cuenta inactiva no puede iniciar sesión.

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
- Bloqueo temporal: 5 fallos consecutivos → 15 minutos, contador en Redis; un login correcto reinicia el contador. Es independiente del estado del usuario.
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
- **CA03:** Dado que la cuenta acumuló 5 intentos fallidos, el sistema debe responder 423 aunque la contraseña sea correcta, hasta que pasen 15 minutos.
- **CA04:** Dado que la cuenta está inactiva, el sistema debe responder 403 sin revelar más detalle.
- **CA05:** Dado que el correo o la contraseña vienen vacíos o con formato inválido, el sistema debe responder 400 y el cliente debe mostrar el error bajo el campo.
- **CA06:** Dado un login exitoso, el cliente web no debe guardar ningún token en `localStorage`, `sessionStorage` ni variables de JavaScript (solo cookies `HttpOnly`), y mobile debe guardar los tokens en almacenamiento seguro.
- **CA07:** Dado un login exitoso, el cliente debe redirigir a la pantalla correspondiente a su rol.
- **CA08:** Dado que el usuario tiene sesión vigente y recarga la página web, el sistema debe mantenerlo autenticado consultando `GET /me`; sin sesión vigente debe responder 401 y el cliente debe ir al login.
- **CA09:** Dado un `POST` web sin header `X-XSRF-TOKEN` válido, el sistema debe responder 403 sin ejecutar la acción.
- **CA10:** Dado que el correo no existe y que existe con contraseña incorrecta, el tiempo de respuesta debe ser equivalente (ambos pasan por una comparación BCrypt).

## Seguridad

- No se registran contraseñas ni tokens (OWASP A09). Respuesta uniforme anti-enumeración (A07). Contraseñas comparadas con BCrypt.

## Desarrollador

Pendiente de asignar.
