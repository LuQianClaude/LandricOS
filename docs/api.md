# REST API reference

Base URL: `http(s)://<APP_HOST>:<APP_PORT>` (defaults to `http://localhost:8000`).

All endpoints under `/api` require a `Authorization: Bearer <JWT>` header
except where noted. The frontend obtains the JWT from `POST /api/auth/login`.

Field names are JSON `snake_case`. Timestamps are ISO 8601 UTC strings.
Empty optional fields are omitted, not `null`, unless noted.

Conventions used below:

- `Auth: yes` -- requires a valid bearer token.
- `Auth: no` -- public endpoint.
- Request bodies are JSON. Response bodies are JSON.

## Health

### `GET /health`

Liveness probe. No auth.

- Request body: none.
- Response `200`:
  ```json
  { "status": "ok" }
  ```

## User

### `POST /api/auth/register`

Create a new account.

- Auth: no
- Request body:
  ```json
  { "username": "string", "password": "string", "email": "string (optional)" }
  ```
- Response `201`:
  ```json
  { "user_id": "uuid", "username": "string", "token": "jwt-string" }
  ```

### `POST /api/auth/login`

Exchange credentials for a JWT.

- Auth: no
- Request body:
  ```json
  { "username": "string", "password": "string" }
  ```
- Response `200`:
  ```json
  { "user_id": "uuid", "username": "string", "token": "jwt-string" }
  ```

### `GET /api/users/me`

- Auth: yes
- Request body: none.
- Response `200`:
  ```json
  {
    "user_id": "uuid",
    "username": "string",
    "email": "string or null",
    "created_at": "iso8601"
  }
  ```

### `PUT /api/users/me`

- Auth: yes
- Request body (any subset of fields):
  ```json
  { "email": "string (optional)", "password": "string (optional)" }
  ```
- Response `200`: same as `GET /api/users/me`.

## Models

The model configuration endpoints store the user's AI provider key, base URL,
and model name. Keys are stored encrypted at rest by the backend and are never
returned in full by any endpoint.

### `GET /api/models/config`

- Auth: yes
- Request body: none.
- Response `200`:
  ```json
  {
    "base_url": "string or null",
    "model": "string or null",
    "has_key": true,
    "last_validated_at": "iso8601 or null"
  }
  ```

### `PUT /api/models/config`

- Auth: yes
- Request body (any subset of fields; `key` is write-only and never returned):
  ```json
  {
    "base_url": "string (optional)",
    "model": "string (optional)",
    "key": "string (optional)"
  }
  ```
- Response `200`: same shape as `GET /api/models/config` (no `key` field).

### `POST /api/models/test`

Send a trivial ping to the configured provider to validate the key + URL.

- Auth: yes
- Request body: none.
- Response `200`:
  ```json
  { "ok": true, "model": "string", "latency_ms": 123 }
  ```
- Response `400` on failure:
  ```json
  { "ok": false, "error": "string" }
  ```

## Chat

### `POST /api/chat/messages`

Send a user message and run one companion turn (memory recall + model call +
memory write + any MCP tool calls).

- Auth: yes
- Request body:
  ```json
  {
    "companion_id": "uuid",
    "content": "string",
    "voice": false
  }
  ```
- Response `200`:
  ```json
  {
    "message_id": "uuid",
    "companion_id": "uuid",
    "role": "assistant",
    "content": "string",
    "audio_url": "string or null",
    "tool_calls": [ { "name": "string", "ok": true, "summary": "string" } ],
    "memory_meta": {
      "short_count": 12,
      "long_recall_count": 3,
      "emotional_state": "string"
    },
    "created_at": "iso8601"
  }
  ```

### `GET /api/chat/history`

- Auth: yes
- Query params: `companion_id` (required), `limit` (default 50), `before`
  (iso8601 cursor, optional).
- Request body: none.
- Response `200`:
  ```json
  {
    "messages": [
      {
        "message_id": "uuid",
        "companion_id": "uuid",
        "role": "user|assistant",
        "content": "string",
        "audio_url": "string or null",
        "created_at": "iso8601"
      }
    ],
    "next_cursor": "iso8601 or null"
  }
  ```

### `POST /api/chat/voice`

Upload an audio recording for transcription and a companion turn.

- Auth: yes
- Request body: `multipart/form-data` with field `audio` (webm/wav) and
  `companion_id`.
- Response `200`: same shape as `POST /api/chat/messages` plus
  `transcript: "string"`.

## Memory

### `GET /api/memory`

- Auth: yes
- Query params: `companion_id` (required), `store` (`short|long|emotional`,
  default `long`), `limit` (default 20).
- Request body: none.
- Response `200`:
  ```json
  {
    "store": "long",
    "items": [
      {
        "memory_id": "uuid",
        "companion_id": "uuid",
        "content": "string",
        "score": 0.83,
        "tags": ["string"],
        "created_at": "iso8601"
      }
    ]
  }
  ```

### `POST /api/memory`

Manually write a memory entry.

- Auth: yes
- Request body:
  ```json
  {
    "companion_id": "uuid",
    "store": "short|long|emotional",
    "content": "string",
    "tags": ["string (optional)"]
  }
  ```
- Response `201`:
  ```json
  { "memory_id": "uuid", "store": "string", "created_at": "iso8601" }
  ```

### `DELETE /api/memory/{memory_id}`

- Auth: yes
- Response `204`: no body.

### `POST /api/memory/reset`

Clear a single store for a companion (dangerous; used by Settings).

- Auth: yes
- Request body:
  ```json
  { "companion_id": "uuid", "store": "short|long|emotional|all" }
  ```
- Response `200`:
  ```json
  { "cleared": ["short", "long", "emotional"] }
  ```

## Music

### `POST /api/music/generate`

- Auth: yes
- Request body:
  ```json
  { "companion_id": "uuid", "prompt": "string", "duration_s": 30 }
  ```
- Response `202` (async):
  ```json
  { "job_id": "uuid", "status": "pending" }
  ```

### `GET /api/music/jobs/{job_id}`

- Auth: yes
- Response `200`:
  ```json
  { "job_id": "uuid", "status": "pending|running|done|failed", "track_url": "string or null", "error": "string or null" }
  ```

### `GET /api/music/tracks`

- Auth: yes
- Query params: `companion_id` (required), `limit` (default 20).
- Response `200`:
  ```json
  {
    "tracks": [
      { "track_id": "uuid", "companion_id": "uuid", "prompt": "string", "url": "string", "duration_s": 30, "created_at": "iso8601" }
    ]
  }
  ```

## Pet

A virtual pet that lives in the villa alongside the companion.

### `GET /api/pet/{companion_id}`

- Auth: yes
- Response `200`:
  ```json
  {
    "companion_id": "uuid",
    "name": "string",
    "mood": "string",
    "hunger": 0.2,
    "energy": 0.8,
    "last_updated_at": "iso8601"
  }
  ```

### `POST /api/pet/{companion_id}/interact`

- Auth: yes
- Request body:
  ```json
  { "action": "feed|play|rest|pet" }
  ```
- Response `200`: same shape as `GET /api/pet/{companion_id}`.

## Moments

Short journal entries the companion keeps about the user.

### `GET /api/moments`

- Auth: yes
- Query params: `companion_id` (required), `limit` (default 20), `before`
  (iso8601, optional).
- Response `200`:
  ```json
  {
    "moments": [
      { "moment_id": "uuid", "companion_id": "uuid", "title": "string", "body": "string", "mood": "string", "created_at": "iso8601" }
    ],
    "next_cursor": "iso8601 or null"
  }
  ```

### `POST /api/moments`

- Auth: yes
- Request body:
  ```json
  { "companion_id": "uuid", "title": "string", "body": "string", "mood": "string (optional)" }
  ```
- Response `201`:
  ```json
  { "moment_id": "uuid", "created_at": "iso8601" }
  ```

### `DELETE /api/moments/{moment_id}`

- Auth: yes
- Response `204`: no body.

## MCP

### `GET /api/mcp/tools`

List tools the agent may call.

- Auth: yes
- Response `200`:
  ```json
  {
    "tools": [
      {
        "name": "string",
        "description": "string",
        "parameters_schema": { "...json schema..." }
      }
    ]
  }
  ```

### `POST /api/mcp/tools/{name}/invoke`

Invoke a tool directly (used by the frontend for testing, and by the agent
during a chat turn).

- Auth: yes
- Request body:
  ```json
  { "arguments": { "...per tool schema..." } }
  ```
- Response `200`:
  ```json
  { "ok": true, "result": "any" }
  ```
- Response `400` on failure:
  ```json
  { "ok": false, "error": "string" }
  ```

### `GET /api/mcp/tools/{name}/schema`

- Auth: yes
- Response `200`:
  ```json
  { "name": "string", "description": "string", "parameters_schema": { "...json schema..." } }
  ```
