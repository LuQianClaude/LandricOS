"""Application settings loaded from environment / .env file.

Users add their own AI provider keys via the app Settings page, NOT here.
This file never contains real provider API keys.
"""
from __future__ import annotations

from typing import List

from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    """Central configuration. Override via environment variables or .env."""

    APP_NAME: str = "LandricsVilla"
    HOST: str = "0.0.0.0"
    PORT: int = 8000

    # SQLite default. async SQLAlchemy needs the aiosqlite driver; database.py
    # rewrites a bare "sqlite:///" prefix to "sqlite+aiosqlite:///" automatically.
    DB_URL: str = "sqlite:///./database/landricsvilla.db"

    STORAGE_DIR: str = "./storage"
    GENERATED_MUSIC_DIR: str = "./storage/music"
    UPLOADS_DIR: str = "./storage/uploads"
    LOG_DIR: str = "./storage/logs"

    JWT_SECRET: str = "change-me"
    JWT_ALG: str = "HS256"
    ACCESS_TOKEN_EXPIRE_MINUTES: int = 1440

    CORS_ORIGINS: List[str] = ["*"]

    # Optional embedding backend: "sentence_transformers" or "hashing".
    EMBEDDING_BACKEND: str = "hashing"
    EMBEDDING_DIM: int = 256

    model_config = SettingsConfigDict(
        env_file=".env",
        env_file_encoding="utf-8",
        extra="ignore",
    )


settings = Settings()
