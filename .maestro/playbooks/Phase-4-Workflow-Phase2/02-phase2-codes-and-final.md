# Phase 4.2: Phase 2 codes and final documents
**Agent:** backend, docgen, frontend | **Branch:** feat/phase2
> Before any task: read `AGENTS.md`, `docs/00-context.md`, `docs/01-field-ownership.md`. One unchecked task per session; tick it and add a one-line result note.

- [ ] Backend: `POST /api/projects/{id}/phase2/codes` storing health code, CO code, PS code and CS. Only allowed in state AWAITING_AUTHORITY. Test.
- [ ] Docgen: final templates embedding the codes and removing the DRAFT watermark. Test PDF text contains each code.
- [ ] Backend: FINALIZED makes the project read-only; changes require a new revision. Test that edits are rejected.
- [ ] Frontend: Phase 2 screen, final documents download, read-only banner.
- [ ] QA: end-to-end test (Playwright) of the whole timeline from project creation to FINALIZED.
- [ ] Commit `feat: phase 2 and finalization`.
