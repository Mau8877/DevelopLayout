# UT-HUI-CAMBIARPASSWORD-FE-03 — Prueba de navegador del cambio

| Campo | Valor |
|-------|-------|
| **Código** | `UT-HUI-CAMBIARPASSWORD-FE-03` |
| **Nombre** | Prueba de navegador del cambio |
| **Historia** | HU-INICIAL-CambiarPassword-FE |
| **Depende de** | 02 |

## Antes de implementar

TESTING_FRONTEND (pruebas de navegador) y CI_CD.

## Solicitud de negocio

El flujo cruza cookies, CSRF, revocación y correo: solo un navegador lo comprueba entero.

## Objetivo

El cambio de contraseña queda cubierto de punta a punta.

## Alcance

- `password.spec.ts`: cada prueba crea su cuenta. Con dos navegadores: cambiar desde el menú, terminar en el login con el aviso, comprobar que las dos sesiones pierden el acceso, que las cookies desaparecen, que la contraseña anterior no entra y la nueva sí, y que llega el aviso por correo.
- Un caso con la contraseña actual incorrecta: sigue en la pantalla, con la sesión abierta.
- Subir en `scripts/e2e.mjs` el límite por IP de esta ruta, como los demás.

**Archivos esperados (crear/tocar):**

- `projects/frontend/e2e/password.spec.ts`
- `projects/frontend/e2e/soporte/pagina.ts`
- `scripts/e2e.mjs`

## Seguridad

A07: comprueba en un navegador real que ninguna sesión sobrevive al cambio.

## Fuera de alcance (no tocar)

- Repetir cada validación del formulario.
- Relajar las protecciones de `scripts/e2e.mjs`.

## Resultado esperado

Una regresión del flujo hace fallar el CI.

## Validación

1. `pnpm test:e2e` en verde tres veces seguidas, sin esperas fijas.
2. Acceso denegado: la otra sesión termina en el login.
3. `node --test scripts/e2e.test.mjs` en verde.
