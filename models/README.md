# LandricsVilla local models

These directories hold optional local model files the villa can use when
present. They are not required and are git-ignored except for these `.gitkeep`
markers.

- `embedding/` -- sentence-transformers (or compatible) embedding models used
  by the long-memory recall layer to index and retrieve memories. If absent,
  the backend falls back to an API-based embedding provider configured in
  Settings.
- `voice/` -- text-to-speech and (optionally) speaker verification models for
  voice input/output. If absent, the backend uses an API-based TTS provider.
- `music/` -- local music generation models for the music pipeline. If absent,
  the backend uses an API-based music provider or skips generation.

Drop model weights directly into the matching subdirectory. The backend probes
each directory at startup; a present, loadable model is preferred, otherwise
the API fallback is used silently.

No model files are ever downloaded automatically, and no provider credentials
are bundled with any model. Whether local or API-based, you supply the keys and
URLs through the in-app Settings page.
