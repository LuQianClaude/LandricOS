"""Short-term memory: rolling cache of the last N messages per conversation.

Keeps an in-process cache for fast recall and mirrors into the Message table
so it survives restarts. get_recent reconstructs from DB if the cache is cold.
"""
from __future__ import annotations

import asyncio
from collections import deque
from typing import Deque, Dict, List, Optional, Tuple

from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from backend.database.models import Message
from backend.utils.logger import get_logger

logger = get_logger(__name__)

# (role, content, conversation_id)
_Entry = Tuple[str, str, int]


class ShortMemory:
    """Per-conversation rolling message window."""

    _windows: Dict[int, Deque[_Entry]] = {}
    _max: int = 20
    _lock = asyncio.Lock()

    def __init__(self, max_messages: int = 20) -> None:
        self._max = max_messages

    async def add(self, conversation_id: int, role: str, content: str) -> None:
        async with self._lock:
            window = ShortMemory._windows.setdefault(
                conversation_id, deque(maxlen=ShortMemory._max)
            )
            window.append((role, content, conversation_id))

    async def get_recent(
        self,
        conversation_id: int,
        n: int = 10,
        session: Optional[AsyncSession] = None,
    ) -> List[_Entry]:
        async with self._lock:
            window = ShortMemory._windows.get(conversation_id)
            if window:
                return list(window)[-n:]
        if session is None:
            return []
        # Cache miss: reconstruct from DB.
        stmt = (
            select(Message)
            .where(Message.conversation_id == conversation_id)
            .order_by(Message.created_at.desc())
            .limit(n)
        )
        result = await session.execute(stmt)
        rows = result.scalars().all()
        entries: List[_Entry] = [
            (r.role, r.content, r.conversation_id) for r in reversed(rows)
        ]
        async with self._lock:
            window = deque(entries, maxlen=ShortMemory._max)
            ShortMemory._windows[conversation_id] = window
        return entries

    async def clear(self, conversation_id: int) -> None:
        async with self._lock:
            ShortMemory._windows.pop(conversation_id, None)

    async def as_openai_messages(self, conversation_id: int, n: int = 10, session: Optional[AsyncSession] = None) -> List[Dict[str, str]]:
        entries = await self.get_recent(conversation_id, n, session)
        return [{"role": role, "content": content} for role, content, _ in entries]


# Shared singleton.
short_memory = ShortMemory()
