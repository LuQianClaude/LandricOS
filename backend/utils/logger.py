"""Logging configuration. stdout + rotating file handler."""
from __future__ import annotations

import logging
import sys
from logging.handlers import RotatingFileHandler
from pathlib import Path

from backend.config.settings import settings
from backend.utils.file import ensure_dir

_LOG_FORMAT = "%(asctime)s | %(levelname)-8s | %(name)s | %(message)s"
_DATE_FORMAT = "%Y-%m-%d %H:%M:%S"

_configured = False


def _configure_root() -> None:
    global _configured
    if _configured:
        return

    log_dir = Path(settings.LOG_DIR).resolve()
    ensure_dir(str(log_dir))
    log_file = log_dir / "landricsvilla.log"

    root = logging.getLogger()
    root.setLevel(logging.INFO)

    formatter = logging.Formatter(_LOG_FORMAT, datefmt=_DATE_FORMAT)

    stream_handler = logging.StreamHandler(sys.stdout)
    stream_handler.setFormatter(formatter)
    root.addHandler(stream_handler)

    try:
        file_handler = RotatingFileHandler(
            str(log_file), maxBytes=5_000_000, backupCount=3, encoding="utf-8"
        )
        file_handler.setFormatter(formatter)
        root.addHandler(file_handler)
    except OSError:
        # Filesystem may be read-only in some sandboxes; stdout still works.
        pass

    _configured = True


def get_logger(name: str) -> logging.Logger:
    """Return a configured logger writing to stdout and a rotating file."""
    _configure_root()
    return logging.getLogger(name)
