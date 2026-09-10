"""Database initialization: create tables, storage dirs, seed defaults."""
from __future__ import annotations

from backend.config.database import Base, engine
from backend.utils.file import ensure_storage_tree
from backend.utils.logger import get_logger

logger = get_logger(__name__)

# Default MCP tool registry seed. Tool classes themselves live in mcp/tools
# and self-register at import time; this list is informational metadata that is
# persisted so the UI can show available tools even before first import.
DEFAULT_MCP_TOOLS = [
    {
        "name": "weather",
        "description": "Get current weather for a location via wttr.in.",
    },
    {
        "name": "map",
        "description": "Geocode a place name using Nominatim (OpenStreetMap).",
    },
    {
        "name": "music",
        "description": "Search locally generated music tracks by query.",
    },
    {
        "name": "calendar",
        "description": "Manage a local calendar of events (add/list/find).",
    },
    {
        "name": "search",
        "description": "Web search via DuckDuckGo HTML with safe fallback.",
    },
    {
        "name": "system",
        "description": "System info, current time, host statistics.",
    },
]


async def init_db() -> None:
    """Create tables and storage tree. Safe to call on every startup."""
    logger.info("Initializing database and storage")
    ensure_storage_tree()

    # Import models so Base.metadata is fully populated.
    from backend.database import models as _models  # noqa: F401

    async with engine.begin() as conn:
        await conn.run_sync(Base.metadata.create_all)
    logger.info("Database tables ensured")

    await _seed_defaults()
    logger.info("Database initialization complete")


async def _seed_defaults() -> None:
    """Seed any first-run data (e.g. default pet). Implemented lazily."""
    # Default per-user data (pet, etc.) is created on first user interaction,
    # so there is nothing global to seed at startup yet. Kept as a hook.
    return None
