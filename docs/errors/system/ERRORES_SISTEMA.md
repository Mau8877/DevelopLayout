# Errores — Sistema (`SYS`)

Códigos transversales, no ligados a ningún feature de negocio puntual (ver
[EXCEPCIONES_BACKEND.md](../../../.claude/rules/backend/EXCEPCIONES_BACKEND.md#catálogo-de-códigos-de-error)
para el criterio de cuándo un código va acá en vez de en el módulo de un
feature).

## `ERR_SYS_00`

- **HTTP:** 500
- **Excepción:** (catch-all, no es una `BusinessException`)
- **Significado:** Error interno no controlado — cualquier excepción no
  mapeada explícitamente, o una falla de infraestructura (base de datos
  caída, etc.).
- **Dónde se lanza:** `GlobalExceptionHandler.handleUnexpectedException()`

## `ERR_SYS_01`

- **HTTP:** 400
- **Excepción:** `ValidationException` (genérica, no de un módulo puntual)
- **Significado:** Falló una validación automática de `@Valid`/Bean
  Validation sobre un DTO de request (campo obligatorio faltante, formato
  inválido, etc.), o un parámetro ausente o mal tipado — no una regla de negocio, esa se cataloga con su propio
  código en el módulo que corresponda.
- **Dónde se lanza:** `GlobalExceptionHandler.handleValidationException()`,
  `handleNotReadable()`, `handleTypeMismatch()` y `handleUnexpectedException()`
  (errores 400 de Spring MVC)

## `ERR_SYS_02`

- **HTTP:** 429
- **Excepción:** ninguna — lo escribe `RateLimitFilter` directo, antes de
  que exista un `@RestControllerAdvice` que pueda intervenir (ver
  [SEGURIDAD_AUTH_BACKEND.md](../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md#rate-limit)).
- **Significado:** Se superó el límite de requests por IP configurado en
  `app.ratelimit.capacity` / `app.ratelimit.refill-per-minute`.
- **Dónde se lanza:** `RateLimitFilter.writeTooManyRequests()`

## `ERR_SYS_03`

- **HTTP:** 404
- **Excepción:** `NoResourceFoundException` (Spring MVC)
- **Significado:** La ruta pedida no existe.
- **Dónde se lanza:** `GlobalExceptionHandler.handleUnexpectedException()`

## `ERR_SYS_04`

- **HTTP:** 405
- **Excepción:** `HttpRequestMethodNotSupportedException` (Spring MVC)
- **Significado:** La ruta existe pero no admite ese verbo HTTP; la respuesta incluye la cabecera `Allow`.
- **Dónde se lanza:** `GlobalExceptionHandler.handleUnexpectedException()`

## `ERR_SYS_05`

- **HTTP:** otro 4xx (ej. 406, 413, 415)
- **Excepción:** cualquier `ErrorResponse` 4xx de Spring MVC sin código propio
- **Significado:** La solicitud no es compatible con el recurso, por ejemplo un `Content-Type` no soportado.
- **Dónde se lanza:** `GlobalExceptionHandler.handleUnexpectedException()`
