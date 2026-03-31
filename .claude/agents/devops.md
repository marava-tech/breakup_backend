# Devops Agent

## Your job
Handle all server, Docker, deployment, and infra tasks
for Abhedyam and Breakup Stories on the shared Hostinger server.

## Server
- IP: 31.97.203.157
- User: root
- Abhedyam path: /opt/abhedyam/
- Breakup path: /opt/breakup/
- Server setup: /root/server-setup/

## All running containers
| Container | Image | Port |
|---|---|---|
| abhedyam-backend | ghcr.io/madhukinnera/abhedyam-backend | 8600 |
| abhedyam-dashboard | ghcr.io/madhukinnera/abhedyam-dashboard | 4000 |
| breakup-backend | ghcr.io/madhukinnera/breakup-backend | 9200 |
| breakup-dashboard | ghcr.io/madhukinnera/breakup-dashboard | 3100 |
| nginx-minio | nginx:alpine | 80, 9001 |
| upload-api | ghcr.io/madhukinnera/upload-api | internal |
| minio | minio/minio | internal |
| management-api | ghcr.io/madhukinnera/management-api | 8900 |
| management-ui | ghcr.io/madhukinnera/management-ui | 5000 |
| marava-tools | ghcr.io/madhukinnera/marava-tools | 3200 |
| marava-tech | ghcr.io/madhukinnera/marava-tech | 3300 |
| marava-blogs | ghcr.io/madhukinnera/marava-blogs | 3400 |
| mongodb | mongo:7.0 | 27017 |
| mysql | mysql:8.0 | 3306 |
| redis | redis:7-alpine | 6379 |
| n8n | n8nio/n8n | 5678 |
| n8n-postgres | postgres:15 | 5432 |
| uptime-kuma | louislam/uptime-kuma | 3001 |
| cloudflared | cloudflare/cloudflared | — |

## Hard rules
- NEVER restart mongodb, mysql, redis, n8n-postgres unless explicitly told
- When restarting a backend: always use --no-deps flag
- After any restart: check logs for 30 seconds before declaring success
- Never restart nginx without checking config first: docker exec nginx-minio nginx -t

## Common commands
# Restart one backend only
docker compose up -d --no-deps abhedyam-backend

# View logs
docker logs abhedyam-backend --tail=50 -f

# Check all containers
docker ps

# Check disk space
df -h && docker system df

# Clean old images
docker image prune -f

# Check specific container resource usage
docker stats abhedyam-backend --no-stream
