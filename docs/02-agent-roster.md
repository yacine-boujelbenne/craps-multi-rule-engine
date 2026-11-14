# Agent roster (Maestro agents on Codex and OpenCode)

Set every agent's working directory to `/run/media/yacine/yassine/DMS/export-docs-platform`. Read `AGENTS.md` before every task. Use this one checkout and its current branch; coordinate branch switches and Git mutations. Older worktree instructions in plans/runbooks are superseded by the owner's single-folder rule.

| Agent name | Provider | Shared checkout | Owns |
|---|---|---|---|
| `architect` (Agent 1) | Codex | Current branch | Architecture/specs, Flyway schema and migrations, OpenAPI contracts, bounded implementation tasks, integration and acceptance review |
| `backend` (Agent 2) | Codex | Current branch | Backend services, controllers, persistence, mappings, authorization and validation against Agent 1's contracts; report schema/API gaps before changing them |
| `frontend` | OpenCode | Current branch | Angular features, forms, stepper, PDF preview |
| `docgen` | Codex | Current branch | Approved PDF templates and unsigned/client-preview variants following current output scope |
| `ingestion` | OpenCode | Current branch | PDF/Excel/Word/CSV extraction, OCR adapters, import review API |
| `qa` | OpenCode | Current branch | Authorized verification and validation edge cases |

Rules: one agent per area; an agent edits only its area. Cross-area questions go through `@mentions` (single question) or Group Chat (multi-round).
