# Provisioning & deploy

## Target platform

**Single Linux VM + Docker Compose.** Cheapest and simplest for an MVP; the
whole stack (app, Postgres, RabbitMQ, Caddy/HTTPS, backups) runs from one
`docker compose` file with no external managed services.

Recommended box: **Ubuntu 24.04 LTS, 1 vCPU / 2 GB RAM / 20 GB disk**
(Hetzner CX22, DigitalOcean 2 GB, Lightsail 2 GB, …).

## 1. Provision the VM

```bash

apt-get update && apt-get -y upgrade
curl -fsSL https://get.docker.com | sh              # Docker Engine + compose plugin
adduser --disabled-password --gecos "" deploy
usermod -aG docker deploy


apt-get install -y ufw
ufw allow OpenSSH && ufw allow 80 && ufw allow 443
ufw --force enable
```

DNS: point an `A` record for your domain at the VM's public IP.

## 2. Deploy

```bash
su - deploy
git clone https://github.com/caioeduardopereirafelix/Billing-Core-API.git
cd Billing-Core-API

cp .env.prod.example .env.prod

docker compose --env-file .env.prod -f docker-compose.prod.yaml up -d --build
```

## 3. Verify

```bash
bash deploy/qa-check.sh          # black-box deploy-readiness checks
```

## What runs

| Service     | Exposed        | Notes                                             |
|-------------|----------------|---------------------------------------------------|
| `caddy`     | 80, 443        | HTTPS termination + serves the frontend + proxies the API |
| `app`       | internal only  | Spring Boot on :8080, non-root                    |
| `postgres`  | internal only  | volume `postgres_data`                            |
| `rabbitmq`  | internal only  | management UI **not** exposed                     |
| `db-backup` | internal only  | `pg_dump` -> volume `db_backups`, rotated         |

No `pgadmin`, no published database/broker ports.

## Backups

Dumps land in the `db_backups` volume (`billing-prod_db_backups`). Interval and
retention come from `BACKUP_INTERVAL_SECONDS` / `BACKUP_KEEP` in `.env.prod`.

```bash
docker compose --env-file .env.prod -f docker-compose.prod.yaml exec db-backup ls -lh /backups
docker compose --env-file .env.prod -f docker-compose.prod.yaml cp \
  db-backup:/backups/ ./db-dumps/
gunzip -c billing-YYYYMMDDTHHMMSSZ.sql.gz | \
  docker compose --env-file .env.prod -f docker-compose.prod.yaml exec -T postgres \
  psql -U billing -d billing
```

For off-box durability, sync `./db-dumps/` to object storage (cron + `rclone`/`aws s3`).

## Updating

```bash
git pull
docker compose --env-file .env.prod -f docker-compose.prod.yaml up -d --build
```

`server.shutdown: graceful` drains in-flight requests; Flyway runs `validate` on
boot and applies any new migrations.
