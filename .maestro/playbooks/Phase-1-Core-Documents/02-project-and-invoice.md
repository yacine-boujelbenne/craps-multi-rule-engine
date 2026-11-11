# Phase 1.2: Project and invoice
**Agent:** backend, frontend | **Branch:** feat/project-invoice
> Before any task: read `AGENTS.md`, `docs/00-context.md`, `docs/01-field-ownership.md`. One unchecked task per session; tick it and add a one-line result note.

- [ ] Backend: `POST/GET/PUT /api/projects` (exporter, producer, consignee, notify, forwarder, shipping line, ETD, ports, incoterm). Generate a human reference `EXP-YYYY-NNNN`. Tests: reference sequence, notify may differ from consignee.
  - BLOCKED verification (2026-11-11): Agent 2 implemented POST/GET/PATCH using Agent 1's `docs/contracts/projects-v1.yaml` (reference-only drafts, DRAFT_DATA) and PostgreSQL integration tests on `feat/backend-projects`. Maven Central DNS and Docker socket access prevent build/test acceptance; keep unchecked. See `backend/PROJECT-IMPLEMENTATION-HANDOFF.md`.
- [ ] Backend: `invoice` + `invoice_line` tables (Flyway V2) and `PUT /api/projects/{id}/invoice`. Totals computed server-side. Tests: total = sum(qty x unit price); negative quantity rejected.
- [ ] Backend: support several `supplier_lot` rows per project (Trinity model) and expose them in the project payload. Test: two suppliers, one consignee.
- [ ] Frontend: project creation wizard (steps: parties, transport, ETD) and the invoice form with line editor and live totals. Component test for the totals.
- [ ] Frontend: project workspace stepper showing the chronological timeline (Setup, Invoice, Packing list, Catch certificates, Transport/BL, HC, CO, PS, Review). Steps not yet available are disabled.
- [ ] Commit `feat: project and invoice`.
