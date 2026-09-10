"""Password hashing, JWT tokens and API key encryption helpers.

Uses passlib/bcrypt for passwords, python-jose for JWT, and Fernet (from the
cryptography package, pulled in by python-jose[cryptography]) to encrypt
user-supplied provider API keys at rest.
"""
from __future__ import annotations

import base64
import hashlib
import os
from datetime import datetime, timedelta, timezone
from typing import Any, Dict, Optional

from jose import JWTError, jwt
from passlib.context import CryptContext

from backend.config.settings import settings

# bcrypt rounds handled internally by passlib.
_pwd_context = CryptContext(schemes=["bcrypt"], deprecated="auto")


def hash_password(password: str) -> str:
    """Hash a plaintext password using bcrypt."""
    return _pwd_context.hash(password)


def verify_password(password: str, hashed: str) -> bool:
    """Verify a plaintext password against a stored hash."""
    try:
        return _pwd_context.verify(password, hashed)
    except (ValueError, TypeError):
        return False


def create_access_token(
    data: Dict[str, Any],
    expires_delta: Optional[timedelta] = None,
) -> str:
    """Create a signed JWT carrying the given claims."""
    to_encode: Dict[str, Any] = dict(data)
    expire = datetime.now(timezone.utc) + (
        expires_delta or timedelta(minutes=settings.ACCESS_TOKEN_EXPIRE_MINUTES)
    )
    to_encode["exp"] = expire
    return jwt.encode(to_encode, settings.JWT_SECRET, algorithm=settings.JWT_ALG)


def decode_token(token: str) -> Optional[Dict[str, Any]]:
    """Decode and validate a JWT. Returns None if invalid/expired."""
    try:
        return jwt.decode(token, settings.JWT_SECRET, algorithms=[settings.JWT_ALG])
    except JWTError:
        return None


# --- API key encryption -------------------------------------------------
# Derive a 32-byte Fernet key from JWT_SECRET so stored provider keys are
# encrypted at rest without requiring a second secret.


def _fernet_key() -> bytes:
    raw = hashlib.sha256(settings.JWT_SECRET.encode("utf-8")).digest()
    return base64.urlsafe_b64encode(raw)


def _get_fernet():  # pragma: no cover - imported lazily
    try:
        from cryptography.fernet import Fernet
    except ImportError as exc:  # pragma: no cover
        raise RuntimeError("cryptography package is required to encrypt keys") from exc
    return Fernet(_fernet_key())


def encrypt_key(value: str) -> str:
    """Encrypt a user-supplied API key for storage."""
    if not value:
        return ""
    return _get_fernet().encrypt(value.encode("utf-8")).decode("utf-8")


def decrypt_key(value: str) -> str:
    """Decrypt a stored API key."""
    if not value:
        return ""
    try:
        return _get_fernet().decrypt(value.encode("utf-8")).decode("utf-8")
    except Exception:
        return ""


def generate_token_bytes(length: int = 16) -> str:
    """Return a URL-safe random token (used for session/refresh ids)."""
    return base64.urlsafe_b64encode(os.urandom(length)).decode("utf-8").rstrip("=")
