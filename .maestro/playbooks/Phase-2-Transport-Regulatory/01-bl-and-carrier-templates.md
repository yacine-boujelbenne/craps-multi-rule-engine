# Phase 2.1: BL
**Agent:** backend, docgen | **Branch:** feat/bl
> Before any task: read `AGENTS.md`, `docs/00-context.md`, `docs/01-field-ownership.md`. One unchecked task per session; tick it and add a one-line result note.

- [ ] Backend: `bill_of_lading` table and API. The draft BL is fully assembled from other documents (shipper, consignee, notify, containers, seals, weights, goods description). Only vessel, voyage and BL number are imported from the carrier. Test: draft BL payload built from a fixture project has no manually typed fields.
- [ ] Backend: rule "lock number must exist before BL draft generation", returned as a BLOCKER `validation_issue`. Test.
- [ ] Docgen: PDF generation module (JasperReports or HTML to PDF; pick one, record the choice in `docs/DECISIONS.md`). Create a generic BL template and an interface `BlTemplate` selected by `shipping_line.bl_template_key`.
- [ ] Docgen: first carrier templates (placeholder layouts for Maersk and CMA CGM until real samples arrive; add questions to `docs/OPEN-QUESTIONS.md`).
- [ ] Commit `feat: BL draft`.
