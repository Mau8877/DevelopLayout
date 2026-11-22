#!/bin/sh
set -e

# * `mc config host add` (el comando viejo) ya no existe en versiones
# * actuales de mc -- falla como "not a recognized command" pero el script
# * seguía igual (sin `set -e`) e imprimía falsos "Bucket created
# * successfully" en los pasos siguientes, porque sin un alias real
# * registrado, `mc mb myminio/...` cae a operar sobre una ruta de archivo
# * local del contenedor en vez de MinIO real. El bucket nunca se creaba.
# * `mc alias set` es el comando correcto y vigente.
mc alias set myminio http://minio:9000 "$MINIO_ROOT_USER" "$MINIO_ROOT_PASSWORD"

# Crear el bucket si no existe. Nombre debe coincidir con
# app.minio.bucket-name en projects/backend/src/main/resources/application.properties.
mc mb myminio/app-storage --ignore-existing

# El bucket es PRIVADO (política por defecto de MinIO: sin acceso anónimo).
# Los archivos se sirven siempre a través del backend (autorización por
# recurso) o con URLs prefirmadas de corta duración, nunca con acceso
# público. Ver .claude/rules/SEGURIDAD_OWASP.md (A01) y la regla de
# almacenamiento. `set private` fuerza el estado aunque un bucket viejo
# hubiera quedado público.
mc anonymous set private myminio/app-storage

exit 0
