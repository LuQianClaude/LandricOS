"""Recall orchestrator: combine short + long + emotional memory for a query."""
from __future__ import annotations

from typing import Any, Dict, List, Optional

from sqlalchemy.ext.asyncio import AsyncSession

from backend.memory.emotional_memory import emotional_memory
from backend.memory.long_memory import long_memory
from backend.memory.short_memory import short_memory


class Recall:
    """Orchestrates memory recall to build prompt context."""

    async def recall(
        self,
        session: AsyncSession,
        user_id: int,
        conversation_id: Optional[int],
        query: str,
        top_k: int = 5,
    ) -> Dict[str, Any]:
        """Return combined memory context: short, long, emotional."""
        recent: List[Dict[str, str]] = []
        if conversation_id is not None:
            recent = await short_memory.as_openai_messages(
                conversation_id, n=10, session=session
            )

        long_results = await long_memory.search(session, user_id, query, top_k=top_k)
        emotional = await emotional_memory.get_current(session, user_id)

        return {
            "short_memory": recent,
            "long_memory": [
                {
                    "content": r.get("content", ""),
                    "score": r.get("score", 0.0),
                    "kind": r.get("kind", "long"),
                    "metadata": r.get("metadata", {}),
                }
                for r in long_results
            ],
            "emotional_memory": emotional,
        }


recall = Recall()
