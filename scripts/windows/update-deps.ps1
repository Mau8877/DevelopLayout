# Actualiza dependencias de los 3 proyectos y valida. Revisá `git diff` antes de commitear.
$ErrorActionPreference = 'Stop'
Set-Location (Join-Path $PSScriptRoot '..\..')

Write-Host '== Frontend / workspace =='
pnpm up --latest -r
pnpm install
pnpm audit --audit-level high
pnpm --filter frontend lint; pnpm --filter frontend test; pnpm --filter frontend build

Write-Host '== Mobile =='
Push-Location projects/mobile
flutter pub upgrade --major-versions; flutter analyze; flutter test
Pop-Location

Write-Host '== Backend =='
Push-Location projects/backend
.\mvnw.cmd -U versions:display-dependency-updates
.\mvnw.cmd verify
Pop-Location
