# Single project workspace

Canonical checkout: `/run/media/yacine/yassine/DMS/export-docs-platform`.

```text
export-docs-platform/
  backend/    Spring Boot, PostgreSQL/Flyway schema and backend tests
  frontend/   Angular, including the existing uncommitted frontend work
  docs/       Architecture, contracts and handoffs
  infra/      Local run helpers and migration diagnostics
```

The former architecture-foundations, backend-projects, commercial-backend, backend-commercial-csrf-fix and strict-uuid-binding directories were isolated Git worktrees. Their latest backend changes are integrated with the frontend branch in this checkout. Their branches remain in Git history; redundant physical copies are removed. Use branches within this checkout and coordinate switches. Do not recreate separate project folders.

Frontend/Maestro files are preserved byte-for-byte through consolidation; no Angular source edits are part of the move. Uncommitted frontend work remains uncommitted for its owner. Local PostgreSQL configuration is retained in ignored .env. Saved verification reports are kept under ignored .local/verification; generated build/dependency caches may be recreated as needed. JOURNAL.md is retained at the project root.

Backend status: commercial setup and strict UUID HTTP binding are integrated. The last host run passed 120 of 121 PostgreSQL integration invocations; its only failure accepted a shortened UUID in a route. A shared HTTP binder now rejects shortened/padded/malformed UUIDs before service lookup while accepting full upper/lowercase UUIDs and retaining 404 for valid absent IDs. All 79 unit/MVC tests and 18 contract checks pass. A fresh full host run is required after this correction; expected total is 200 (79 unit/MVC + 121 PostgreSQL).

```bash
cd /run/media/yacine/yassine/DMS/export-docs-platform
./backend/verify.sh
```

Frontend/browser acceptance remains with the frontend owner. The commercial screen handoff is docs/handoffs/FRONTEND-COMMERCIAL-SETUP.md; invoice/packing entry and applied-default snapshots remain a later slice. Historical absolute paths in older handoffs and logs identify their original execution locations and do not indicate additional active project copies.
