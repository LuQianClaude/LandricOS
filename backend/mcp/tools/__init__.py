"""MCP tools package. Importing this module auto-registers all tools."""
from backend.utils.logger import get_logger

logger = get_logger(__name__)

# Importing each module triggers @register_tool side effects.
from backend.mcp.tools import (  # noqa: F401
    calendar,
    map,
    music,
    search,
    system,
    weather,
)

logger.info("MCP tools loaded")
