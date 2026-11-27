# Este proyecto es un workspace pnpm (ver /pnpm-workspace.yaml) -- el build
# de esta imagen SE HACE CON CONTEXTO EN LA RAÍZ DEL REPO, no en
# projects/frontend, porque `pnpm install` necesita pnpm-workspace.yaml y
# pnpm-lock.yaml de la raíz para instalar con el lockfile real (reproducible)
# en vez de resolver versiones nuevas cada build.
#
#   docker build -f projects/frontend/Dockerfile -t app-frontend .
#
# (la versión vieja de este Dockerfile copiaba solo projects/frontend/package.json
# con contexto en projects/frontend/ -- nunca se probó un build real esta
# sesión porque todo el desarrollo pasa por docker-compose.yml en modo dev,
# y de hecho fallaba: sin el lockfile de la raíz, pnpm no podía resolver el
# workspace correctamente)

# ==========================================
# ETAPA 1: CONSTRUCCIÓN (Builder)
# ==========================================
FROM node:22-alpine AS builder
RUN corepack enable pnpm

WORKDIR /app

# 1. Copiamos primero solo los manifiestos (cache de Docker: si no cambian
# las dependencias, no se reinstala nada aunque cambie el código fuente)
COPY package.json pnpm-workspace.yaml pnpm-lock.yaml ./
COPY projects/frontend/package.json ./projects/frontend/package.json

RUN pnpm install --frozen-lockfile --filter frontend...

# 2. Copiamos el código fuente del frontend y compilamos
COPY projects/frontend ./projects/frontend
# Vite incrusta VITE_* en el bundle al compilar (no se leen en runtime). En
# producción el frontend y la API comparten origen detrás del proxy, así
# que la URL es relativa.
ARG VITE_API_BASE_URL=/api/v1
ARG VITE_ENABLE_API_LOGS=false
ENV VITE_API_BASE_URL=$VITE_API_BASE_URL VITE_ENABLE_API_LOGS=$VITE_ENABLE_API_LOGS
RUN pnpm --filter frontend build

# ==========================================
# ETAPA 2: SERVIR CON NGINX (producción)
# ==========================================
# nginx-unprivileged: corre como usuario sin privilegios y escucha en 8080
# (un puerto >1024 no necesita root). Sirve solo estáticos; el proxy de
# infrastructure/nginx/nginx.prod.conf es el que enruta /api al backend.
# La variante slim trae solo nginx: sin módulos dinámicos, curl ni librerías XML
# que esta imagen no usa y que acumulan CVEs.
FROM nginxinc/nginx-unprivileged:stable-alpine-slim

# La imagen base se publica con menos frecuencia que los parches de Alpine y de
# nginx: sin esto sale con CVEs que ya tienen arreglo (Trivy en CI). La base fija
# `nginx=<versión>`, por eso `apk upgrade` solo no lo sube y hay que pedir el
# mínimo corregido. Cuando la base ya lo traiga, esa línea no cambia nada.
# Solo esto corre como root; el contenedor sigue corriendo como 101.
USER root
RUN apk upgrade --no-cache     && apk add --no-cache --upgrade 'nginx>=1.30.7'

COPY projects/frontend/nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=builder /app/projects/frontend/dist /usr/share/nginx/html

USER 101
EXPOSE 8080
CMD ["nginx", "-g", "daemon off;"]
