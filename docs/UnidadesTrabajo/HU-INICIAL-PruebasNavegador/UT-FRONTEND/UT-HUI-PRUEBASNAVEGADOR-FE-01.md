# UT-HUI-PRUEBASNAVEGADOR-FE-01 — Entorno desechable y Playwright

| Campo | Valor |
|-------|-------|
| **Código** | `UT-HUI-PRUEBASNAVEGADOR-FE-01` |
| **Nombre** | Entorno desechable y Playwright |
| **Historia** | HU-INICIAL-PruebasNavegador-FE |
| **Depende de** | — (con HU-INICIAL-Idiomas implementada) |

## Antes de implementar

CI_CD (prueba de producción y sus protecciones), TESTING_FRONTEND y SEGURIDAD_OWASP.

## Solicitud de negocio

Las pruebas necesitan un sistema completo que no toque nada de nadie.

## Objetivo

`pnpm test:e2e` levanta todo, corre una prueba mínima y limpia.

## Alcance

- Dependencia nueva `@playwright/test` y el navegador Chromium (descarga ya autorizada).
- `scripts/e2e.mjs`: Postgres, Redis y Mailpit en contenedores propios con prefijo `e2e-nav-` y puertos propios en `127.0.0.1`; secretos y contraseñas generados en cada ejecución; backend y web en puertos propios; no lee ningún `.env`.
- **Antes de crear o borrar** comprueba que no existan contenedores con esos nombres y que los puertos estén libres; limpia también ante error o interrupción. Sus protecciones tienen pruebas.
- Backend con vida corta del access token (para probar la renovación) y límites de peticiones altos.
- `e2e/`: configuración (solo Chromium, sin reintentos locales, traza al fallar) y utilidades (crear usuarios por la API, leer el correo de Mailpit, iniciar sesión).

**Archivos esperados (crear/tocar):**

- `scripts/e2e.mjs`
- `scripts/e2e.test.mjs`
- `projects/frontend/e2e/playwright.config.ts`
- `projects/frontend/e2e/soporte/`
- `projects/frontend/e2e/arranque.spec.ts`
- `package.json`
- `projects/frontend/package.json`
- `pnpm-lock.yaml`
- `.gitignore`

## Seguridad

A02/A05: nada se publica fuera de `127.0.0.1`, sin secretos fijos y sin tocar recursos ajenos.

## Fuera de alcance (no tocar)

- Los flujos (UT 02 a 04).
- Reutilizar los contenedores de desarrollo.

## Resultado esperado

Hay un entorno reproducible y seguro.

## Validación

1. `pnpm test:e2e` pasa en un equipo limpio y no deja contenedores.
2. Con un contenedor del mismo nombre o un puerto ocupado, se detiene sin tocar nada.
3. Interrumpir a mitad también limpia.
4. Las pruebas de las protecciones fallan si se quitan.
