"""Search tool. DuckDuckGo HTML scrape with a safe fallback."""
from __future__ import annotations

import re
from typing import Any, Dict, List

import httpx

from backend.mcp.manager import McpTool, register_tool
from backend.utils.logger import get_logger

logger = get_logger(__name__)

_TITLE_RE = re.compile(r'<a[^>]*class="result__a"[^>]*>(.*?)</a>', re.DOTALL)
_SNIPPET_RE = re.compile(r'<a[^>]*class="result__snippet"[^>]*>(.*?)</a>', re.DOTALL)
_TAG_RE = re.compile(r"<[^>]+>")


def _strip(html: str) -> str:
    return _TAG_RE.sub("", html).strip()


class SearchTool(McpTool):
    name = "search"
    description = "Web search via DuckDuckGo HTML with safe fallback."
    parameters = [
        {"name": "query", "type": "string", "description": "Search query.", "required": True},
        {"name": "limit", "type": "integer", "description": "Max results (default 5).", "required": False, "default": 5},
    ]

    async def run(self, args: Dict[str, Any]) -> Dict[str, Any]:
        query = str(args.get("query") or "").strip()
        if not query:
            return {"error": "query is required"}
        limit = max(1, int(args.get("limit") or 5))
        url = "https://html.duckduckgo.com/html/"
        headers = {
            "User-Agent": "LandricsVilla/1.0 (compatible search tool)",
            "Accept": "text/html",
        }
        try:
            async with httpx.AsyncClient(timeout=15.0, follow_redirects=True) as client:
                resp = await client.post(url, data={"q": query}, headers=headers)
            if resp.status_code != 200:
                return {"query": query, "error": f"duckduckgo returned {resp.status_code}", "results": []}
            titles = [t for t in (m.group(1) for m in _TITLE_RE.finditer(resp.text))]
            snippets = [s for s in (m.group(1) for m in _SNIPPET_RE.finditer(resp.text))]
            results: List[Dict[str, Any]] = []
            for i in range(min(len(titles), limit)):
                results.append(
                    {
                        "title": _strip(titles[i]),
                        "snippet": _strip(snippets[i]) if i < len(snippets) else "",
                    }
                )
            if not results:
                return {"query": query, "results": [], "note": "No results parsed from DuckDuckGo."}
            return {"query": query, "results": results}
        except Exception as exc:
            logger.warning("search tool failed for %s: %s", query, exc)
            return {"query": query, "error": str(exc), "results": []}


register_tool(SearchTool())
