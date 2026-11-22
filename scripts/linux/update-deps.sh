#!/bin/sh
# Actualiza dependencias de los 3 proyectos y valida. Correr en TU máquina
# (necesita pnpm, flutter y mvn/mvnw). Revisá `git diff` antes de commitear.
set -e
cd "$(dirname "$0")/../.."

echo "== Frontend / workspace =="
pnpm up --latest -r
pnpm install
pnpm audit --audit-level high || true
pnpm --filter frontend lint && pnpm --filter frontend test && pnpm --filter frontend build

echo "== Mobile =="
(cd projects/mobile && flutter pub upgrade --major-versions && flutter analyze && flutter test)

echo "== Backend =="
(cd projects/backend && ./mvnw -U versions:display-dependency-updates && ./mvnw verify)
