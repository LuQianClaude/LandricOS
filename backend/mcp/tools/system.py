"""System tool. System info, current time, host statistics."""
from __future__ import annotations

import os
import platform
import socket
import time
from datetime import datetime, timezone
from typing import Any, Dict

from backend.config.settings import settings
from backend.mcp.manager import McpTool, register_tool
from backend.utils.logger import get_logger

logger = get_logger(__name__)


class SystemTool(McpTool):
    name = "system"
    description = "System info, current time, host statistics."
    parameters = [
        {"name": "action", "type": "string", "description": "info | time | stats (default info).", "required": False, "default": "info"},
    ]

    async def run(self, args: Dict[str, Any]) -> Dict[str, Any]:
        action = str(args.get("action") or "info").lower()
        if action == "time":
            now = datetime.now(timezone.utc)
            return {
                "utc": now.isoformat(),
                "epoch": int(now.timestamp()),
                "boot_epoch": int(time.time() - (time.time() - time.time())),
            }
        if action == "stats":
            try:
                import resource

                ru = resource.getrusage(resource.RUSAGE_SELF)
                mem_mb = round(ru.ru_maxrss / 1024.0, 2)
            except Exception:  # pragma: no cover - platform dependent
                mem_mb = None
            return {
                "loadavg": os.getloadavg() if hasattr(os, "getloadavg") else None,
                "max_rss_mb": mem_mb,
                "process_id": os.getpid(),
                "storage_dir": settings.STORAGE_DIR,
            }
        # default info
        return {
            "app": settings.APP_NAME,
            "python": platform.python_version(),
            "platform": platform.platform(),
            "host": socket.gethostname(),
            "storage_dir": settings.STORAGE_DIR,
            "db_url": settings.DB_URL,
            "embedding_backend": settings.EMBEDDING_BACKEND,
            "utc_now": datetime.now(timezone.utc).isoformat(),
        }


register_tool(SystemTool())
