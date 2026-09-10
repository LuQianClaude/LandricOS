# Architecture

LandricsVilla is a three-tier local-first AI companion villa. The three tiers
are interchangeable: any of them can be swapped or replaced without rewriting
the others, as long as the contracts below hold.

## Tiers

1. **Android WebView shell** (`android/`) -- a thin Java app whose only job is
   to host a `WebView`, wire up permissions (internet, microphone, camera,
   storage), provide a small JS bridge for native share/camera/external links,
   and load either the bundled offline frontend (`assets/web/index.html`) or a
   remote server URL the user configures in Settings. It contains no business
   logic and no AI provider keys.
2. **Web frontend** (`frontend/`) -- static HTML/CSS/JS. Talks to the backend
   over the REST API documented in [api.md](api.md). Served by the backend in
   production, or bundled into the Android assets for offline use.
3. **Python FastAPI backend** (`backend/`) -- the actual villa. Owns the
   SQLite database, the memory system, the AI provider layer, the MCP tool
   runtime, and all storage. Authenticates users with JWT. Talks to whatever
   AI provider the user configured (OpenAI-compatible base URL + key).

## Local-first design

- **SQLite is the default.** `DB_URL` in `.env` points at a local file
  (`sqlite:///./database/landricsvilla.db`) created on first run. No external
  database server is required for a single host.
- **User data stays on disk.** All generated files (photos, audio, memories,
  music) land under `STORAGE_DIR` (`storage/` by default). In Docker these are
  bind-mounted volumes.
- **The frontend can run offline.** Bundled into the Android assets, it loads
  from `file:///android_asset/web/index.html`. The shell only needs the server
  URL when the user configures one in Settings.
- **Keys are user-supplied.** No provider credentials are baked into any image
  or APK. The user enters their AI provider key + base URL through the web
  Settings page; the backend stores them per user and uses them only to call
  the user's chosen provider.

## Memory-as-state

Inspired by AionsHome: the villa's persistent state is its memory, not a log of
messages. Three coordinated stores form that state -- short, long, and
emotional memory. They are read and written every turn, so the companion's
"who it is right now" emerges from recalled memory plus the current message,
not from a fixed system prompt. See [memory.md](memory.md).

## Multi-provider AI layer

The backend exposes a thin `AIProvider` abstraction. At request time it reads
the requesting user's configured base URL + key + model (stored per user in the
database, set via Settings) and issues an OpenAI-compatible chat completion
request. Local sentence-transformer embeddings in `models/embedding/` are used
for long-memory recall if present; otherwise an API-based embedding provider
fills in. Same pattern for voice (`models/voice/`) and music (`models/music/`):
present local model wins, API fallback otherwise.

## MCP tool extension model

Tools live in `mcp/tools/` and are registered with an `MCPManager` at backend
startup. During a turn the AI agent may request a tool call; the backend
executes the registered tool and returns the result to the model in the same
turn. Adding a tool is one file plus one registration line. See [mcp.md](mcp.md).

## Request flow

A typical chat turn, end to end:

```
            Android WebView shell                 Web frontend                 FastAPI backend
            -----------------------                ------------                 ---------------
  user types message  -->  JS fetch /chat   -->   build request
                                                              auth: Bearer <JWT>
                                                              POST /api/chat/messages
                                                                                   |
                                                                                   v
                                                          +--------------------+   |
                                                          | auth: verify JWT  |   |
                                                          | load user + companion
                                                          +--------------------+   |
                                                                                   |
                                                                                   v
                                              +---------------------------------------------------+
                                              | memory recall:                                   |
                                              |   short_memory  (recent window)                  |
                                              |   long_memory   (vector recall via embeddings)   |
                                              |   emotional_memory (state)                       |
                                              +---------------------------------------------------+
                                                                                   |
                                                                                   v
                                              +---------------------------------------------------+
                                              | MCPManager: offer available tools to the agent   |
                                              +---------------------------------------------------+
                                                                                   |
                                                                                   v
                                              +---------------------------------------------------+
                                              | AIProvider: chat completion against user's        |
                                              | configured base URL + key + model                |
                                              |   (agent may request tool call -> MCPManager runs |
                                              |    tool, returns result to model in same turn)    |
                                              +---------------------------------------------------+
                                                                                   |
                                                                                   v
                                              +---------------------------------------------------+
                                              | memory write:                                     |
                                              |   append short memory, embed long memory, update |
                                              |   emotional memory                               |
                                              +---------------------------------------------------+
                                                                                   |
                                                                                   v
                                                          <-- 200 { reply, memory_meta }
  JS renders reply  <--  JSON response  <--
  (optional) AndroidBridge.shareText() for native share
```

## Deployment shapes

- **Single host, Docker** (default): one `landricsvilla` service runs the
  backend, serves the frontend, owns the SQLite file and the storage volume.
  See `docker-compose.yml`.
- **Single host, Python**: `scripts/start_backend.sh` runs uvicorn directly.
- **Offline Android**: the shell loads the bundled frontend; the user points
  the shell at the server URL only when a server is reachable.
