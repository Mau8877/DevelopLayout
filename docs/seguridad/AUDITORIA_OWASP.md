# Auditoría de seguridad (OWASP)

| Dato | Valor |
| --- | --- |
| Fecha | 2026-11-28 |
| Alcance | La plantilla: backend, web, mobile, scripts, CI e infraestructura |
| Referencias | OWASP Top 10:2025, API Security Top 10:2023 y MASVS 2.1.0 (ver [SEGURIDAD_OWASP.md](../../.claude/rules/SEGURIDAD_OWASP.md)) |
| Método | Revisión de código y configuración, pruebas automáticas y reproducción de los fallos |

> ! **Esto no es una certificación ni una prueba de penetración.** No se usaron
> escáneres dinámicos, no se probó en un servidor ni en un dispositivo reales y
> nadie externo revisó el sistema. La lista de controles es la que fija el
> proyecto, no la totalidad de ASVS o de las pruebas de MASTG. Un proyecto que
> nazca de la plantilla debe revisar lo suyo.

## Cómo leer los estados

| Estado | Significa |
| --- | --- |
| **Cumple** | Revisado en el alcance de esta auditoría, con evidencia |
| **Parcial** | Hay controles, pero falta algo que se indica |
| **No verificado** | No se pudo comprobar aquí |
| **No aplica** | La plantilla no tiene ese flujo |

## Hallazgos corregidos

La gravedad es una valoración propia, no un cálculo CVSS.

| Id | Gravedad | Qué pasaba | Corrección y prueba |
| --- | --- | --- | --- |
| H-01 | Alta | Contraseñas y tokens podían quedar en los logs: con `devtools` (desarrollo) la validación escribía el valor rechazado sin activar nada; con `DEBUG`, los bodies recibidos | `toString()` oculto y logger de excepciones resueltas apagado. `SecretosEnLogsTest`, `RecordsConDatosSensiblesTest` |
| H-02 | Media | Un error de la base escribía datos personales (un correo) en el log, por el manejador de errores y por Hibernate | `ExcepcionSinMensajes`, logger de Hibernate apagado y `logServerErrorDetail=false`. `DatosEnErroresInesperadosTest` |
| H-03 | Baja | El alta del administrador inicial registraba su correo | Registra su código. `AdminInicialInitializerTest` |
| H-04 | Media | Se aceptaban contraseñas muy usadas | Lista local de filtraciones. `ContrasenasComunesTest`, `PasswordComunTest` |
| H-05 | Media | Sin límite de consulta a la base; Redis usaba la espera de la librería | Tiempos máximos. `EsperasLimitadasTest` |
| H-06 | **Alta** | **Los límites por ruta se evitaban codificando la URL** (`/auth/lo%67in`), y el límite general codificando el prefijo | Las rutas se comparan decodificadas, como en Spring MVC. `EvasionHttpTest` (servidor real) |
| H-07 | Baja | Un `Authorization: Bearer` inventado desactivaba el CSRF de una petición que se autenticaba con cookie | Solo el canal mobile queda exento. `EvasionHttpTest` |
| H-08 | Baja | Con `DEBUG` se veían correos, IP, dispositivo y texto de búsqueda | `toString()` oculto también con datos personales. `SecretosEnLogsTest` |
| H-09 | Baja | La consola de la web registraba el mensaje completo de un error no controlado | Registra solo el tipo |
| H-10 | Baja | El proxy aceptaba cuerpos de 10 MB, también sin sesión | 1 MB. `compose-seguridad.test.mjs` y `pnpm test:prod` |
| H-11 | Media | Los 403 y 429 no dejaban rastro, contra la regla A09 del proyecto | Se registran sin datos personales. `EventosDeSeguridadEnLogsTest` |

Sobre H-06: el límite de intentos por correo e IP y el límite de nginx en el
borde (20 por segundo, que compara la ruta ya normalizada) seguían activos. Lo
que se perdía eran los límites por ruta de la aplicación, por ejemplo los que
frenan probar una contraseña contra muchas cuentas o pedir muchas
recuperaciones. Esa protección de nginx no se probó con rutas codificadas.

## OWASP Top 10:2025

| Id | Categoría | Estado | Evidencia y pendientes |
| --- | --- | --- | --- |
| A01 | Control de acceso (incluye SSRF) | Cumple | Todo privado por defecto (`SecurityConfig`). Cada operación de administrador lleva `@PreAuthorize`; lo propio se filtra por el usuario del token y por RLS. Pruebas 401/403/404 y `Rls*Test`. El backend no hace peticiones salientes por HTTP |
| A02 | Configuración insegura | Cumple | Swagger y detalle de errores apagados fuera de `dev`; credenciales obligatorias en producción; puertos de desarrollo solo en `127.0.0.1`; en producción solo publica el proxy (`pnpm test:prod`) |
| A03 | Cadena de suministro | **Parcial** | Lockfiles, `--frozen-lockfile`, acciones fijadas por SHA, Dependabot, Trivy en CI y `pnpm audit` limpio. **Falta:** verificación SHA-256 de la descarga de Maven (P-03); la auditoría de dependencias Java y Flutter no se ejecutó en local (P-04) |
| A04 | Fallos criptográficos | Cumple | BCrypt; claves de firma de 32 bytes o más, distintas entre sí y rechazadas si son las de ejemplo; tokens aleatorios de 32 bytes y solo su hash guardado; cookies `HttpOnly`, `SameSite=Strict`, `Secure`; TLS 1.2/1.3 y HSTS; correo con STARTTLS obligatorio (la librería verifica el certificado por defecto) |
| A05 | Inyección | Cumple | Consultas parametrizadas o Criteria (los comodines de búsqueda se escapan); `@Valid`; sin `dangerouslySetInnerHTML`, `eval` ni ejecución dinámica; todavía no hay reportes |
| A06 | Diseño inseguro | Cumple | Cada historia tiene su sección de Seguridad. Desde ahora, cada historia nueva cierra con una [UT de seguridad](../../.claude/rules/UNIDADES_DE_TRABAJO.md#toda-hu-termina-con-una-ut-de-seguridad) |
| A07 | Autenticación | **Parcial** | Límites de intentos, sin enumeración, rotación y detección de reutilización, cierre inmediato de sesiones, contraseñas comunes rechazadas. **Falta:** doble factor (P-01) |
| A08 | Integridad de software y datos | Cumple | Sin deserialización no confiable; CI antes de fusionar; sin scripts de terceros en la web. La integridad de Maven está en A03 |
| A09 | Registro y alertas | **Parcial** | Bitácora de solo inserción; eventos de seguridad registrados sin secretos ni datos personales. **Falta:** reglas de alerta y a quién avisar (P-02) |
| A10 | Condiciones excepcionales | Cumple | Un solo manejador, respuestas sin detalles internos y fallo cerrado (Redis o base caídos no entregan tokens) |

## API Security Top 10:2023

| Id | Riesgo | Estado | Evidencia |
| --- | --- | --- | --- |
| API1 | Autorización a nivel de objeto | Cumple | Sesión ajena: 404. RLS forzado y probado con `app_runtime` |
| API2 | Autenticación rota | Cumple | Ver A07; el doble factor es P-01 |
| API3 | Autorización a nivel de propiedad | Cumple | DTO de entrada y salida; todo DTO de entrada rechaza propiedades desconocidas |
| API4 | Consumo sin límites | Cumple | `pageSize` máximo 100, límites por IP y por cuenta, cuerpo de 1 MB en el proxy, tiempos máximos. El límite de la aplicación es por instancia |
| API5 | Autorización a nivel de función | Cumple | `@PreAuthorize` en el service y pruebas de rol insuficiente |
| API6 | Flujos de negocio sensibles | Cumple | Límites propios en login, recuperación, invitación, cambio y reenvío |
| API7 | SSRF | No aplica | El backend no pide URL que dé el usuario |
| API8 | Configuración insegura | Cumple | CORS con lista explícita; ver A02 |
| API9 | Inventario | Cumple | Solo `/api/v1`; contrato versionado y comparado en CI |
| API10 | Consumo inseguro de APIs | No aplica | No se consumen APIs de terceros |

## MASVS 2.1.0 (mobile)

> ! **Revisión del código solamente.** Nada de esta sección se probó en un
> emulador ni en un dispositivo.

| Grupo | Estado | Evidencia y pendientes |
| --- | --- | --- |
| STORAGE | Parcial | Tokens solo en el almacenamiento seguro del sistema; copias de seguridad excluidas en Android. **Falta:** protección de capturas en pantallas sensibles (P-06) y comprobar iOS |
| CRYPTO | Cumple | Sin criptografía propia ni claves en el código |
| AUTH | Cumple | La sesión la decide el servidor |
| NETWORK | Cumple | Sin tráfico en claro en release (en debug, solo hacia la máquina local) y HTTPS obligatorio al arrancar. El *certificate pinning* no se evaluó |
| PLATFORM | Parcial | Sin enlaces profundos; solo la actividad principal está exportada. Falta P-06 |
| CODE | **Parcial** | `flutter analyze` en CI. **Falta:** firma propia del release (P-05), procedimiento de compilación ofuscada (P-07) y auditoría local de dependencias (P-04) |
| RESILIENCE | No aplica | La regla del proyecto lo exige solo si el análisis de riesgo lo pide |
| PRIVACY | Cumple | Sin identificadores publicitarios ni analítica; los logs solo salen en debug y sin datos personales |

## Pendientes

Ninguno se corrigió porque necesita una decisión, una clave, un dispositivo o
descargar algo.

| Id | Pendiente | Cuándo hace falta |
| --- | --- | --- |
| P-01 | Doble factor, al menos para `SUPER_ADMIN` | Antes de producción con datos sensibles. Es una función nueva |
| P-02 | Reglas de alerta (401, 403, 429, 5xx y tope de intentos por cuenta) y su destino | Antes de producción |
| P-03 | `distributionSha256Sum` en `maven-wrapper.properties` | Pronto: hay que descargar la distribución de Maven y comprobar su huella contra la publicada |
| P-04 | Confirmar en el CI el resultado de Trivy y evaluar una auditoría de dependencias Flutter | En cada PR |
| P-05 | Firma de release de Android: hoy usa la clave de **debug** | Obligatorio antes de publicar la app |
| P-06 | Bloquear capturas en las pantallas con contraseña o sesiones | Con la validación mobile |
| P-07 | Documentar y probar el release con `--obfuscate --split-debug-info` | Con la validación mobile |
| P-08 | Las imágenes propias de producción usan `latest` si no se define `APP_VERSION` | Al definir el despliegue |

## Otras limitaciones conocidas

- La lista de contraseñas es una copia fija: no consulta filtraciones nuevas.
- El correo de un intento fallido se registra como un hash corto sin sal: sirve
  para relacionar eventos, pero quien tenga una lista de correos podría
  reconocerlo.
- El `DEBUG` de la capa web puede mostrar datos que no se reconocen por su
  nombre. No se usa en producción.
- `gitleaks` no se ejecuta en local.

## Cómo repetir las comprobaciones

```bash
cd projects/backend && ./mvnw test     # incluye las pruebas de seguridad citadas
pnpm test:scripts                      # compose, CI y rutas
pnpm test:e2e                          # navegador
pnpm test:prod                         # producción desechable
pnpm audit --audit-level moderate
```

`gitleaks` y Trivy corren en el job `security` del CI. Cada vez que cambie el
sistema de forma relevante se actualizan las tablas y la fecha.
