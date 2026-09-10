"""Emotional memory: tracks user and AI emotional state over time.

State is persisted as kind="emotional" Memory rows and cached in-process for
fast access to the current state.
"""
from __future__ import annotations

import json
from datetime import datetime, timezone
from typing import Any, Dict, List, Optional

from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from backend.database.models import Memory
from backend.utils.logger import get_logger

logger = get_logger(__name__)


class EmotionalMemory:
    """Per-user emotional state tracker (user + AI sides)."""

    # In-process cache: {user_id: {"user": {...}, "ai": {...}}}
    _cache: Dict[int, Dict[str, Dict[str, Any]]] = {}

    @classmethod
    def _empty_state(cls) -> Dict[str, Any]:
        return {
            "mood": "neutral",
            "valence": 0.0,
            "arousal": 0.0,
            "note": "",
            "updated_at": datetime.now(timezone.utc).isoformat(),
        }

    async def update(
        self,
        session: AsyncSession,
        user_id: int,
        state: Dict[str, Any],
        subject: str = "user",
    ) -> Dict[str, Any]:
        """Record a new emotional state for a subject ('user' or 'ai')."""
        subject = subject if subject in ("user", "ai") else "user"
        merged = self._empty_state()
        merged.update(state)
        merged["updated_at"] = datetime.now(timezone.utc).isoformat()

        cache = EmotionalMemory._cache.setdefault(user_id, {"user": self._empty_state(), "ai": self._empty_state()})
        cache[subject] = merged

        # Persist as an emotional memory row.
        record = Memory(
            user_id=user_id,
            kind="emotional",
            content=merged.get("mood", "neutral"),
            metadata_json=json.dumps({"subject": subject, "state": merged}, ensure_ascii=False),
            importance=0.8,
        )
        session.add(record)
        await session.flush()
        logger.debug("Emotional state updated user=%s subject=%s mood=%s", user_id, subject, merged.get("mood"))
        return merged

    async def get_current(self, session: AsyncSession, user_id: int) -> Dict[str, Dict[str, Any]]:
        """Return current user + AI emotional state, loading from DB on miss."""
        cache = EmotionalMemory._cache.get(user_id)
        if cache:
            return cache

        out = {"user": self._empty_state(), "ai": self._empty_state()}
        stmt = (
            select(Memory)
            .where(Memory.user_id == user_id, Memory.kind == "emotional")
            .order_by(Memory.created_at.desc())
            .limit(50)
        )
        result = await session.execute(stmt)
        rows = list(result.scalars().all())
        # newest first -> keep last seen per subject
        for row in reversed(rows):
            try:
                meta = json.loads(row.metadata_json or "{}")
            except json.JSONDecodeError:
                continue
            subject = meta.get("subject", "user")
            state = meta.get("state", {})
            if subject in out and not out[subject].get("mood"):
                out[subject] = state
        EmotionalMemory._cache[user_id] = out
        return out

    async def get_history(self, session: AsyncSession, user_id: int, limit: int = 50) -> List[Dict[str, Any]]:
        stmt = (
            select(Memory)
            .where(Memory.user_id == user_id, Memory.kind == "emotional")
            .order_by(Memory.created_at.desc())
            .limit(limit)
        )
        result = await session.execute(stmt)
        out: List[Dict[str, Any]] = []
        for row in result.scalars().all():
            try:
                meta = json.loads(row.metadata_json or "{}")
            except json.JSONDecodeError:
                meta = {}
            out.append(
                {
                    "id": row.id,
                    "subject": meta.get("subject", "user"),
                    "state": meta.get("state", {}),
                    "created_at": row.created_at.isoformat() if row.created_at else None,
                }
            )
        return out


emotional_memory = EmotionalMemory()
