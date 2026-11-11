# Export Documentation Platform

Angular + Spring Boot + PostgreSQL platform that links and generates the export documents of a seafood shipment (invoice, packing list, catch certificates, BL, HC, CO, PS), built with Maestro agents (Codex and OpenCode).

## Start in 6 steps
1. Read `docs/03-maestro-runbook.md` and install Maestro plus Codex and OpenCode.
2. `cp .env.example .env && docker compose up -d db`
3. `git init && git add . && git commit -m "chore: initial scaffold"`
4. In Maestro, create the six agents from `docs/02-agent-roster.md`.
5. Point Auto Run at `.maestro/playbooks` and run `Phase-0-Foundations` in order.
6. Answer anything the agents put in `docs/OPEN-QUESTIONS.md`, then continue with Phase 1.

## Phases
| Folder | Result |
|---|---|
| Phase-0-Foundations | Repo tooling, Spring Boot and Angular skeletons, schema, auth |
| Phase-1-Core-Documents | Master data, project, invoice, packing list, catch certificates, link engine |
| Phase-2-Transport-Regulatory | BL, HC, CO, PS drafts, PDF generation, stamp variants, validation engine |
| Phase-3-Ingestion | Excel/PDF import, OCR per document, review screen, carrier BL import |
| Phase-4-Workflow-Phase2 | State machine, ETD reminders, email, Phase 2 codes, final documents |

Full plan: the "Export Documentation Platform" doc.
