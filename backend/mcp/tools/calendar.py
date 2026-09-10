"""Calendar tool. Local event store kept in a JSON file under storage."""
from __future__ import annotations

import json
import os
from datetime import datetime, timezone
from typing import Any, Dict, List

from backend.config.settings import settings
from backend.mcp.manager import McpTool, register_tool
from backend.utils.file import ensure_dir
from backend.utils.logger import get_logger

logger = get_logger(__name__)

_STORE_FILE = os.path.join(os.path.dirname(settings.STORAGE_DIR), "storage", "calendar.json")


def _path() -> str:
    storage = ensure_dir(settings.STORAGE_DIR)
    return str(storage / "calendar.json")


def _load() -> List[Dict[str, Any]]:
    try:
        with open(_path(), "r", encoding="utf-8") as f:
            data = json.load(f)
            return data if isinstance(data, list) else []
    except (OSError, json.JSONDecodeError):
        return []


def _save(events: List[Dict[str, Any]]) -> None:
    with open(_path(), "w", encoding="utf-8") as f:
        json.dump(events, f, ensure_ascii=False, indent=2)


class CalendarTool(McpTool):
    name = "calendar"
    description = "Manage a local calendar of events (add/list/find)."
    parameters = [
        {"name": "action", "type": "string", "description": "add | list | find.", "required": True},
        {"name": "event", "type": "object", "description": "Event dict {title, time, note}.", "required": False},
        {"name": "query", "type": "string", "description": "Search text for 'find'.", "required": False},
    ]

    async def run(self, args: Dict[str, Any]) -> Dict[str, Any]:
        action = str(args.get("action") or "list").lower()
        events = _load()

        if action == "add":
            event = args.get("event") or {}
            if not isinstance(event, dict) or not event.get("title"):
                return {"error": "event.title is required for add"}
            event.setdefault("time", datetime.now(timezone.utc).isoformat())
            event.setdefault("id", len(events) + 1)
            events.append(event)
            _save(events)
            return {"action": "add", "event": event, "total": len(events)}

        if action == "find":
            q = str(args.get("query") or "").lower()
            found = [e for e in events if q in json.dumps(e, ensure_ascii=False).lower()]
            return {"action": "find", "query": q, "results": found}

        # default: list
        return {"action": "list", "events": events, "total": len(events)}


register_tool(CalendarTool())
