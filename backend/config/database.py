"""SQLAlchemy 2.x async engine, session factory and Base.

SQLite is the default store. A bare "sqlite:///" URL is rewritten to use the
aiosqlite async driver so the same code path works locally without a server.
"""
from __future__ import annotations

from typing import AsyncIterator

from sqlalchemy.ext.asyncio import (
    AsyncSession,
    async_sessionmaker,
    create_async_engine,
)
from sqlalchemy.orm import DeclarativeBase

from backend.config.settings import settings


def _to_async_url(url: str) -> str:
    """Ensure the SQLite URL uses the aiosqlite driver for async SQLAlchemy."""
    if url.startswith("sqlite:///"):
        return url.replace("sqlite:///", "sqlite+aiosqlite:///", 1)
    return url


DB_URL = _to_async_url(settings.DB_URL)

engine = create_async_engine(DB_URL, echo=False, future=True)
async_session_maker = async_sessionmaker(
    engine,
    class_=AsyncSession,
    expire_on_commit=False,
    autoflush=False,
)


class Base(DeclarativeBase):
    """Declarative base for all ORM models."""

    pass


async def get_db() -> AsyncIterator[AsyncSession]:
    """FastAPI dependency that yields an async session."""
    async with async_session_maker() as session:
        try:
            yield session
        finally:
            await session.close()


async def init_db() -> None:
    """Create all tables. Called from the app lifespan startup."""
    # Import here so models register with Base.metadata before create_all.
    from backend.database import models as _models  # noqa: F401

    async with engine.begin() as conn:
        await conn.run_sync(Base.metadata.create_all)
