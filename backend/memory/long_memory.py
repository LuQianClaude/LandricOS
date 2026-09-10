"""Long-term memory: persists important memories to SQLite + local vectors.

Uses the local Embedding facade for semantic recall. Keeps deps minimal by
storing embeddings as base64 numpy blobs in the Memory table and doing cosine
similarity in-process (sqlite-vec style local recall).
"""
from __future__ import annotations

import json
from typing import Any, Dict, List, Optional

from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from backend.database.models import Memory
from backend.memory.embedding import Embedding, decode_vector, encode_vector, cosine_similarity
from backend.utils.logger import get_logger

logger = get_logger(__name__)


class LongMemory:
    """CRUD + semantic search over long-term memories."""

    def __init__(self, embedding: Optional[Embedding] = None) -> None:
        self.embedding = embedding or Embedding.get()

    async def add(
        self,
        session: AsyncSession,
        user_id: int,
        text: str,
        metadata: Optional[Dict[str, Any]] = None,
        kind: str = "long",
        importance: float = 0.5,
    ) -> Memory:
        vec = self.embedding.embed(text)
        record = Memory(
            user_id=user_id,
            kind=kind,
            content=text,
            metadata_json=json.dumps(metadata or {}, ensure_ascii=False),
            embedding=encode_vector(vec),
            importance=float(importance),
        )
        session.add(record)
        await session.flush()
        logger.debug("Stored long memory id=%s for user=%s", record.id, user_id)
        return record

    async def list(self, session: AsyncSession, user_id: int, limit: int = 100) -> List[Memory]:
        stmt = (
            select(Memory)
            .where(Memory.user_id == user_id)
            .order_by(Memory.created_at.desc())
            .limit(limit)
        )
        result = await session.execute(stmt)
        return list(result.scalars().all())

    async def get(self, session: AsyncSession, memory_id: int) -> Optional[Memory]:
        return await session.get(Memory, memory_id)

    async def delete(self, session: AsyncSession, memory_id: int) -> bool:
        record = await session.get(Memory, memory_id)
        if record is None:
            return False
        await session.delete(record)
        await session.flush()
        return True

    async def search(
        self,
        session: AsyncSession,
        user_id: int,
        query: str,
        top_k: int = 5,
    ) -> List[Dict[str, Any]]:
        """Semantic recall: embed the query and rank stored memories."""
        q_vec = self.embedding.embed(query)
        stmt = select(Memory).where(Memory.user_id == user_id)
        result = await session.execute(stmt)
        rows = result.scalars().all()
        scored: List[Dict[str, Any]] = []
        for row in rows:
            vec = decode_vector(row.embedding) if row.embedding else None
            if vec is None or vec.shape != q_vec.shape:
                # Re-embed lazily if the stored vector is missing/mismatched.
                vec = self.embedding.embed(row.content)
            score = cosine_similarity(q_vec, vec)
            meta = {}
            try:
                meta = json.loads(row.metadata_json or "{}")
            except json.JSONDecodeError:
                meta = {}
            scored.append(
                {
                    "id": row.id,
                    "content": row.content,
                    "kind": row.kind,
                    "score": float(score),
                    "metadata": meta,
                    "importance": float(row.importance),
                }
            )
        scored.sort(key=lambda x: x["score"], reverse=True)
        return scored[:top_k]


long_memory = LongMemory()
