# Phase 0.1: Repo and tooling
**Agent:** architect (Codex) | **Branch:** feat/tooling-setup
> Before any task: read `AGENTS.md`, `docs/00-context.md`, `docs/01-field-ownership.md`. One unchecked task per session; tick it and add a one-line result note.

- [ ] Verify JDK 21, Node LTS, Docker and Maven are available (`java -version`, `node -v`, `docker -v`). Write the versions into `docs/ENVIRONMENT.md`. If something is missing, record it there and stop.
- [ ] Add `.editorconfig` (UTF-8, LF, 2 spaces for ts/html/json/yml, 4 for java) and a root `Makefile` with targets `db-up`, `db-down`, `be-test`, `fe-test`, `verify` (runs both test targets).
- [ ] Add a GitHub Actions workflow `.github/workflows/ci.yml` that starts a Postgres service, runs `backend ./mvnw verify` and `frontend npm ci && npm test -- --watch=false && npm run build`. Do not run it; just make it valid YAML.
- [ ] Commit with message `chore: tooling and CI`.
