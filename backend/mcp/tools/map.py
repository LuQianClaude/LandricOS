"""Map tool. Geocodes a place name via Nominatim (OpenStreetMap)."""
from __future__ import annotations

from typing import Any, Dict, List

import httpx

from backend.mcp.manager import McpTool, register_tool
from backend.utils.logger import get_logger

logger = get_logger(__name__)


class MapTool(McpTool):
    name = "map"
    description = "Geocode a place name using Nominatim (OpenStreetMap)."
    parameters = [
        {"name": "query", "type": "string", "description": "Place name or address to search.", "required": True},
        {"name": "limit", "type": "integer", "description": "Max results (default 5).", "required": False, "default": 5},
    ]

    async def run(self, args: Dict[str, Any]) -> Dict[str, Any]:
        query = str(args.get("query") or "").strip()
        if not query:
            return {"error": "query is required"}
        limit = int(args.get("limit") or 5)
        url = "https://nominatim.openstreetmap.org/search"
        params = {
            "q": query,
            "format": "json",
            "addressdetails": 1,
            "limit": limit,
        }
        headers = {"Accept": "application/json", "User-Agent": "LandricsVilla/1.0 (local-first AI companion)"}
        try:
            async with httpx.AsyncClient(timeout=15.0) as client:
                resp = await client.get(url, params=params, headers=headers)
            if resp.status_code != 200:
                return {"query": query, "error": f"nominatim returned {resp.status_code}"}
            results: List[Dict[str, Any]] = resp.json() or []
            return {
                "query": query,
                "results": [
                    {
                        "display_name": r.get("display_name"),
                        "lat": r.get("lat"),
                        "lon": r.get("lon"),
                        "type": r.get("type"),
                        "address": r.get("address"),
                    }
                    for r in results
                ],
            }
        except Exception as exc:
            logger.warning("map tool failed for %s: %s", query, exc)
            return {"query": query, "error": str(exc)}


register_tool(MapTool())
