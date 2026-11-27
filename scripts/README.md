# Scripts

Scripts de automatización del proyecto. Se corren en **tu máquina** (necesitan
`pnpm`, `flutter` y Java; el backend usa el `mvnw` del repo).

## `setup:env`: preparar los `.env` en un paso

Crea (o completa) los `.env` de los cuatro proyectos a partir de sus
`.env.example`, con secretos aleatorios. Solo necesita Node; no instala nada.

```bash
pnpm setup:env          # crea/completa los .env
pnpm setup:env:check    # no escribe nada; falla si falta algo o queda un CAMBIAME
```

- **Nunca pisa un valor que ya existe.** Solo reemplaza lo que sigue diciendo
  `CAMBIAME` y agrega las claves que el `.env.example` tiene y tu `.env` no.
  Se puede correr las veces que haga falta.
- **No imprime los secretos.** El reporte dice cuántos generó, no cuáles. Están
  en cada `.env` (ignorado por git, con permisos 600 en Linux/macOS).
- **Mantiene los archivos sincronizados:** el `.env` del backend toma de la raíz
  la misma contraseña de Postgres, del rol `app_runtime`, de Redis y de MinIO, y
  las mismas claves `JWT_SECRET` y `REFRESH_TOKEN_SECRET`.
- `setup:env:check` falla si falta una clave, queda un `CAMBIAME` o un valor compartido (contraseñas, claves, usuarios) **difiere** entre el `.env` de la raíz y el del backend; muestra solo los nombres. Si la URL local de la base usa otro puerto u otro nombre, solo avisa. Ninguno de los dos comandos pisa un valor existente: se igualan a mano. Ojo: cambiar el `.env` no cambia la contraseña de un Postgres ya creado.
- **Qué te toca a vos:** cambiar `INITIAL_ADMIN_EMAIL` (el script avisa si sigue
  con el valor de ejemplo), y los nombres de base y usuario si el proyecto usa
  otros. Los secretos que generó los podés leer del `.env` (ej. la contraseña
  del administrador inicial).
- Los tests del script: `pnpm test:scripts`.

## `db:backup` / `db:verify` / `db:restore`: respaldo y restauración de Postgres

```bash
pnpm db:backup              # backups/<base>-<fecha>.dump (formato custom de pg_dump)
pnpm db:backup --keep 7     # y conserva solo los 7 más recientes
pnpm db:verify              # restaura el último respaldo en una base TEMPORAL, comprueba
                            # migraciones/tablas/usuarios/permisos y la borra (tu base real no se toca)
pnpm db:restore backups/<archivo>.dump --confirmar
                            # REEMPLAZA la base real (backend detenido)
```

- Requiere el stack levantado (`docker compose up`). Con `--prod` usa `docker-compose.prod.yml`.
- Un respaldo que nunca se restauró no es un respaldo: corré `db:verify` después de cada `db:backup`.
- Contiene datos reales (hashes de contraseñas, bitácora): `backups/` está en `.gitignore`. Guardalo cifrado y fuera de la máquina.
- **`db:restore`** se niega sin `--confirmar`, sin un archivo explícito o con el backend en marcha. Crea el rol `app_runtime` si falta (usa `APP_RUNTIME_PASSWORD` del `.env`), restaura en **una sola transacción** (si falla, la base no cambia) y **revoca todas las sesiones**, porque el respaldo revive las que se cerraron después. Los access token ya emitidos siguen valiendo hasta 15 minutos. Sirve sobre un servidor vacío: ver [INFRAESTRUCTURA.md](../docs/INFRAESTRUCTURA.md#7d-respaldos-de-la-base). No uses `pg_restore` a mano.
- Tests: `pnpm test:scripts`.

## `openapi:sync` / `openapi:types`: contrato de la API para los clientes

```bash
pnpm openapi:sync     # con el backend de dev levantado: guarda docs/api/openapi.json
pnpm openapi:types    # genera projects/frontend/src/shared/api/schema.d.ts desde ese snapshot
pnpm openapi:sync --check   # falla si el backend en vivo ya no coincide con el snapshot
```

- El snapshot (`docs/api/openapi.json`) **se versiona**: es el contrato, y su diff en un PR
  muestra exactamente qué cambió para frontend y mobile. Se normaliza (claves ordenadas, sin `servers`).
- El test `OpenApiContractTest` (backend) falla si el contrato publicado difiere del snapshot:
  si cambiás un endpoint o un DTO, corré `openapi:sync` y commiteá el snapshot con el cambio.
- `schema.d.ts` se **genera, no se edita a mano** (sí se commitea, para que el build y CI no dependan del backend) (reemplaza escribir a mano los
  tipos de request/response del frontend). Mobile (Dart) no genera código: valida contra el
  snapshot en sus tests de parsing cuando haga falta.
- Tests: `pnpm test:scripts`.

## `update-deps`: actualizar y validar las dependencias

Actualiza las dependencias de los tres proyectos y comprueba que todo sigue
funcionando. Corre, en orden:

1. **Frontend / workspace**: `pnpm up -r`, `pnpm install`, `pnpm outdated -r`
   (mayores pendientes), `pnpm audit`, y después lint, tests y build del
   frontend.
2. **Mobile**: `flutter pub upgrade`, `flutter pub outdated`, `flutter analyze`
   y `flutter test`.
3. **Backend**: `mvnw -U versions:display-dependency-updates` y `mvnw verify`
   (compila, corre los tests y los de integración con Testcontainers; los de
   Testcontainers necesitan Docker).

Solo sube dentro del rango compatible: las versiones **mayores** se deciden
una por una (`pnpm outdated -r` te muestra las pendientes). No commitea nada,
revisá `git diff` antes.

```powershell
# Windows (desde la raíz del repo)
powershell -ExecutionPolicy Bypass -File .\scripts\windows\update-deps.ps1
```

```bash
# Linux / macOS (desde la raíz del repo)
bash scripts/linux/update-deps.sh
```

### El log

Todo lo que sale en pantalla también se guarda en `update-deps.log` en la raíz
(ignorado por git), para poder compartirlo sin copiar la terminal. En la
versión de Windows cada paso queda marcado con `>>> título` y
`<<< exit code: N`: un `0` es que pasó, cualquier otro número es que falló.

> El log de Windows mezcla dos codificaciones (PowerShell 5.1 escribe UTF-16 con
> `Tee-Object` y UTF-8 con `Set-Content`). Para leerlo desde Python, quitá los
> bytes `\x00` antes de decodificar como UTF-8.

La actualización automática semanal de dependencias (Maven, npm, pub, Docker y
GitHub Actions) la hace además `.github/dependabot.yml`.

## `test:prod`: producción de punta a punta

```bash
pnpm test:prod
```

Levanta `docker-compose.prod.yml` con secretos, certificado y volúmenes descartables, comprueba que funciona, borra los datos, restaura un respaldo en un servidor vacío y limpia. Necesita Docker, `openssl` (en Windows viene con Git) y los puertos `18080` y `18443` libres. Se niega a correr si hay contenedores con los nombres del proyecto (`docker compose down` antes; **no** uses `-v`, que borra tus datos). Deja las imágenes construidas con el tag `e2e-prod`. Tarda varios minutos. Es la misma prueba que corre el job `docker` de CI.

