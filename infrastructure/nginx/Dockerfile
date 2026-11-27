# Proxy de producción (ver docker-compose.prod.yml). La configuración no va en la
# imagen: el compose monta nginx.prod.conf y los certificados.
# * Mismo parcheo que projects/frontend/Dockerfile (la base fija la versión de nginx,
# * ver .claude/rules/CI_CD.md). Si subís el mínimo en uno, subilo en el otro.
FROM nginxinc/nginx-unprivileged:stable-alpine-slim

USER root
RUN apk upgrade --no-cache \
    && apk add --no-cache --upgrade 'nginx>=1.30.7'

USER 101
