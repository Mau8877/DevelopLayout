---
globs: projects/frontend/**/*
---

# Errores y logging — Frontend

Complementa [SEGURIDAD_OWASP.md](../SEGURIDAD_OWASP.md) (A09/A10) y
[RESPONSES_BACKEND.md](../backend/RESPONSES_BACKEND.md). Ninguna pantalla
muestra un error crudo, y ningún `catch` se traga un error en silencio.

## Un solo tipo de error hacia la UI: `ApiError`

Todo fallo de una llamada a la API llega a hooks y componentes como `ApiError`
(`code` + `message`). El único lugar que convierte errores de red es
`src/store/apiClient.ts`:

- Respuesta con sobre `failed` → `ApiError(error, message)` (ya implementado).
- **Fallos sin sobre** (sin conexión, timeout, respuesta no JSON de un proxy,
  `502/504`) → también `ApiError`, con códigos propios del cliente
  `CLIENT_NETWORK`, `CLIENT_TIMEOUT`, `CLIENT_UNKNOWN` y un mensaje genérico en
  el idioma vigente, desde `shared/language` (`normalizeError`).
- Un componente nunca inspecciona `AxiosError`, `error.response` ni
  `error.message` de una excepción que no sea `ApiError`.

## Cómo se muestra cada tipo de error

| Situación | Dónde se muestra |
| --- | --- |
| Validación de un campo (Zod) | Inline, debajo del campo |
| Falla de una mutación (crear/editar/borrar) | Toast (`sonner`) o alerta inline del formulario, con `ApiError.message` |
| Falla de una query (carga de datos) | Componente de estado de error con "Reintentar", no un toast que desaparece |
| Error de render inesperado | Error boundary del router (ver abajo) |

- Se muestra `ApiError.message` (mensaje de negocio, ya en el idioma pedido, del
  backend). Nunca `error.stack`, el body completo, ni un `JSON.stringify` del error.
- Sin `alert()`/`confirm()` del navegador.
- Los avisos (`components/ui/toast.ts`) necesitan `<Toaster />`, montado una sola
  vez en `main.tsx`.
- Mensajes de login y reset: se muestran tal cual los devuelve el backend (no se
  reescriben para revelar si un usuario existe, ver A07).
- `401` se resuelve **solo** en el interceptor de `apiClient` (refresh y
  redirección a `/login`, ver HU de Refresh); ninguna pantalla lo maneja por su
  cuenta. `403` → mensaje de "sin permiso"; `423` y `429` → mensaje de espera.

## Errores no controlados

- El router define `defaultErrorComponent` y `defaultNotFoundComponent`
  (`createRouter` en `main.tsx`): un error de render no deja la pantalla en blanco.
- `window.onerror` y `unhandledrejection` se registran una sola vez en `main.tsx`
  y pasan por el `logger`.
- No hay servicio externo de reporte de errores definido. Si se agrega uno, es
  una decisión explícita y debe redactar datos personales y tokens antes de enviar.

## Logging

- Un único `logger` (`src/utils/logger.ts`). Prohibido `console.log` suelto en
  código de la app; ESLint tiene `no-console` activo y solo el `logger` lo
  desactiva.
- En producción el `logger` no emite `debug`/`info`. `VITE_ENABLE_API_LOGS` debe
  ser `false` en cualquier build de producción.
- **Nunca se loguea:** tokens, cookies, cabecera `Authorization`, passwords,
  `XSRF-TOKEN`, ni el body de los endpoints de `/auth/**`. El logging de
  `createBaseApi` debe redactar esos datos aunque la bandera esté activa.
- Ningún dato sensible va en una URL, en un `queryKey` ni en `localStorage`
  (ver [TANSTACK_QUERY_FRONTEND.md](TANSTACK_QUERY_FRONTEND.md) y las HU de Login).

## Tests

Todo hook de `api/` con lógica de error tiene un test del camino de fallo
(`ApiError` mostrado, reintento). La normalización de `apiClient`
(`CLIENT_NETWORK`, etc.) se prueba con respuestas simuladas.
