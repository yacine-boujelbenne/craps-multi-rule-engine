# Phase 3.1: Excel, CSV, Word import
**Agent:** ingestion (OpenCode) | **Branch:** feat/ingestion-excel
> Before any task: read `AGENTS.md`, `docs/00-context.md`, `docs/01-field-ownership.md`. One unchecked task per session; tick it and add a one-line result note.

- [ ] Define the `DocumentExtractor` interface (`supports(type, file)`, `extract(file) -> ExtractionResult` with per-field confidence) and a registry. Add POI, Commons CSV and Tika dependencies.
- [ ] Implement `ExcelPackingListExtractor` driven by a saved `mapping profile` (column mapping per supplier, stored in DB). Test with an anonymized sample workbook in `backend/src/test/resources/samples`.
- [ ] Implement `ExcelInvoiceExtractor` the same way and CSV variants.
- [ ] API: `POST /api/projects/{id}/imports` (multipart), `GET /api/imports/{jobId}`, `POST /api/imports/{jobId}/confirm`. Confirm writes domain rows and `field_value` (source EXCEL). Tests.
- [ ] Frontend: import button on each document form, mapping profile editor, and the review screen (original vs extracted, low-confidence highlighted).
- [ ] Commit `feat: excel/csv import`.
