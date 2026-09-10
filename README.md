# LandricsVilla

LandricsVilla is a local-first AI companion villa. Think of it as a small,
private residence where one or more AI companions live alongside their owner
(Jasmine). The villa keeps its memory on disk by default, talks to whatever AI
provider you point it at, and exposes itself through three interchangeable
tiers: an Android WebView shell, a web frontend, and a Python FastAPI backend.

The project is inspired by the AionsHome philosophy of self-hosted AI
companions: memory-as-state, multi-model access, MCP tool extensions, and
SQLite as the default store. You own your data; you bring your own AI keys.

## Features

- Local-first: SQLite is the default database, user data lives under `storage/`,
  and the web frontend can be bundled into the Android app so it works offline.
- Multi-provider AI: no provider keys are baked in. You add your own AI
  provider API key and base URL in the in-app Settings page, and LandricsVilla
  routes chat through that provider (OpenAI-compatible endpoints by default).
- Memory system: three coordinated stores -- short memory (recent conversation
  context), long memory (semantic vector recall), and emotional memory
  (companion state tracking). See [docs/memory.md](docs/memory.md).
- MCP tool extensions: tools live in `mcp/tools/` and are registered through an
  MCP manager the AI agent can call during a turn. See [docs/mcp.md](docs/mcp.md).
- Three client tiers: a thin Android WebView shell (`android/`), a web frontend
  (`frontend/`), and a FastAPI backend (`backend/`).
- Voice and media: audio recording permission is requested for voice input;
  storage permissions cover uploads and generated media.

## Tech Stack

- Backend: Python 3.11, FastAPI, Uvicorn, SQLite (default), JWT auth.
- Frontend: static HTML/CSS/JS served by the backend or bundled in the Android
  assets.
- Mobile: Android WebView shell (Java, AndroidX, minSdk 24, targetSdk 34).
- AI: pluggable provider layer driven by user-supplied key + base URL.
- Container: Docker + docker-compose for a single-service deployment.

## Quick Start

Option A -- Docker (simplest):

    docker-compose up --build

Option B -- Local Python:

    cp .env.example .env
    bash scripts/start_backend.sh

Then open http://localhost:8000 in a browser, or open the web frontend
directly via `frontend/index.html` for development.

Option C -- Android build:

    bash scripts/build_android.sh

Install the resulting APK, then open LandricsVilla. On first launch it loads
the bundled offline frontend; use the in-app Settings page to point it at your
server URL and to add your AI provider key.

## Project Structure

```
LandricsVilla/
├── README.md
├── LICENSE
├── requirements.txt
├── .env.example
├── docker-compose.yml
├── .gitignore
├── android/                  # Android WebView client
├── frontend/                 # Web frontend
├── backend/                  # Python FastAPI backend
├── storage/                  # User data
├── database/                 # SQLite DB + migrations
├── models/                   # AI model files
├── scripts/                  # Build/deploy scripts
└── docs/                     # Documentation
```

## Configuration

Two layers of configuration:

1. Server environment (`.env`, copied from `.env.example`): host/port, database
   URL, JWT secret, storage directory, log level, CORS origins. See
   `.env.example` for the full list and safe defaults.
2. In-app settings (per user, stored by the backend): AI provider key, base
   URL, model selection, companion profile. These are entered through the web
   Settings page after login.

The backend reads `.env` on startup. The Android shell reads a server URL from
its own SharedPreferences (set via the Settings page) and falls back to the
bundled `assets/web/index.html` when no server is configured.

## AI Model Setup

LandricsVilla ships with no AI provider keys and no hardcoded base URLs. To
enable AI chat:

1. Start the backend (Quick Start above) and log in.
2. Open the Settings page in the web UI (or in the Android app).
3. Enter your AI provider API key and the provider's base URL (any
   OpenAI-compatible endpoint works).
4. Pick a model name your provider serves.
5. Save. Chat now routes through your provider. Your key is stored by the
   backend (never in the Android APK) and used only to call your provider.

Optional local models (sentence-transformers embeddings, TTS voice models,
music models) can be dropped into `models/`; the app uses them if present and
falls back to API-based alternatives otherwise. See [models/README.md](models/README.md).

## Memory System

LandricsVilla treats memory as the primary long-term state of the villa, not
as an append-only log. Three stores coordinate per turn:

- `short_memory`: the rolling recent-context window injected into every
  prompt.
- `long_memory`: semantic vector recall over durable facts and episodes,
  indexed by local sentence embeddings.
- `emotional_memory`: companion affect / state tracking that biases tone and
  recall.

See [docs/memory.md](docs/memory.md) for storage schema and per-turn recall
flow.

## MCP Tools

Tools are plain Python modules in `mcp/tools/` registered with an MCP manager.
The AI agent can request a tool call during a turn; the backend executes it and
returns the result to the model. Adding a tool is one file plus one
registration line. See [docs/mcp.md](docs/mcp.md).

## Android Build

Requirements: Android SDK, JDK 17, and the Gradle wrapper present in
`android/`. The wrapper is not committed by default; generate it once with
`gradle wrapper` inside `android/` if absent.

    bash scripts/build_android.sh

The script copies `frontend/*` into
`android/app/src/main/assets/web/`, runs `./gradlew assembleDebug`, and prints
the path to the built APK.

## License

MIT License, Copyright (c) 2026 Jasmine. See [LICENSE](LICENSE).

## Acknowledgements

LandricsVilla is inspired by the AionsHome self-hosted AI companion philosophy:
local-first, memory-as-state, multi-model access, MCP tool extensions, and
SQLite as the default store. The AionsHome project demonstrated that a
companion can live entirely on your own hardware; LandricsVilla carries that
idea forward with a FastAPI backend and an Android WebView shell.
