#!/bin/bash
# deploy.sh -- pull latest code, restart the Docker stack, run migrations,
# and verify the service is healthy. Intended for a single-host deployment.
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
cd "${PROJECT_ROOT}"

HEALTH_URL="${HEALTH_URL:-http://localhost:8000/health}"
MAX_TRIES="${MAX_TRIES:-30}"

echo "[deploy] Pulling latest code"
git pull --ff-only

echo "[deploy] Building and restarting containers"
docker-compose up -d --build --remove-orphans

echo "[deploy] Running pending migrations"
# Run inside the running container so the SQLite file and storage volumes are
# shared with the live process. The backend exposes its migration entrypoint as
# `python -m backend.migrate`.
docker-compose exec -T landricsvilla python -m backend.migrate || {
    echo "[deploy] WARNING: migration step failed or not implemented yet"
}

echo "[deploy] Waiting for health check at ${HEALTH_URL}"
ok=0
for _ in $(seq 1 "${MAX_TRIES}"); do
    if curl -fsS "${HEALTH_URL}" >/dev/null 2>&1; then
        ok=1
        break
    fi
    sleep 2
done

if [ "${ok}" -eq 1 ]; then
    echo "[deploy] Healthy. Service is live at ${HEALTH_URL}"
else
    echo "[deploy] ERROR: service did not become healthy within $((MAX_TRIES * 2))s"
    echo "[deploy] Inspect logs with: docker-compose logs --tail=200 landricsvilla"
    exit 1
fi
