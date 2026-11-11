# Phase 3.2: PDF text extraction and OCR
**Agent:** ingestion | **Branch:** feat/ingestion-ocr
> Before any task: read `AGENTS.md`, `docs/00-context.md`, `docs/01-field-ownership.md`. One unchecked task per session; tick it and add a one-line result note.

- [ ] Implement text-based PDF extraction with PDFBox for invoice and packing list (zone-based or regex per issuer template). Test on anonymized samples.
- [ ] OCR bake-off: build an `OcrEngine` interface and adapters for Tesseract and one of PaddleOCR/docTR. Evaluate on at least 20 anonymized scans; write field-level accuracy to `docs/OCR-BAKEOFF.md` and recommend one engine. Do not choose silently: add the decision to `docs/DECISIONS.md` for the owner to approve.
- [ ] Implement a scan fallback: if PDF has no text layer, route to the approved OCR engine.
- [ ] Carrier BL import: extract vessel, voyage, BL number, container, seal from a carrier BL PDF or forwarder email text; update `bill_of_lading`.
- [ ] Commit `feat: pdf and ocr extraction`.
