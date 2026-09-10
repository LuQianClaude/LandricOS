# Memory system

LandricsVilla treats memory as the primary long-term state of the villa, not
as an append-only log of messages. The companion's behaviour in any given turn
is a function of the current message plus recalled memory; the prompt is
assembled fresh each turn from those pieces.

There are three coordinated stores plus an embedding layer and a recall
orchestrator. All four live in the backend (`backend/memory/`) and share a
single SQLite database plus the `storage/memories/` directory for serialized
artifacts.

## Stores

### short_memory

A rolling window of recent turns (messages and tool results) kept verbatim so
the model has coherent immediate context. Older entries age out by turn count
or token budget. This is the cheapest recall path and is always included in
the prompt.

- Storage: SQLite rows keyed by `companion_id` and `created_at`.
- Recall: by `companion_id`, ordered by `created_at`, bounded by a turn/token
  limit.

### long_memory

Durable facts, episodes, and notes worth recalling across sessions. Each entry
is embedded into a vector and retrieved by semantic similarity to the current
message. This is the recall path that gives the companion continuity across
days or weeks.

- Storage: SQLite rows with an `embedding` blob (float32 vector) plus a
  serialized index snapshot under `storage/memories/`.
- Embeddings: local sentence-transformers model from `models/embedding/` if
  present, else an API-based embedding provider configured in Settings.
- Recall: embed the current message, query the top-k nearest entries by cosine
  similarity, inject their contents (with optional scores) into the prompt.

### emotional_memory

The companion's tracked affect / state, which biases tone and which memories
get emphasised. This is what makes the companion feel like the same entity
over time rather than a fresh assistant every turn.

- Storage: a small per-companion state document in SQLite (current mood,
  intensity, drift, last-updated) plus a history of state transitions.
- Recall: load the current state document and include it in the prompt as a
  concise "current state" block; optionally surface recent transitions.
- Update: every turn, the model's reply is parsed (or a small structured
  output is requested) for an emotional delta, which is folded into the state.

## Embedding layer

A single `Embedder` interface hides whether embeddings come from a local
sentence-transformers model or an API provider:

- `embed(text) -> vector`
- `embed_batch(texts) -> matrix`
- `cosine(a, b) -> float`

On startup the backend probes `models/embedding/`; a loadable local model is
preferred. Otherwise it falls back to the API embedding provider the user
configured in Settings. The choice is transparent to the stores above.

## Recall orchestration

A `Recall` orchestrator assembles a turn's context from the three stores:

```
current user message
        |
        v
+----------------------------------------------+
| short_memory.get(companion, recent_limit)   |  <-- always included
| long_memory.search(embed(msg), top_k)       |  <-- semantic recall
| emotional_memory.get(companion)             |  <-- current state
+----------------------------------------------+
        |
        v
build prompt:  system(state) + recalled(long) + recent(short) + user msg
        |
        v
AIProvider.chat(...)      <-- may request MCP tool calls mid-turn
        |
        v
+----------------------------------------------+
| short_memory.append(user msg, assistant msg)|
| long_memory.maybe_embed(assistant msg + tool |
|                         results if durable)   |
| emotional_memory.apply(delta)                 |
+----------------------------------------------+
```

## What gets remembered

Not every message becomes long memory. The orchestrator asks the model (or a
small classifier) whether the turn produced anything worth durable storage:
facts about the user, decisions, emotional events, or tool results with
lasting value. Only those entries are embedded and written to long memory.
Everything is in short memory until it ages out.

## Per-turn storage and recall

For a single `POST /api/chat/messages` turn:

1. Load the companion's identity and emotional state.
2. Pull the recent short-memory window.
3. Embed the user's message and query long memory for top-k matches.
4. Assemble the prompt: `system(state) + recalled(long) + recent(short) + msg`.
5. Offer the agent the set of available MCP tools (see [mcp.md](mcp.md)).
6. Call the configured AI provider; if the agent requests a tool call, run it
   via the MCP manager and feed the result back to the model in the same
   turn.
7. Write the user message and assistant reply to short memory; embed and
   persist any durable facts to long memory; fold the emotional delta into the
   state.
8. Return the reply plus `memory_meta` (counts and the new emotional state) to
   the caller.

## Privacy and ownership

All memory lives in the user's own SQLite database and `storage/memories/`
directory. Nothing about memory is sent to any AI provider except the final
prompt text the user chose to send by chatting. AI provider keys are never
written to memory; they live only in the per-user settings table.
