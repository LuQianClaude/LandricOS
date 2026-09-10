#!/bin/bash
# start_backend.sh -- create a venv, install requirements, run the FastAPI
# backend with live reload for local development.
#
# Run from anywhere; the script resolves the project root from its own path.
set -e

# Resolve project root (directory containing this script's parent).
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
cd "${PROJECT_ROOT}"

VENV_DIR="${PROJECT_ROOT}/.venv"

# Create a virtual environment if it does not exist.
if [ ! -d "${VENV_DIR}" ]; then
    echo "[start_backend] Creating virtualenv at ${VENV_DIR}"
    python3 -m venv "${VENV_DIR}"
fi

# Activate it for this script.
# shellcheck disable=SC1091
source "${VENV_DIR}/bin/activate"

echo "[start_backend] Upgrading pip"
python -m pip install --upgrade pip

echo "[start_backend] Installing requirements"
if [ -f "${PROJECT_ROOT}/requirements.txt" ]; then
    python -m pip install -r "${PROJECT_ROOT}/requirements.txt"
else
    echo "[start_backend] WARNING: requirements.txt not found; skipping install"
fi

# Ensure a .env exists (copy from .env.example) so the backend has defaults.
if [ ! -f "${PROJECT_ROOT}/.env" ] && [ -f "${PROJECT_ROOT}/.env.example" ]; then
    echo "[start_backend] Copying .env.example -> .env"
    cp "${PROJECT_ROOT}/.env.example" "${PROJECT_ROOT}/.env"
fi

# Read host/port from .env with safe fallbacks.
APP_HOST="${APP_HOST:-0.0.0.0}"
APP_PORT="${APP_PORT:-8000}"
if [ -f "${PROJECT_ROOT}/.env" ]; then
    APP_HOST="$(grep -E '^APP_HOST=' "${PROJECT_ROOT}/.env" | cut -d= -f2- | tail -n1 | tr -d '\r' || true)"
    APP_PORT="$(grep -E '^APP_PORT=' "${PROJECT_ROOT}/.env" | cut -d= -f2- | tail -n1 | tr -d '\r' || true)"
    APP_HOST="${APP_HOST:-0.0.0.0}"
    APP_PORT="${APP_PORT:-8000}"
fi

echo "[start_backend] Starting uvicorn at http://${APP_HOST}:${APP_PORT}"
exec uvicorn backend.main:app --reload --host "${APP_HOST}" --port "${APP_PORT}"
