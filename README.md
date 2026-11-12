# Export Documentation Platform

Angular + Spring Boot + PostgreSQL platform that links and generates the export documents of a seafood shipment (invoice, packing list, catch certificates, BL, HC, CO, PS), built with Maestro agents (Codex and OpenCode).

## Workspace and local development

The single project checkout is `/run/media/yacine/yassine/DMS/export-docs-platform`. Backend, frontend, documentation and infrastructure live together here; use Git branches instead of duplicate project folders. Read [docs/WORKSPACE.md](docs/WORKSPACE.md) for integration status and [docs/architecture/README.md](docs/architecture/README.md) for the data model.

```bash
# From the project root, create .env from .env.example only if it does not exist,
# then configure its local PostgreSQL password.
docker compose up -d --wait db
./infra/scripts/backend-local.sh
```

In another terminal:

```bash
cd frontend
npm start
```

Backend verification: `./backend/verify.sh` with working Docker. Frontend checks: run the frontend package's test/build scripts. See [infra/README.md](infra/README.md) and [backend/VERIFYING.md](backend/VERIFYING.md) for prerequisites; account bootstrap remains explicit. Existing uncommitted frontend changes are preserved and still belong to the frontend agent.

## Phases
| Folder | Result |
|---|---|
| Phase-0-Foundations | Repo tooling, Spring Boot and Angular skeletons, schema, auth |
| Phase-1-Core-Documents | Master data, project, invoice, packing list, catch certificates, link engine |
| Phase-2-Transport-Regulatory | BL, HC, CO, PS drafts, PDF generation, stamp variants, validation engine |
| Phase-3-Ingestion | Excel/PDF import, OCR per document, review screen, carrier BL import |
| Phase-4-Workflow-Phase2 | State machine, ETD reminders, email, Phase 2 codes, final documents |

Full plan: the "Export Documentation Platform" doc.
