# LandricsVilla backend + frontend container image.
#
# AI provider keys are NOT baked into this image. Users add their own provider
# key + base URL via the app Settings page after login.

FROM python:3.11-slim

# Build-time deps for any Python packages that compile C extensions.
RUN apt-get update \
    && apt-get install -y --no-install-recommends build-essential \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Install Python dependencies first for better layer caching.
COPY requirements.txt ./
RUN pip install --no-cache-dir -r requirements.txt

# Copy backend and frontend. The frontend is served by the backend so the
# container is self-contained.
COPY backend ./backend
COPY frontend ./frontend

# Persist user data and the database outside the container's writable layer.
RUN mkdir -p /app/storage /app/database /app/models

ENV APP_HOST=0.0.0.0 \
    APP_PORT=8000 \
    DB_URL=sqlite:////app/database/landricsvilla.db \
    STORAGE_DIR=/app/storage \
    LOG_LEVEL=INFO \
    CORS_ORIGINS=*

EXPOSE 8000

CMD ["uvicorn", "backend.main:app", "--host", "0.0.0.0", "--port", "8000"]
