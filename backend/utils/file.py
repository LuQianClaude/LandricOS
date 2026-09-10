"""Filesystem helpers for storage, uploads and generated assets."""
from __future__ import annotations

import os
import shutil
import uuid
from pathlib import Path
from typing import Optional

from backend.config.settings import settings


def ensure_dir(path: str) -> Path:
    """Create a directory (and parents) if missing. Returns absolute Path."""
    p = Path(path).resolve()
    p.mkdir(parents=True, exist_ok=True)
    return p


def _safe_name(filename: str) -> str:
    """Strip path separators and dangerous characters from a filename."""
    base = os.path.basename(filename or "")
    safe = "".join(c for c in base if c.isalnum() or c in (".", "-", "_"))
    return safe or "file"


async def save_upload(data: bytes, filename: str, subdir: Optional[str] = None) -> Path:
    """Persist an uploaded blob under UPLOADS_DIR and return its path."""
    base = ensure_dir(settings.UPLOADS_DIR)
    if subdir:
        base = ensure_dir(str(base / subdir))
    name = _safe_name(filename)
    # Prefix with a short uuid to avoid collisions.
    unique = f"{uuid.uuid4().hex[:8]}_{name}"
    target = base / unique
    target.write_bytes(data)
    return target


def save_generated(filename: str, data: bytes, subdir: Optional[str] = None) -> Path:
    """Persist a generated asset (e.g. music) under GENERATED_MUSIC_DIR."""
    base = ensure_dir(settings.GENERATED_MUSIC_DIR)
    if subdir:
        base = ensure_dir(str(base / subdir))
    name = _safe_name(filename)
    unique = f"{uuid.uuid4().hex[:8]}_{name}"
    target = base / unique
    target.write_bytes(data)
    return target


def get_path(relative: str) -> Path:
    """Resolve a path under STORAGE_DIR to an absolute Path safely."""
    safe = _safe_name(relative)
    storage = ensure_dir(settings.STORAGE_DIR)
    return storage / safe


def read_bytes(path: Path) -> bytes:
    """Read a file's bytes. Raises FileNotFoundError if missing."""
    return Path(path).read_bytes()


def remove_file(path: Path) -> bool:
    """Delete a file if it exists. Returns True if something was removed."""
    try:
        p = Path(path)
        if p.exists():
            p.unlink()
            return True
    except OSError:
        return False
    return False


def ensure_storage_tree() -> None:
    """Create the standard storage directory layout."""
    ensure_dir(settings.STORAGE_DIR)
    ensure_dir(settings.UPLOADS_DIR)
    ensure_dir(settings.GENERATED_MUSIC_DIR)
    ensure_dir(settings.LOG_DIR)
    # SQLite database directory.
    ensure_dir("./database")
