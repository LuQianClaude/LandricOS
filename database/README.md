# LandricsVilla database

This directory holds the SQLite database and migrations for LandricsVilla.

- `landricsvilla.db` is created automatically on first run from the backend's
  schema definitions. You do not need to create it by hand.
- Migration scripts live in `migrations/` and are run by `scripts/deploy.sh`
  (or manually via the backend's migration entrypoint) when schema changes
  ship.
- The default connection string is
  `sqlite:///./database/landricsvilla.db` (see `.env.example`, `DB_URL`). In
  the Docker container the path is `/app/database/landricsvilla.db`.
- The `.db`, `.db-journal`, `.db-wal`, and `.db-shm` files are git-ignored; only
  the `migrations/` source and this README are tracked.

Back up the database by copying `landricsvilla.db` (and any `-wal`/`-shm`
sidecars while the server is running) to a safe location.
