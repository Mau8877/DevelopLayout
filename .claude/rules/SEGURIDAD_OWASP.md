# Seguridad (OWASP) — Regla transversal

Aplica a **todo el repositorio** (backend, frontend, mobile, infra, docs). Es
obligatoria, no una sugerencia: ninguna implementación, UT o PR se da por
terminada si incumple un punto de acá. Si una regla más específica
(`backend/`, `frontend/`, `mobile/`) es más estricta, gana la más estricta.

## Estándares que se cumplen siempre

| Estándar | Versión de referencia | Se aplica a |
| --- | --- | --- |
| OWASP Top 10 | **2025** | Todo el sistema |
| OWASP API Security Top 10 | **2023** | Backend (`/api/v1/**`) |
| OWASP MASVS | **2.1.0** (incluye MASVS-PRIVACY) | Mobile |

> Las versiones se revisan al menos una vez al año: si OWASP publica una
> edición nueva, se actualiza esta tabla y el checklist de abajo **antes**
> de seguir usando la anterior.

## Checklist Top 10:2025 (qué significa en este proyecto)

| # | Categoría | Regla concreta |
| --- | --- | --- |
| A01 | Broken Access Control (incluye SSRF) | Todo endpoint es privado por defecto. Autorización en el `service` (`@PreAuthorize`) **y** a nivel de fila (RLS, ver [BASE_DE_DATOS_PRIVADA_RLS.md](backend/BASE_DE_DATOS_PRIVADA_RLS.md)). Nunca confiar en un `codigo`/id que manda el cliente sin verificar que pertenece al usuario (BOLA). El backend no hace requests a URLs que el usuario controla (SSRF) salvo lista blanca explícita. |
| A02 | Security Misconfiguration | Config base = segura; lo cómodo vive en el perfil `dev`. Sin credenciales por defecto, sin Swagger/`show-sql`/detalle de health fuera de `dev`, puertos de infra solo en `127.0.0.1`, imágenes con tag fijo. |
| A03 | Software Supply Chain Failures | Dependencias con versión fija o administrada por el BOM, lockfiles commiteados (`pnpm-lock.yaml`, `pubspec.lock`), `--frozen-lockfile` en CI, auditoría de dependencias en CI, sin imágenes `:latest`. Ver [CI_CD.md](CI_CD.md). |
| A04 | Cryptographic Failures | Passwords con BCrypt/Argon2, nunca texto plano ni hash rápido. TLS obligatorio en producción. Secretos solo por variables de entorno. Cookies `Secure`+`HttpOnly`. |
| A05 | Injection | Solo queries parametrizadas (Spring Data/`@Query` con parámetros nombrados); prohibido concatenar input en SQL/JPQL/HQL. Validar input (`@Valid`). Sin `dangerouslySetInnerHTML` ni `eval` en frontend. JasperReports: ver [REPORTES_JASPERREPORTS.md](backend/REPORTES_JASPERREPORTS.md). |
| A06 | Insecure Design | Toda HU/UT lleva su sección de **Seguridad** (ver [HISTORIAS_DE_USUARIO.md](HISTORIAS_DE_USUARIO.md) y [UNIDADES_DE_TRABAJO.md](UNIDADES_DE_TRABAJO.md)) con el modelo de amenazas mínimo del flujo. |
| A07 | Authentication Failures | Ver [SEGURIDAD_AUTH_BACKEND.md](backend/SEGURIDAD_AUTH_BACKEND.md): bloqueo por intentos, access token corto, refresh en cookie `HttpOnly`, sin enumeración de usuarios (mismo mensaje y mismo tiempo de respuesta para "usuario inexistente" y "password incorrecta"). |
| A08 | Software or Data Integrity Failures | Sin deserialización de datos no confiables (Java `ObjectInputStream`, Jasper con clases no filtradas). CI valida el build antes de mergear. Sin scripts de terceros sin versión/integridad en el frontend. |
| A09 | Security Logging & Alerting Failures | Se loguean eventos de seguridad (login fallido/exitoso, acceso denegado, rate limit, cambios de rol) en `WARN`/`INFO` con `errorCode`; nunca secretos ni datos personales (ver [LOGGING_BACKEND.md](backend/LOGGING_BACKEND.md)). Las métricas de `/prometheus` deben permitir alertar sobre 401/403/429. |
| A10 | Mishandling of Exceptional Conditions | Todo error cae en el `GlobalExceptionHandler`; **fallar cerrado** (ante un error al decidir un permiso, se deniega). Nunca devolver stacktraces ni mensajes internos (`server.error.include-*=never`). Sin `catch` que se traga la excepción en silencio. |

## Checklist API Security Top 10:2023 (backend)

- **API1 BOLA / API5 BFLA:** cada endpoint verifica propiedad del objeto **y** permiso sobre la función. Test obligatorio: usuario A no puede leer/editar recursos de usuario B (esperado `404` o `403`).
- **API2 Broken Authentication:** ver A07.
- **API3 Broken Object Property Level Authorization:** los DTOs de respuesta exponen solo los campos necesarios (ya garantizado por "nunca devolver un Model"); los DTOs de request **no** aceptan campos que el cliente no debe controlar (rol, `estado`, `codigo`, flags de admin) — no hay *mass assignment*.
- **API4 Unrestricted Resource Consumption:** paginación obligatoria con `pageSize` **máximo** (100), rate limit (nginx + `RateLimitFilter`), límites de tamaño de body y de archivos subidos, timeouts.
- **API6 Unrestricted Access to Sensitive Business Flows:** los flujos sensibles (registro, recuperación de password, envío de correos, descargas masivas) llevan límite propio más estricto que el global.
- **API7 SSRF:** ver A01.
- **API8 Security Misconfiguration:** ver A02. CORS con lista explícita de orígenes (nunca `*` con credenciales) y cabeceras permitidas explícitas.
- **API9 Improper Inventory Management:** solo existe `/api/v1`; cada endpoint está documentado en OpenAPI (visible solo en `dev`). Endpoint viejo que se retira se borra, no se deja "por las dudas".
- **API10 Unsafe Consumption of APIs:** respuestas de APIs externas se validan (schema/tipos) y se consumen con timeout; nunca se confía en ellas como input ya validado.

## Checklist MASVS 2.1.0 (mobile)

- **STORAGE:** tokens y secretos solo en `flutter_secure_storage`; nunca en `shared_preferences`, archivos ni logs. Sin datos sensibles en backups ni capturas de pantalla de pantallas sensibles.
- **CRYPTO:** sin criptografía propia; sin claves hardcodeadas.
- **AUTH:** la sesión se valida contra el backend; el bloqueo biométrico local nunca reemplaza la autenticación del servidor.
- **NETWORK:** solo HTTPS en producción (sin `usesCleartextTraffic`/ATS deshabilitado fuera de debug); certificate pinning evaluado para proyectos con datos sensibles.
- **PLATFORM:** sin deep links que ejecuten acciones sin confirmar; validar todo parámetro entrante; sin componentes Android exportados innecesariamente.
- **CODE:** build de release con ofuscación (`--obfuscate --split-debug-info`); `flutter analyze` limpio; dependencias auditadas.
- **RESILIENCE:** defensas (detección de root/jailbreak, anti-tampering) solo cuando el análisis de riesgo del proyecto las pida; no sustituyen controles del servidor.
- **PRIVACY:** se recolecta el mínimo de datos; sin identificadores publicitarios ni analítica sin consentimiento; sin datos personales en logs.

## Frontend (aplica A01/A02/A05 del lado cliente)

- Web: tokens **solo en cookies `HttpOnly`** con CSRF double-submit (ver [SEGURIDAD_AUTH_BACKEND.md](backend/SEGURIDAD_AUTH_BACKEND.md)); nunca `localStorage`/`sessionStorage` ni memoria de JS para tokens.
- Prohibido `dangerouslySetInnerHTML` con contenido no sanitizado, `eval`, `new Function`, y `target="_blank"` sin `rel="noopener noreferrer"`.
- Las rutas protegidas son una comodidad de UX; **la autorización real es del backend**. Nunca ocultar un botón como único control.
- Producción: la CSP, HSTS y demás cabeceras las pone nginx (ver [INFRAESTRUCTURA](../../docs/INFRAESTRUCTURA.md)).
- Variables `VITE_*` son **públicas** (van al bundle): nunca poner un secreto en ellas.

## Secretos y configuración

- Ningún secreto en el repo: `.env*` ignorados, solo `.env.example` con placeholders (`CAMBIAME`), nunca valores reales ni "de ejemplo que funcionan".
- Sin valor por defecto para credenciales en `application.properties` (la app debe fallar al arrancar si faltan). Los defaults de comodidad viven en `application-dev.properties`.
- Un secreto que se filtró (commit, log, captura) se **rota**, no solo se borra del historial.

## Infraestructura

- Postgres, Redis, MinIO, Prometheus y Grafana **nunca** se publican a la red: en desarrollo, solo `127.0.0.1`; en producción, sin `ports:` (solo red interna de Docker).
- Redis siempre con password; MinIO con buckets **privados** (archivos servidos por el backend o URLs prefirmadas de corta duración); Grafana sin credenciales por defecto.
- Imágenes con tag fijo (nunca `:latest`); contenedores de producción no corren como root.
- `/var/run/docker.sock` solo en el compose de desarrollo (Testcontainers), nunca en producción.
- `/prometheus` y `/health` no se exponen por el proxy público (solo red interna).

## Antes de dar algo por terminado

1. La HU/UT tiene su sección de Seguridad completa.
2. Hay tests de autorización (acceso permitido **y** denegado) para cada endpoint nuevo.
3. No se agregó ningún secreto, `:latest`, puerto público ni `permitAll` nuevo sin justificarlo por escrito en la UT.
4. La auditoría de dependencias de CI pasa.
