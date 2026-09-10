"""Pydantic v2 request/response schemas for all entities."""
from __future__ import annotations

from datetime import datetime
from typing import Any, Dict, List, Optional

from pydantic import BaseModel, ConfigDict, Field


# --- User ---------------------------------------------------------------


class UserCreate(BaseModel):
    username: str = Field(min_length=3, max_length=64)
    password: str = Field(min_length=6, max_length=128)
    email: Optional[str] = None
    display_name: Optional[str] = None


class UserLogin(BaseModel):
    username: str
    password: str


class UserUpdate(BaseModel):
    email: Optional[str] = None
    display_name: Optional[str] = None
    password: Optional[str] = None


class UserOut(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: int
    username: str
    email: Optional[str] = None
    display_name: Optional[str] = None
    is_active: bool
    created_at: datetime


class TokenOut(BaseModel):
    access_token: str
    token_type: str = "bearer"
    user: UserOut


# --- Provider / model config ------------------------------------------


class ModelsListRequest(BaseModel):
    """User fills in their own API key + base URL; we fetch models live."""

    provider: str = "openai"
    api_key: str
    base_url: str


class ModelInfo(BaseModel):
    id: str
    name: Optional[str] = None
    owned_by: Optional[str] = None


class ModelsListResponse(BaseModel):
    provider: str
    base_url: str
    models: List[ModelInfo]


class ConnectivityTestRequest(BaseModel):
    provider: str = "openai"
    api_key: str
    base_url: str
    model: Optional[str] = None


class ConnectivityTestResponse(BaseModel):
    ok: bool
    provider: str
    detail: str
    models: List[ModelInfo] = Field(default_factory=list)


class ProviderConfigCreate(BaseModel):
    provider: str
    api_key: str
    base_url: str
    model: Optional[str] = None
    is_active: bool = False


class ProviderConfigOut(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: int
    provider: str
    base_url: str
    model: Optional[str] = None
    is_active: bool
    created_at: datetime
    # never expose the raw key; only a masked hint
    api_key_hint: Optional[str] = None


class ProviderConfigUpdate(BaseModel):
    api_key: Optional[str] = None
    base_url: Optional[str] = None
    model: Optional[str] = None
    is_active: Optional[bool] = None


# --- Chat --------------------------------------------------------------


class ChatMessage(BaseModel):
    role: str = "user"  # user | assistant | system | tool
    content: str


class ChatSendRequest(BaseModel):
    conversation_id: Optional[int] = None
    message: str
    model: Optional[str] = None
    stream: bool = False


class ChatMessageOut(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: int
    conversation_id: int
    role: str
    content: str
    tool_name: Optional[str] = None
    created_at: datetime


class ChatHistoryResponse(BaseModel):
    conversation_id: int
    messages: List[ChatMessageOut]


class ChatSendResponse(BaseModel):
    conversation_id: int
    user_message: ChatMessageOut
    assistant_message: ChatMessageOut
    tool_calls: List[Dict[str, Any]] = Field(default_factory=list)


# --- Memory ------------------------------------------------------------


class MemoryCreate(BaseModel):
    content: str
    kind: str = "long"
    metadata: Dict[str, Any] = Field(default_factory=dict)
    importance: float = 0.5


class MemoryOut(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: int
    kind: str
    content: str
    metadata: Dict[str, Any] = Field(default_factory=dict)
    importance: float
    created_at: datetime


class MemorySearchResult(BaseModel):
    id: int
    content: str
    kind: str
    score: float
    metadata: Dict[str, Any] = Field(default_factory=dict)


class MemorySearchResponse(BaseModel):
    query: str
    results: List[MemorySearchResult]


class EmotionalState(BaseModel):
    mood: str = "neutral"
    valence: float = 0.0
    arousal: float = 0.0
    note: Optional[str] = None


# --- Pet ---------------------------------------------------------------


class PetState(BaseModel):
    name: str = "Companion"
    species: str = "spirit"
    hunger: float = 50.0
    mood: float = 80.0
    energy: float = 80.0


class PetInteractRequest(BaseModel):
    action: str  # feed | play | rest | pet
    amount: float = 10.0


class PetOut(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: int
    name: str
    species: str
    hunger: float
    mood: float
    energy: float


# --- Moments -----------------------------------------------------------


class MomentCreate(BaseModel):
    content: str
    image_url: Optional[str] = None


class MomentOut(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: int
    user_id: int
    content: str
    image_url: Optional[str] = None
    likes: int
    created_at: datetime


class MomentLikeResponse(BaseModel):
    id: int
    likes: int


# --- Music -------------------------------------------------------------


class MusicGenerateRequest(BaseModel):
    prompt: str
    duration: float = 10.0
    style: Optional[str] = None


class MusicTrackOut(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: int
    title: str
    prompt: str
    file_path: str
    duration: Optional[float] = None
    created_at: datetime


class MusicGenerateResponse(BaseModel):
    track: MusicTrackOut
    detail: str = "generated"


# --- MCP ---------------------------------------------------------------


class McpToolParam(BaseModel):
    name: str
    type: str
    description: str
    required: bool = False
    default: Optional[Any] = None


class McpToolInfo(BaseModel):
    name: str
    description: str
    parameters: List[Dict[str, Any]] = Field(default_factory=list)


class McpCallRequest(BaseModel):
    name: str
    args: Dict[str, Any] = Field(default_factory=dict)


class McpCallResponse(BaseModel):
    name: str
    ok: bool
    result: Any = None
    error: Optional[str] = None


# --- Generic -----------------------------------------------------------


class OkResponse(BaseModel):
    ok: bool = True
    message: Optional[str] = None


class ErrorResponse(BaseModel):
    detail: str


class HealthResponse(BaseModel):
    status: str = "ok"
    app: str = "LandricsVilla"
    version: str = "1.0.0"
