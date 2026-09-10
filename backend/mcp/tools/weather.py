"""Weather tool. Returns current conditions via wttr.in or a mock fallback."""
from __future__ import annotations

from typing import Any, Dict

import httpx

from backend.mcp.manager import McpTool, register_tool
from backend.utils.logger import get_logger

logger = get_logger(__name__)


class WeatherTool(McpTool):
    name = "weather"
    description = "Get current weather for a location via wttr.in."
    parameters = [
        {"name": "query", "type": "string", "description": "City or place name.", "required": True},
        {"name": "format", "type": "string", "description": "j1 (json, default) or plain.", "required": False, "default": "j1"},
    ]

    async def run(self, args: Dict[str, Any]) -> Dict[str, Any]:
        query = str(args.get("query") or "").strip()
        if not query:
            return {"error": "query is required"}
        fmt = str(args.get("format") or "j1").strip() or "j1"
        url = f"https://wttr.in/{query}"
        try:
            async with httpx.AsyncClient(timeout=15.0) as client:
                resp = await client.get(url, params={"format": fmt}, headers={"Accept": "application/json"})
            if resp.status_code != 200:
                return {"error": f"wttr.in returned {resp.status_code}", "query": query}
            if fmt == "j1":
                data = resp.json()
                current = (data.get("current_condition") or [{}])[0]
                return {
                    "query": query,
                    "description": current.get("weatherDesc", [{}])[0].get("value", ""),
                    "temp_c": current.get("temp_C"),
                    "feels_like_c": current.get("FeelsLikeC"),
                    "humidity": current.get("humidity"),
                    "wind_kmph": current.get("windspeedKmph"),
                    "area": (data.get("nearest_area") or [{}])[0].get("areaName", [{}])[0].get("value", query),
                    "raw": data,
                }
            return {"query": query, "text": resp.text}
        except Exception as exc:
            logger.warning("weather tool failed for %s: %s", query, exc)
            return {"query": query, "error": str(exc), "mock": {"description": "sunny", "temp_c": "22"}}


register_tool(WeatherTool())
