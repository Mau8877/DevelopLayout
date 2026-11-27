# Unidades de trabajo — HU-INICIAL-Login-BE

**Historia:** Iniciar sesión y consultar la sesión actual, con defensas de OWASP.
**Fuente HU:** [`HU-INICIAL-Login.md`](../../../HistoriasDeUsuario/HU-INICIAL-Login.md)
**Recurso HTTP / contrato:** `GET /api/v1/auth/csrf`, `POST /api/v1/auth/login`, `GET /api/v1/auth/me`.

## Antes de implementar cualquier UT

Leer las rules de [`backend/`](../../../../.claude/rules/backend/) y [`SEGURIDAD_OWASP.md`](../../../../.claude/rules/SEGURIDAD_OWASP.md). Estas fichas **no** sustituyen esas reglas: si hay conflicto, ganan las rules del proyecto.

## Decisiones de diseño

- Los tokens van en cookies `HttpOnly` en web y en el body para mobile (`X-Client-Type: mobile`). Los dos canales no se mezclan: con el header mobile se ignoran las cookies.
- CSRF double-submit solo para web; mobile queda exento porque el header propio obliga a preflight CORS y no usa cookies.
- `RlsContext` se crea aquí por ser el primer consumidor; las HU siguientes lo reutilizan.

## Orden de ejecución

| Código | Nombre | Depende de |
|--------|--------|------------|
| [UT-HUI-LOGIN-BE-01](./UT-HUI-LOGIN-BE-01.md) | Configuración de seguridad stateless, CSRF y manejo de 401/403 | — |
| [UT-HUI-LOGIN-BE-02](./UT-HUI-LOGIN-BE-02.md) | JwtService: emitir y validar access token | — |
| [UT-HUI-LOGIN-BE-03](./UT-HUI-LOGIN-BE-03.md) | UserDetailsService y contexto RLS por transacción | Models y repositorios de usuarios (HU-INICIAL-Base) |
| [UT-HUI-LOGIN-BE-04](./UT-HUI-LOGIN-BE-04.md) | Bloqueo temporal por intentos fallidos (Redis) | — |
| [UT-HUI-LOGIN-BE-05](./UT-HUI-LOGIN-BE-05.md) | SesionService: crear la sesión y emitir el refresh token | Models y repositorios de sesiones (HU-INICIAL-Base) |
| [UT-HUI-LOGIN-BE-06](./UT-HUI-LOGIN-BE-06.md) | JwtAuthenticationFilter: autenticar por cookie o Bearer | UT-HUI-LOGIN-BE-01, UT-HUI-LOGIN-BE-02 |
| [UT-HUI-LOGIN-BE-07](./UT-HUI-LOGIN-BE-07.md) | AuthService.login y me | UT-HUI-LOGIN-BE-02, UT-HUI-LOGIN-BE-03, UT-HUI-LOGIN-BE-04, UT-HUI-LOGIN-BE-05, UT-HUI-LOGIN-BE-05, HU-INICIAL-Base: BitacoraService (UT-HUI-BASE-BE-06) |
| [UT-HUI-LOGIN-BE-08](./UT-HUI-LOGIN-BE-08.md) | AuthController: csrf, login y me, cookies y permisos | UT-HUI-LOGIN-BE-06, UT-HUI-LOGIN-BE-07 |
| [UT-HUI-LOGIN-BE-09](./UT-HUI-LOGIN-BE-09.md) | Tests de autenticación (login, lockout, autorización) | UT-HUI-LOGIN-BE-08 |
| [UT-HUI-LOGIN-BE-10](./UT-HUI-LOGIN-BE-10.md) | Límite de intentos atómico, sin agotar conexiones | UT-HUI-LOGIN-BE-09 |

**Cadena:** 01 → 02 → 03 → 04 → 05 → 06 → 07 → 08 → 09

## Criterios de aceptación cubiertos

| CA (HU-INICIAL-Login) | Descripción | UTs |
|---|---|---|
| CA01 | Login correcto | 07, 08, 09 |
| CA02 | Error genérico | 07, 09 |
| CA03 | Bloqueo 423 | 04, 07, 09 |
| CA04 | Cuenta inactiva 403 | 07, 09 |
| CA05 | Validación 400 | 08, 09 |
| CA06 | Sin tokens en storage | 08 (cookies) y FE/MOB |
| CA07 | Redirección por rol | FE/MOB |
| CA08 | `/me` al recargar | 03, 08, 09 |
| CA09 | CSRF 403 | 01, 09 |
| CA10 | Tiempo equivalente | 07, 09 |
| CA11 | Sesión creada en `sesion` y evento OK | 05, 07, 09 |
| CA12 | Eventos de fallo y bloqueo | 07, 09 |

## Follow-ups (no se implementan en estas UT)

- Cierre de sesiones por rol o por dispositivo desde el panel de administración.

## Qué NO incluyen estas UT

- Refresh, logout y recuperación de contraseña.
- Registro público de usuarios.

## Notas de implementación (desviaciones respecto de las fichas)

- `JwtAuthenticationFilter` **no** es `@Component`: Spring Boot registraría una copia en la cadena de servlets, fuera de Spring Security, y la request quedaría marcada como ya filtrada. Se instancia en `SecurityConfig`.
- UT-BE-06: el filtro y los resolvers se prueban a nivel unitario (`JwtAuthenticationFilterTest`) y de punta a punta en `AuthControllerTest`, no con un `MockMvc` aparte.
- UT-BE-07: no hay `AuthServiceTest` separado; sus casos (éxito, 401 igual para ambos fallos, 423, 403, bitácora) viven en `AuthControllerTest`. El test de igualdad de tiempos no se escribió (inestable en CI); la igualdad se logra por diseño (BCrypt contra hash ficticio).
- Bitácora: la clave del detalle es `correoRef` (no `...Hash`) porque `BitacoraService` rechaza claves que contengan `hash`.
- Se agregó `HttpMessageNotReadableException` → 400 `ERR_SYS_01` en `GlobalExceptionHandler` y `spring.jackson.deserialization.fail-on-unknown-properties=true` (sin eso un campo extra se ignoraba o daba 500).
- Estas UT **no se compilaron ni se ejecutaron** en el entorno donde se escribieron (sin Java 21/Maven/Docker).
