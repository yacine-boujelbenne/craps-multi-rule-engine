# Phase 1.4: Link engine and field provenance
**Agent:** backend (architect reviews) | **Branch:** feat/link-engine
> Before any task: read `AGENTS.md`, `docs/00-context.md`, `docs/01-field-ownership.md`. One unchecked task per session; tick it and add a one-line result note.

- [ ] Define a `FieldMapping` registry in `application.linking` from `docs/01-field-ownership.md`: target field key, source field key or aggregate (SUM, COPY), owner document. Test: every row in the ownership table has a mapping or an explicit `MANUAL` flag.
- [ ] Implement `LinkService.recompute(projectId)` that fills inherited fields and writes `field_value` rows with `source_type` DERIVED and `source_document`. Tests: changing a packing line net weight updates dependent fields; a manual override is kept and logged.
- [ ] Implement the `outdated` flag: when a source changes after a `document_instance` was generated or sent, mark that instance OUTDATED. Test the transition.
- [ ] Expose `GET /api/projects/{id}/fields` returning each field with value, source and override status.
- [ ] Frontend: shared `inherited-field` component (read-only, source badge, "override" action with reason dialog). Component tests.
- [ ] Commit `feat: link engine`.
