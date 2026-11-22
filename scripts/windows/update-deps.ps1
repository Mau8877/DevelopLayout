# Actualiza dependencias de los 3 proyectos y valida. Revisa `git diff` antes de commitear.
# Toda la salida (incluida la de pnpm, flutter y mvnw) se guarda ademas en
# update-deps.log en la raiz del repo (ignorado por git), para poder compartirlo.
# Funciona en Windows PowerShell 5.1: Start-Transcript NO captura programas externos.
$ErrorActionPreference = 'Continue'
$root = Join-Path $PSScriptRoot '..\..'
Set-Location $root
$log = Join-Path (Get-Location) 'update-deps.log'
Set-Content -Path $log -Value ("Inicio: " + (Get-Date)) -Encoding UTF8

function Run([string]$titulo, [string]$comando, [string]$carpeta = '.') {
    "`n>>> $titulo  ($comando)  en $carpeta" | Tee-Object -FilePath $log -Append
    Push-Location $carpeta
    # cmd /c junta stderr en stdout para que PowerShell lo capture como texto normal
    cmd /c "$comando 2>&1" | Tee-Object -FilePath $log -Append
    "<<< exit code: $LASTEXITCODE" | Tee-Object -FilePath $log -Append
    Pop-Location
}

"== Frontend / workspace ==" | Tee-Object -FilePath $log -Append
# Sube solo dentro del rango compatible (semver). Las versiones MAYORES traen cambios
# incompatibles (ej. react-table 8->9, TypeScript 6->7) y se deciden una por una.
Run 'pnpm up'      'pnpm up -r'
Run 'pnpm install' 'pnpm install'
Run 'pnpm outdated (mayores pendientes)' 'pnpm outdated -r'
Run 'pnpm audit'   'pnpm audit --audit-level high'
Run 'lint'         'pnpm --filter frontend lint'
Run 'test'         'pnpm --filter frontend test'
Run 'build'        'pnpm --filter frontend build'

"== Mobile ==" | Tee-Object -FilePath $log -Append
Run 'pub upgrade'  'flutter pub upgrade' 'projects/mobile'
Run 'pub outdated (mayores pendientes)' 'flutter pub outdated' 'projects/mobile'
Run 'analyze'      'flutter analyze' 'projects/mobile'
Run 'test'         'flutter test' 'projects/mobile'

"== Backend ==" | Tee-Object -FilePath $log -Append
Run 'updates'      'mvnw.cmd -U versions:display-dependency-updates' 'projects/backend'
Run 'verify'       'mvnw.cmd verify' 'projects/backend'

"`nFin: " + (Get-Date) | Tee-Object -FilePath $log -Append
