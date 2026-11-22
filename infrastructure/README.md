# Infrastructure

Configuración de la infraestructura de desarrollo y producción. Los
`docker-compose*.yml` están en la raíz del repo; esta carpeta tiene lo que
monta cada servicio:

| Carpeta | Contenido |
| --- | --- |
| `nginx/` | `nginx.conf` (proxy de desarrollo, HTTP + WebSockets para Vite) y `nginx.prod.conf` (producción: TLS, HSTS, CSP) |
| `postgres/init/` | Scripts de inicialización de Postgres (vacía por ahora) |
| `redis/` | `redis.conf` (memoria, desalojo LRU y persistencia AOF; la contraseña viene del `.env`) |
| `minio/` | `create-buckets.sh`: crea el bucket privado al levantar el stack |
| `prometheus/` | `prometheus.yml`: scrapea las métricas del backend |
| `grafana/` | Provisioning del datasource de Prometheus |
| `docker/` | Reservada para configuración Docker adicional (vacía por ahora) |

Detalle de cada servicio, volúmenes y variables de entorno en
[`docs/INFRAESTRUCTURA.md`](../docs/INFRAESTRUCTURA.md).
