# Phase 2.3: Validation engine
**Agent:** backend, qa, frontend | **Branch:** feat/validation
> Before any task: read `AGENTS.md`, `docs/00-context.md`, `docs/01-field-ownership.md`. One unchecked task per session; tick it and add a one-line result note.

- [ ] Backend: `ValidationRule` interface and runner producing `validation_issue` (BLOCKER, WARNING, INFO). Implement every rule in `docs/01-field-ownership.md`, one class per rule, each with a unit test (pass case and fail case).
- [ ] Backend: run the validators on save and before every generation; BLOCKER issues stop generation. Integration test.
- [ ] QA: build a fixture set of three projects (clean, with errors, Trinity multi-supplier) in `backend/src/test/resources/fixtures` and a golden-file test comparing generated PDF text.
- [ ] Frontend: validation center screen listing issues with links to the offending field.
- [ ] Commit `feat: validation engine`.
