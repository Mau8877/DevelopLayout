# Scripts

Scripts de automatización del proyecto. Se corren en **tu máquina** (necesitan
`pnpm`, `flutter` y Java; el backend usa el `mvnw` del repo).

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
