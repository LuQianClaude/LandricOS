"""MCPManager: registry and dispatcher for async tools.

Tools register themselves via the @register_tool decorator (or register()).
Each tool exposes: name, description, parameters (JSON schema), async run(args).
"""
from __future__ import annotations

import importlib
from typing import Any, Dict, List, Optional

from backend.utils.logger import get_logger

logger = get_logger(__name__)


class McpTool:
    """Base tool interface. Subclass and override run()."""

    name: str = ""
    description: str = ""
    parameters: List[Dict[str, Any]] = []

    async def run(self, args: Dict[str, Any]) -> Any:  # pragma: no cover
        raise NotImplementedError


_REGISTRY: Dict[str, McpTool] = {}


def register_tool(tool: McpTool) -> McpTool:
    """Register a tool instance by its name."""
    if not tool.name:
        raise ValueError("Tool must define a non-empty name")
    _REGISTRY[tool.name] = tool
    logger.debug("Registered MCP tool: %s", tool.name)
    return tool


def get_tool(name: str) -> Optional[McpTool]:
    return _REGISTRY.get(name)


class MCPManager:
    """Lists and dispatches registered MCP tools."""

    def __init__(self) -> None:
        self._loaded = False

    def _ensure_loaded(self) -> None:
        # Import the tools package so every tool module self-registers.
        if self._loaded:
            return
        try:
            importlib.import_module("backend.mcp.tools")
        except Exception as exc:  # pragma: no cover
            logger.warning("MCP tools import failed: %s", exc)
        self._loaded = True

    def list_tools(self) -> List[Dict[str, Any]]:
        self._ensure_loaded()
        return [
            {
                "name": t.name,
                "description": t.description,
                "parameters": list(t.parameters),
            }
            for t in _REGISTRY.values()
        ]

    def get_descriptions_for_prompt(self) -> str:
        """Compact textual listing of tools for the AI system prompt."""
        self._ensure_loaded()
        if not _REGISTRY:
            return "No tools available."
        lines: List[str] = []
        for t in _REGISTRY.values():
            params = ", ".join(p.get("name", "?") for p in t.parameters)
            lines.append(f"- {t.name}({params}): {t.description}")
        return "\n".join(lines)

    async def call_tool(self, name: str, args: Dict[str, Any]) -> Dict[str, Any]:
        self._ensure_loaded()
        tool = _REGISTRY.get(name)
        if tool is None:
            return {"ok": False, "error": f"Unknown tool: {name}", "result": None}
        try:
            result = await tool.run(args)
            return {"ok": True, "result": result, "error": None}
        except Exception as exc:
            logger.warning("Tool %s failed: %s", name, exc)
            return {"ok": False, "result": None, "error": str(exc)}


mcp_manager = MCPManager()
