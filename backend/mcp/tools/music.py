"""Music tool. Searches locally generated music tracks by query."""
from __future__ import annotations

from typing import Any, Dict, List

from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from backend.config.database import async_session_maker
from backend.database.models import MusicTrack
from backend.mcp.manager import McpTool, register_tool
from backend.utils.logger import get_logger

logger = get_logger(__name__)


class MusicTool(McpTool):
    name = "music"
    description = "Search locally generated music tracks by query."
    parameters = [
        {"name": "query", "type": "string", "description": "Search prompt/title text.", "required": True},
        {"name": "user_id", "type": "integer", "description": "Owner of tracks (default 0 = any).", "required": False, "default": 0},
        {"name": "limit", "type": "integer", "description": "Max results (default 10).", "required": False, "default": 10},
    ]

    async def _session(self) -> AsyncSession:
        return async_session_maker()

    async def run(self, args: Dict[str, Any]) -> Dict[str, Any]:
        query = str(args.get("query") or "").strip().lower()
        user_id = int(args.get("user_id") or 0)
        limit = int(args.get("limit") or 10)
        stmt = select(MusicTrack)
        if user_id:
            stmt = stmt.where(MusicTrack.user_id == user_id)
        stmt = stmt.order_by(MusicTrack.created_at.desc()).limit(max(limit, 50))
        try:
            async with await self._session() as session:
                result = await session.execute(stmt)
                rows: List[MusicTrack] = list(result.scalars().all())
        except Exception as exc:
            logger.warning("music tool query failed: %s", exc)
            return {"query": query, "error": str(exc), "results": []}

        matches: List[Dict[str, Any]] = []
        for r in rows:
            hay = f"{r.title} {r.prompt}".lower()
            if not query or query in hay:
                matches.append(
                    {
                        "id": r.id,
                        "title": r.title,
                        "prompt": r.prompt,
                        "file_path": r.file_path,
                        "duration": r.duration,
                    }
                )
            if len(matches) >= limit:
                break
        return {"query": query, "results": matches}


register_tool(MusicTool())
