"""Local text embeddings with a runnable fallback.

Backend selection:
  - "sentence_transformers": use the all-MiniLM-L6-v2 model if installed.
  - "hashing" (default): a deterministic hashing embedding that needs no GPU
    and no model download. Quality is lower but it is always runnable.

embed(text) -> np.ndarray[float32]
cosine_similarity(a, b) -> float in [-1, 1]
"""
from __future__ import annotations

import base64
import hashlib
import re
from typing import List, Optional

import numpy as np

from backend.config.settings import settings
from backend.utils.logger import get_logger

logger = get_logger(__name__)

_TOKEN_RE = re.compile(r"[a-z0-9]+")
_STOPWORDS = {
    "the", "a", "an", "and", "or", "but", "if", "then", "of", "to", "in",
    "on", "at", "by", "for", "with", "is", "are", "was", "were", "be",
    "been", "this", "that", "it", "as", "so", "i", "you", "he", "she",
    "we", "they", "me", "him", "her", "us", "them",
}


def _tokenize(text: str) -> List[str]:
    return [t for t in _TOKEN_RE.findall(text.lower()) if t not in _STOPWORDS]


class _SentenceTransformerBackend:
    """Wraps the optional sentence-transformers model."""

    def __init__(self, model_name: str = "all-MiniLM-L6-v2") -> None:
        try:
            from sentence_transformers import SentenceTransformer
        except ImportError as exc:  # pragma: no cover
            raise RuntimeError("sentence-transformers not installed") from exc
        self.model = SentenceTransformer(model_name)
        self.dim = int(self.model.get_sentence_embedding_dimension())

    def embed(self, text: str) -> np.ndarray:
        vec = self.model.encode(text, normalize_embeddings=True)
        return np.asarray(vec, dtype=np.float32)


class _HashingBackend:
    """Deterministic hashing embedding. Always runnable, no external deps."""

    def __init__(self, dim: int = 256) -> None:
        self.dim = max(8, int(dim))

    def embed(self, text: str) -> np.ndarray:
        vec = np.zeros(self.dim, dtype=np.float32)
        tokens = _tokenize(text)
        if not tokens:
            tokens = [text.strip().lower() or "<empty>"]
        for token in tokens:
            # Hash token to a bucket and to a sign, accumulate weighted counts.
            digest = hashlib.blake2b(token.encode("utf-8"), digest_size=16).digest()
            bucket = int.from_bytes(digest[:4], "little") % self.dim
            sign = 1.0 if (digest[4] & 1) else -1.0
            weight = 1.0 + np.log1p(float(len(token)))
            vec[bucket] += sign * weight
        # L2 normalize so cosine similarity is a dot product.
        norm = float(np.linalg.norm(vec))
        if norm > 0:
            vec /= norm
        return vec


class Embedding:
    """Public embedding facade. Picks the configured backend with fallback."""

    _instance: Optional["Embedding"] = None

    def __init__(self) -> None:
        backend = settings.EMBEDDING_BACKEND.lower()
        self.backend_name = "hashing"
        self._backend: object
        if backend == "sentence_transformers":
            try:
                self._backend = _SentenceTransformerBackend()
                self.backend_name = "sentence_transformers"
                logger.info("Embedding backend: sentence_transformers")
                return
            except Exception as exc:  # pragma: no cover
                logger.warning(
                    "sentence_transformers unavailable (%s); falling back to hashing",
                    exc,
                )
        self._backend = _HashingBackend(dim=settings.EMBEDDING_DIM)
        logger.info("Embedding backend: hashing (dim=%d)", settings.EMBEDDING_DIM)

    @classmethod
    def get(cls) -> "Embedding":
        if cls._instance is None:
            cls._instance = cls()
        return cls._instance

    @property
    def dim(self) -> int:
        return int(self._backend.dim) if hasattr(self._backend, "dim") else settings.EMBEDDING_DIM

    def embed(self, text: str) -> np.ndarray:
        return self._backend.embed(text)

    def embed_many(self, texts: List[str]) -> np.ndarray:
        return np.vstack([self.embed(t) for t in texts]) if texts else np.zeros((0, self.dim), dtype=np.float32)


def cosine_similarity(a: np.ndarray, b: np.ndarray) -> float:
    """Cosine similarity between two vectors. Returns 0 for zero vectors."""
    a = np.asarray(a, dtype=np.float32)
    b = np.asarray(b, dtype=np.float32)
    if a.shape != b.shape:
        # Resize to the smaller dim if mismatched (defensive).
        n = min(a.shape[0], b.shape[0])
        a, b = a[:n], b[:n]
    na = float(np.linalg.norm(a))
    nb = float(np.linalg.norm(b))
    if na == 0.0 or nb == 0.0:
        return 0.0
    return float(np.dot(a, b) / (na * nb))


def encode_vector(vec: np.ndarray) -> str:
    """Serialize a vector to a base64 string for DB storage."""
    arr = np.asarray(vec, dtype=np.float32).tobytes()
    return base64.b64encode(arr).decode("ascii")


def decode_vector(text: str) -> np.ndarray:
    """Decode a base64 vector string back to a numpy array."""
    if not text:
        return np.zeros(Embedding.get().dim, dtype=np.float32)
    raw = base64.b64decode(text.encode("ascii"))
    return np.frombuffer(raw, dtype=np.float32).copy()
