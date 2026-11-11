# Phase 2.2: HC, CO, PS and PDF variants
**Agent:** backend, docgen | **Branch:** feat/regulatory-docs
> Before any task: read `AGENTS.md`, `docs/00-context.md`, `docs/01-field-ownership.md`. One unchecked task per session; tick it and add a one-line result note.

- [ ] Backend: `health_certificate`, `origin_certificate`, `processing_statement` tables and APIs. Phase 2 code fields exist but are nullable.
- [ ] Docgen: templates for HC, CO, PS, invoice and packing list. Each takes the frozen JSON snapshot of `document_instance.payload`.
- [ ] Docgen: generate each document in two variants, `with_stamp` (stamp image overlay from `company.stamp_asset_id`) and without. Watermark "DRAFT" until Phase 2. Tests: both variants produced, stamp present only in the first (check via PDFBox text/image extraction).
- [ ] Backend: `POST /api/projects/{id}/documents/{type}/generate?stamp=true|false` creating an immutable versioned `document_instance`. Test: regenerating creates version N+1 and never overwrites.
- [ ] Commit `feat: regulatory documents and PDF variants`.

## Clarified scope
- The five required outputs are HC, draft BL, CC, CO and **Shipping List (distinct from Packing List)**. PS is deferred pending confirmation.
- Build clean unsigned forms with NO signature/stamp graphics anywhere, and clearly invalid client previews showing illustrative signature placements.
- Shipping List derives vessel/lot totals and dates from traceability records and is an HC annex (sample dossier pages 16,17,22).
