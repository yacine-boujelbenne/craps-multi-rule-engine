# Agent roster (Maestro agents on Codex and OpenCode)

Create each agent in Maestro with `Cmd/Ctrl+N`, pick the provider, and set the working directory to this repo (or a worktree of it). Providers are suggestions; swap freely, since both read `AGENTS.md`.

| Agent name | Provider | Worktree branch | Owns |
|---|---|---|---|
| `architect` | Codex | main repo | Moderator of Group Chat, specs in `docs/`, schema design, reviews merges |
| `backend` | Codex | `feat/backend-*` | Spring Boot services, Flyway, link and validation engines |
| `frontend` | OpenCode | `feat/frontend-*` | Angular features, forms, stepper, PDF preview |
| `docgen` | Codex | `feat/docgen-*` | PDF templates (invoice, PL, BL per carrier, HC, CO, PS), stamp variants |
| `ingestion` | OpenCode | `feat/ingestion-*` | PDF/Excel/Word/CSV extraction, OCR adapters, import review API |
| `qa` | OpenCode | `feat/qa-*` | Test suites, golden-file PDF tests, e2e, validation edge cases |

Rules: one agent per area; an agent edits only its area. Cross-area questions go through `@mentions` (single question) or Group Chat (multi-round).
