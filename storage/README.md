# LandricsVilla storage

This directory holds user-generated files produced and consumed by the villa.
The default location is `./storage` (see `.env.example`, `STORAGE_DIR`); in the
Docker container it is `/app/storage`. Contents are git-ignored except for the
`.gitkeep` markers that keep each subdirectory present in the repo.

Subdirectories:

- `users/` -- profile photos and account avatars.
- `photos/` -- user-uploaded and generated photos.
- `audio/` -- voice input recordings and TTS output audio.
- `videos/` -- short video clips (moments, captures).
- `memories/` -- serialized memory artifacts (embeddings, snapshots, exports).
- `generated_music/` -- music tracks produced by the music pipeline.

No secrets live in storage. AI provider keys are stored by the backend in the
database, never as files here.
