#!/usr/bin/env bash
# Actualiza dependencias de los 3 proyectos y valida. Correr en TU máquina
# (necesita pnpm, flutter y mvn/mvnw). Revisá `git diff` antes de commitear.
set -e
cd "$(dirname "$0")/../.."

# Guarda todo lo que sale en pantalla en update-deps.log (ignorado por git).
exec > >(tee update-deps.log) 2>&1

echo "== Frontend / workspace =="
# Solo dentro del rango compatible; las versiones MAYORES se deciden una por una.
pnpm up -r
pnpm install
pnpm outdated -r || true
pnpm audit || true
pnpm --filter frontend lint && pnpm --filter frontend test && pnpm --filter frontend build

echo "== Mobile =="
(cd projects/mobile && flutter pub upgrade && flutter pub outdated; flutter analyze && flutter test)

echo "== Backend =="
(cd projects/backend && ./mvnw -U versions:display-dependency-updates && ./mvnw verify)
