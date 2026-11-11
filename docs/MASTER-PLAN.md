# Export Documentation Platform — master implementation plan
**Version:** 0.4 — 2026-11-11  
**Status:** MVP scope confirmed by project owner on 2026-11-11; new traceability and signature rules confirmed; remaining domain questions explicitly marked **CONFIRM**.  
**Source material:** four handwritten workflow photographs; 25-page example shipment dossier; 1-page Maersk original BL; existing full-context markdown and project scaffold; Lucent design reference; official Maestro documentation.

## 0. Executive brief
Build a simple export-shipment documentation workspace. Exactly one project represents exactly one shipment (not several shipments); operators enter or import information **once** into an authoritative database, progressively create dependent documents, inspect conflicts, generate versioned PDF drafts and complete an external approval/issuance process. Preserve the exact business data and provenance of every final or sent document. **Do not automate, fabricate, or imply official issuance or approval.**

**Success criteria:** A trained operator can create a shipment, complete invoice, packing list and catch/traceability data, review transport data, generate consistent PDF drafts, spot discrepancies, and archive the reviewed final pack without double entry. Realism > visual effects. The initial system should be understandable by two maintainers.

## 1. Evidence from the example dossier
- Pages 1–2: commercial invoice and shipment/product packing information; 1,600 boxes, split into product sizes, with weights and the exporter/importer.
- Page 3: product shelf-life / batch-date table.
- Pages 4–5: carrier Bill of Lading example and continuation; sample also supplied as separate original Maersk BL. **Official carrier BL is imported/reference evidence; our generated BL is only a draft/instructions unless specifically approved by the carrier.**
- Page 6: certificate of origin issued/endorsed by an outside body.
- Pages 7–14: health certificate including several pages of annexes; page 14 contains batch/date details.
- Pages 15–18: traceability/catch-related forms and fishing-vessel tables. **Pages 16–17 specifically show catch-vessel entries and quantities, forming part of the distinct Shipping List / vessel traceability annex.**
- Pages 19–25: further fisheries declarations, processing/transport attestations and annexes. **Page 22 includes the vessel recap/list with catch quantities; pages 16, 17, and 22 are the operator-identified reference pages for the Shipping List, attached among the HC supporting documents.** **Map the actual form titles and issuers with the operator before assigning authoritative document-type names.**
- BL includes a **container identifier, seal identifier, gross weight, vessel, voyage and BL number**. The example original BL contains a stamped/signed original image; NEVER reproduce or synthetically represent a carrier's original stamp/signature as an official issuance.
- The example lists a cargo gross weight of 20,480 kg and product net weight of 19,200 kg. This illustrates why gross weight and net weight must never be equated in validation.
- Example names/IDs/dates are **test reference only**; anonymize all fixture data before putting it in Git or feeding it to AI services.

## 2. Scope and priorities
### MVP (must have)
- Login, roles; company/party directory; create/edit shipment/project.
- Invoice and invoice lines, packing list and lines, catch certificates and vessel quantities (manual forms first).
- Single source of truth with explicit field ownership and provenance; derived totals and discrepancy checks.
- Transport data / carrier BL reference import as manually entered fields at first.
- Generate **Health Certificate (HC), draft Bill of Lading (BL), Catch Certificates (CC), Certificate of Origin (CO), and Shipping List** from verified shared data. All are internal drafts for external signing/authorization; use approved per-destination layouts.
- Two PDF variants: (A) **unsigned print/sign copy**, ready for real signing; (B) **CLIENT PREVIEW — NOT VALID FOR OFFICIAL USE** with an access-controlled pre-scanned signature *illustration/placement* only. No preview may masquerade as issued, signed, or authority-approved.
- Invoice and packing list remain mandatory manual input forms/source records; whether they must also be PDF-generated is **not yet confirmed**. Processing Statement is not among the five confirmed launch outputs: defer pending explicit decision.
- PDF preview/export, stamp variant subject to permission, immutable snapshots and audit trail.
- Project progress timeline, warnings and blockers, form autosave, validation center.
- Sample data and test coverage covering one full shipment workflow.
### After MVP
- Excel/CSV imports; text-based PDF extraction; OCR for scanned pages; per-field review.
- Batch/supplier mapping profiles; multiple carriers/destinations; email sending; scheduled alerts.
- Advanced audit/reporting, external client portal, optional Supabase Realtime, additional languages.
### Explicit exclusions for MVP
- No agent execution or LLM inside the production business platform.
- No microservices, Kubernetes, event bus, dual primary databases, automatic legal certification, or unreviewed OCR-to-official-document data flows.
- No direct external authority submissions unless lawful integration is documented and approved.

## 3. Actors / roles
**ADMIN:** users/roles, master data, templates, stamp permissions, audit access.  
**OPERATOR:** create projects; edit owned data; import and review; generate drafts.  
**REVIEWER:** validate project, approve sending/final pack.  
**VIEWER:** read-only access to assigned projects and approved versions.  
External exporter, producer/supplier, consignee/notify, forwarder, carrier, government health authority and origin issuer are **business parties**, not automatically application users.

## 4. Business objects and cardinality
- `Organization` (business party) can have several roles; avoid enumerating a company as one exclusive type.
- `Project` 1:N `DocumentInstance`, 1:N `ProjectItem`, 1:N `Container`, 1:N `ImportJob`.
- `ProjectItem` 1:N `PackingLine`, potentially 1:N `SupplierLot` and allocations.
- `CatchCertificate` 1:N `CatchVesselEntry`; capture batch/lot linkage and quantities with units, not only a bare vessel list. The separate Shipping List aggregates vessel catches, dates/delays, batch/lot sums and traceability references as an HC dossier annex.
- `Project` 1:N `CatchCertificate`, 1:N `SupplierLot`, 1:N `ApprovalEvent`, 1:N `Issue`, 1:N `AuditEvent`.
- **Confirmed:** `Project` has exactly one `Shipment`; no multi-shipment project in this product scope. A shipment may have multiple containers or catch records if business rules permit.
- One consignee with potentially multiple suppliers is plausible; see questions. Do not hardcode this until confirmed.
- Codes from external authorities should be stored with issuer, issue date, evidence asset and status; do not treat the presence of a string as proof of authorization.

## 5. Canonical data model / database
Use Spring Boot modular monolith, PostgreSQL 16 or newer supported version, Flyway, UUID IDs, `numeric(18,3)` weights, `numeric(18,4)` prices, explicit currency ISO code, `date` for dates and timezone-aware timestamps for events. Store original units and normalized units; don't use floating-point for commercial totals.

Tables grouped by module:
- Identity: `app_user`, `project_access`, `role_assignment`.
- Parties: `organization`, `organization_address`, `organization_role`, `shipping_line`, `port`.
- Shipments: `project`, `project_party`, `product`, `project_item`, `container`, `shipment_transport`, `supplier_lot`, `batch`.
- Commercial: `invoice`, `invoice_line`, `packing_list`, `packing_line`.
- Traceability: `catch_certificate`, `catch_vessel_entry`, `catch_batch_allocation`, `production_batch`, `shelf_life_entry`.
- Regulatory: `regulatory_record` (`kind`, `authority`, `external_ref`, `status`, `evidence_id`, `issued_at`).
- Documents: `document_type`, `document_template_version`, `document_instance` (`project_id`, `type`, `revision`, `variant`, `snapshot_json`, `content_hash`, `artifact_id`, `source_revision`, `issued_at`, `generated_by`, `status`).
- Files & import: `file_asset`, `import_job`, `extracted_field` (`confidence`, `page`, `bounding_box`, `review_status`), `mapping_profile`.
- Rules & history: `validation_issue`, `field_provenance`, `audit_event`, `approval_event`, `notification_event`.

Start with a **small real schema** for vertical slice rather than creating all tables with empty logic. Add through migrations. Use proper FK/unique/check constraints, optimistic concurrency version on project, transactionally saved audit events, soft-archive not silent deletion.

## 6. Field ownership and precedence (provisional)
| Field group | Canonical owner | Inherited consumers | Rule |
|---|---|---|---|
| Project/parties/destination | Project setup | Invoice, PL, BL draft, certificates | Explicit project-level edit; track prior values |
| Product, invoice quantities, unit price/currency | Invoice lines / project items | PL, certificates | Invoice commercial quantity may differ from catch landed quantity; require business mapping |
| Cartons, batch, net/gross weight | Packing lines | BL draft, HC, CO | Sum by SKU/lot/container; distinguish net/gross |
| Catch vessel, caught amount, dates | Catch records | HC, regulatory forms | Sum by certificate and allocation; avoid naive global equality |
| Container ID | `container` shared shipment record | PL, BL, HC | Enter once; ISO 6346 validation where applicable |
| Seal identifier | `container.seal_number`, input from verified transport/packing data | BL draft, HC, Shipping List as appropriate | Important earlier field; check exact consistency across all documents using seal |
| Lock identifier | `container.lock_number`, separate optional-late field | Only templates explicitly requesting lock | **Confirmed distinct from seal**; can arrive very late; do not block earlier drafts unless particular document requires it |
| Carrier, shipping vessel, voyage, port, carrier-issued BL number | `shipment_transport` populated from forwarder/carrier evidence | BL reference, HC/CO | Not authoritative until verified against carrier evidence |
| ETD, ETA | Shipment/transport scheduling | Notifications/forms | ETD is estimated; capture actual on-board separately |
| Government certificate numbers | Official regulatory record + evidence | Final pack | Manual authorized entry, confirmation and audit |
| Generated PDF facts | Immutable revision snapshot | All historical views | Sent/reviewed PDF never silently mutates |

For an edited source field, show impacted documents as `STALE` and require explicit regeneration. Inherited fields show source document + timestamp and are read-only. User edits belong to canonical entity, not arbitrary PDF field override. Every conflict is explicit, logged and reviewable.

## 7. Chronological workflow / flexible dependency graph
**Important:** chronology guides operators, but some inputs can arrive late. The UI should allow saving incomplete work and block only actions that truly require the missing data.

1. New project: reference, exporter, buyer, producer(s), destination, carrier/forwarder if known, estimated shipment dates.
2. Commercial draft and invoice: invoice header, products, currencies/prices; validate arithmetic.
3. Packing list: product allocations, cartons, net/gross weights, batches, container info when available; calculate totals.
4. Traceability: catch certificates, multiple vessels, catch quantities, production/batch/shelf-life mapping.
5. Shipping details from forwarder/carrier: vessel/voyage, ports, container, **seal number** (key identifier), BL number (when actually issued). Record **lock number later** as a separate field, without overwriting the seal.
6. Generate **HC, BL draft, CC, CO, and Shipping List**, subject to per-document prerequisites, with an unambiguous draft status; allow unsigned signing PDFs and separately labeled client previews with simulated/authorized pre-scanned signature placement.
7. External review: export/send **client-preview versions** visually showing where a real signature will go, prominently labeled **CLIENT PREVIEW — NOT VALID FOR OFFICIAL USE**; keep unsigned version as the source for physical/official signing. Record reviewed/sent hashes.
8. External processing: record government approval, dates, official numbers and proof; do **not** digitally counterfeit official seals/signatures. An authorized pre-scanned signature asset is used **only in a visibly invalid client-preview**; no use to represent a final authorized signing.
9. Final documentation pack: attach authentic issued third-party documents, publish approved versions of internally generated documents, archive complete revision history.

**Early invoice rule**: notes say draft invoice >=3 days before ETD. Treat as **configurable alert** until confirmed if it means exactly ETD−3 calendar days or business days, and whether deadline is a hard blocker.

Project states: `DRAFT` → `IN_PROGRESS` → `READY_FOR_DRAFT_REVIEW` → `DRAFT_REVIEWED` → `AWAITING_EXTERNAL_ISSUANCE` → `OFFICIAL_DOCUMENTS_RECEIVED` → `COMPLETE` → `ARCHIVED`. Documents have independent states `NOT_STARTED/IN_PROGRESS/READY/DRAFT/STALE/APPROVED/EXTERNAL_ISSUED`. Never confuse generated company drafts with authority-issued originals.

## 8. Cross-document validation (start deterministic)
On every save and every generation:
- Invoice line amounts = quantities × unit prices; invoice amount = sum lines + agreed adjustments; currency must match.
- Packing list carton count/net/gross = sums of lines, gross >= net, consistent by item/lot/container.
- Cargo gross on BL reference vs packing gross; configurable tolerances with reason/source.
- Catch total = sum vessel entries **for that specific certificate**; allocation cannot exceed caught quantity. Match appropriate processed/exported product by conversion/yield mapping, not assumed equality.
- Product/species/HS code, origin and destination consistent where legitimately required.
- Unique container IDs per shipment, valid ISO code/check digit where applicable. **Seal and lock are distinct identifiers.** Validate each independently and never require their values to match. Lock is late and conditionally required per document.
- Date chronology checks should be by event semantics: catch date <= production/processing <= shipment/on-board where relevant; ETD is an estimate so don't treat it as guaranteed actual departure.
- Reference codes/external evidence verified before marking externally issued.
- Missing required fields by template/type/destination block that document only, not necessarily the entire project.
- Any released/sent generated PDF becomes `STALE` if source revision changes; show what changed, regenerate only on operator action.

Represent each issue as `severity`, `rule`, `fieldPaths`, `observed`, `expected`, `source`, `resolution`, `reviewedBy`. Add unit tests for every rule and integration tests to ensure no partial writes.

## 9. Document handling pipeline
**Manual first**: Angular reactive form → REST DTO → Bean Validation + domain validation → transactional save → provenance/audit → project revision increments → affected generated docs marked stale.  
**Import later**: file upload → private storage → MIME detection/virus checks/size cap → text extraction (native PDF first; POI/CSV for structured files) → OCR fallback if scan → canonical field suggestions with page/source/confidence → human review diff → confirmed canonical save → validation.  
**Generate**: canonical data → template-version-specific render DTO → server-rendered PDF → immutable JSON snapshot/hash/templateVersion/PDF asset → create two separate variants:
1. `UNSIGNED_FOR_SIGNATURE`: **absolutely no signatures, scanned signatures, signature images, handwritten marks, or stamp impressions embedded in the generated output**, even when the source template had them. Preserve the printed signature/endorsement boxes as empty areas ready for legitimate physical signing and stamping; **not official until actually signed/issued**.
2. `CLIENT_PREVIEW`: demonstrative signature placements for **all relevant signing parties/locations**, using authorized illustration assets only; do not embed actual government/carrier endorsement as though authentic. Always with conspicuous watermark **CLIENT PREVIEW — NOT VALID FOR OFFICIAL USE** and preview metadata. Never embed an official authority/carrier signature as a substitute for its actual issuance.
Secure signature image files separately, audit use, avoid allowing an unwatermarked signed-preview download. Whether a company stamp is necessary is a separate approval decision.  
**External PDFs**: attach original document separately, record issuer/status/hash; NEVER alter official PDFs to pretend they were issued by our platform.

Template strategy: HTML/CSS → PDF for self-designed invoice and packing forms (fast iteration); only use a fixed-position renderer such as JasperReports/PDFBox for precise officially permissible draft layouts. Every regulatory/carrier layout requires review of ownership and acceptance. Template versions are pinned per generated document.

## 10. Architecture and API
**Angular SPA** ↔ **Spring Boot REST/OpenAPI modular monolith** ↔ **one PostgreSQL database**; private object storage for uploads/outputs. Local Docker Compose PostgreSQL; optional Supabase managed PostgreSQL and private Storage in deployment. Do not duplicate Postgres into an additional synchronized database.

Spring package conventions: `com.exportdocs.project`, `.party`, `.commercial`, `.traceability`, `.transport`, `.documents`, `.imports`, `.validation`, `.auth`; within modules `api/application/domain/infrastructure` only when size warrants. Keep services small, avoid generic frameworks.

Representative endpoints:
- `POST /api/projects`, `GET /api/projects`, `GET/PATCH /api/projects/{id}`;
- `GET/PUT /api/projects/{id}/invoice`, `GET/PUT /api/projects/{id}/packing-list`;
- `GET/PUT /api/projects/{id}/catch-certificates`, `GET/PUT /api/projects/{id}/transport`;
- `GET /api/projects/{id}/issues`, `GET /api/projects/{id}/documents`;
- `POST /api/projects/{id}/documents/{type}/generate` payload `{variant,expectedProjectVersion}`;
- `POST /api/projects/{id}/imports`, `POST /api/imports/{id}/confirm`, `GET /api/documents/{id}/download`;
- `POST /api/projects/{id}/reviews`, `POST /api/projects/{id}/external-records`.

Contract standard: OpenAPI-generated typed Angular clients; predictable errors using RFC 9457 Problem Details; UUIDs in URLs; cursor/pagination on project lists; authorization at service/data query layer; server-side rate/size limits; timestamps UTC.

## 11. UI/UX — Angular only; Lucent as an inspiration resource
**Reference:** https://uberdesigner.ai/t/lucent — user means this site as a **catalogue/reference for animations and styling ideas**, **not** a chosen template. Evaluate only small useful free animation/style ideas with license checks; do not adopt React or copy the site. Remain **Angular-only**. Use subtle CSS/Angular animations where they increase clarity; avoid scroll-heavy effects on data-entry screens. Colors published on reference: `#FAFAFA` canvas, `#1F2022` ink, `#61A6F6` blue accent, `#A4A4A4` muted; add accessible semantic success/warning/error tokens as needed. Use legal open font fallback such as Inter/System UI (Lucent describes Instrument Sans). Verify licenses before using source/templates/assets; free account prompts may be personal-use only.

**App shell:** compact left navigation (Projects, Companies, Templates, Administration), top search and account, responsive content panel, breadcrumb.  
**Dashboard:** active projects, nearest ETDs, missing documents, issues table.  
**Project page:** high-level shipment status, chronological but editable document stepper, right-side readiness/issues panel.  
**Document editor:** sticky actions Save/Validate/Generate, grouped form sections, clear inherited/owned fields, inline source badges, repeatable line items, keyboard-friendly tables, localized number/date formatting.  
**PDF preview:** split or tabbed editor/preview, version badge, two explicit export buttons: **Unsigned for signing** and **Client preview (sample signature placement)**. Preview has permanent warning/watermark; access to authorized signature image is permission-controlled and logged.  
**Import review:** original scan/PDF on left, extracted fields on right, low-confidence and conflicting values highlighted; confirmation per field.  
**Other:** comparison of revisions, audit timeline, master-data pickers, admin permissions.

No parallax, eye-scanning illustrations, or continuous animation. Optional 120–180ms transitions; respect `prefers-reduced-motion`. Maintain WCAG AA contrast (published blue on white may be too low for small text; use blue for fills/highlights or darken for text). Use Angular Material/CDK with a thin custom CSS-token theme; test mobile but optimize for desktop operations.

## 12. Security, compliance, data governance
- JWT/OIDC auth, server-enforced role and project access, no client-only security decisions.
- Passwords and credentials never checked into repo; secrets injected at runtime; least-privilege storage and DB access.
- Private document/stamp storage; short-lived signed downloads; separate permission for stamps and finalization.
- Filenames/path sanitization, upload scanning, MIME validation, file-size caps, checksum and audit; disallow active file content.
- Encrypt traffic and backups; decide retention/data residency by customer/authority; restricted access to client names and shipping contracts.
- Sample dossier contains real business identifiers and signatories; redact/anonymize before uploading to third-party AI or public Git.
- Audit changes including approvals, stamp use, documents sent, and template versions; keep original third-party files unchanged.
- Verify applicable export/document compliance requirements with actual shipping line and competent authority before production claims.

## 13. Testing and evidence
- Unit tests: totals, allocation, cross-document conflicts, permission rules, revision/stale logic.
- Integration: PostgreSQL/Testcontainers, Flyway, authorization and transactional audit, storage integration.
- PDF golden snapshots: template-specific output with fictional fixtures; verify page numbers and fields, no forged marks.
- End-to-end Playwright: create project → invoice → packing → catch → transport → validate → generate → change source → stale → regenerate → attach external issued document → archive.
- Accessibility: keyboard journey, labels, contrast, reduced motion; performance budgets on 100+ line-item projects.
- OCR benchmark only after collecting labeled, consented/allowed samples: per-field accuracy (not just page OCR text), manual correction time, document-type breakdown.
- Explicit no-go: fails cross-document blocker, missing authorization, unreviewed imports, altered external originals, hardcoded test secrets, or unreviewed mass changes by agents.

## 14. Development through Maestro (NOT part of the production platform)
Official source: https://github.com/RunMaestro/Maestro; documentation https://docs.runmaestro.ai/. Maestro is an agent-command center supporting both Codex and OpenCode, Git worktrees, file-based Auto Run playbooks, and `maestro-cli`. It executes your configured coding agents; it is not a new model, autonomous QA guarantee, or automatic token-cost discount.

### Small agent roster (start with two; expand only after baseline green)
1. **Architect/integrator (Codex):** owns specs, DB migrations, API contract review, final merges; keeps task dependency graph current.
2. **Backend implementer (Codex or OpenCode):** project/business rules/API; isolated feature branch/worktree.
3. **Frontend implementer (OpenCode or Codex):** Angular forms/dashboard and Lucent-inspired theme after OpenAPI stable; isolated worktree.
4. **QA reviewer (separate session; optional):** adversarial tests, lint/security checks; cannot self-approve own feature.

**Most efficient practice:** 1 architect + up to 2 parallel implementers. Only parallelize file-disjoint tasks with stable interfaces; serialize Flyway migrations, shared `AGENTS.md`, OpenAPI schema, and shared design tokens. Adding 10 agents does not automatically reduce latency or tokens.

### Workflow
- Architect creates a small accepted contract/task: clear files, inputs, expected outputs, tests, stop conditions, dependencies.
- Codex/OpenCode agents read **only** the shared `AGENTS.md`, relevant specs and files; do not paste entire dossier each session.
- Maestro uses one Git worktree per active feature; each agent runs task-limited Auto Run markdown checkboxes, ideally one independently verifiable task per fresh context.
- Agent opens PR/commits test evidence. Architect reviews diff, secrets, migration safety; CI runs; merge one at a time.
- QA separately verifies integrated vertical workflow. Regenerate typed client from OpenAPI *after* backend schema is agreed.
- Measure token spend, wall-clock time, regressions per completed task. If parallel overhead outweighs gains, revert to serial execution.

### Correctness and cost safeguards
- **Never mark BLOCKED work `[x]`**. Keep `[ ]`, explain blocker in `docs/OPEN-QUESTIONS.md`, halt affected playbook. A checked task means accepted, tested implementation.
- Never allow an agent to assume silent business rules or assert it has tested a dependency it could not run. `CI configured` ≠ `CI passes`.
- Limit agent context via specs and one feature at a time; smaller capable model for CSS/scaffolding, stronger reasoning for schema/rule changes if available under your plans.
- Shared CI gates before merge; migrations serialized; reuse fixtures; avoid duplicate agents independently rewriting same source files.
- Budget: 2-3 concurrent sessions maximum until actual tokens/cost data demonstrates benefit; review quotas and provider billing.

### On Linux: setup and first commands
```bash
java -version
node --version
npm --version
docker --version
git --version
codex --version
opencode --version
# Install coding CLIs using their official current instructions; authenticate each separately.
# Obtain Maestro from the official releases page, or follow the current source-build instructions.
cd export-docs-platform
git init
git add .
git commit -m "chore: initial export-docs scaffold"
docker compose up -d db
# Add two agents in Maestro with same repository but distinct worktrees.
# First run only the Phase-0 tooling + backend skeleton tasks; do not parallelize schema design.
```
Actual version compatibility and available commands must be rechecked at installation time. Keep `.env` and all real dossier data outside Git.

### Example safe task acceptance contract
**Task:** Create `POST /api/projects` with company-linked identities. **Inputs:** approved API schema & Flyway migrations. **Files owned:** `backend/.../project/**` and tests. **Acceptance:** valid request 201, invalid references 4xx, unauthorized request 403/401 as appropriate, DB rollback atomic, audit recorded, Testcontainers tests passing. **Do not edit:** frontend, migrations owned by architect. **Blocked:** unresolved parties model → leave checkbox open and post decision request.

## 15. Practical build sequence: vertical slices, not all infrastructure at once
| Gate | Deliverables | Exit test | Parallel? |
|---|---|---|---|
| G0: Confirm business definitions | 1-page signed workflow, page-to-document catalog, sample anonymization, field ownership | No blocking unknowns for invoice/PL | No |
| G1: Bootable repo | Spring Boot + Angular + PostgreSQL + migrations + auth placeholder + CI | `verify` green; create DB | Limited |
| G2: First end-to-end project | Create/list project + company master data + Angular shell | UI creates project persisted by API | After shared API |
| G3: Commercial forms | Invoice lines + PL lines + totals | edits persisted; totals reconcile | Backend/frontend parallel against frozen spec |
| G4: Traceability | Catch certificate/vessel/batch allocation | catch arithmetic + mappings tests | Limited |
| G5: Dependencies | Reuse values, provenance, staleness | source changed → stale draft shown | No for core rule engine |
| G6: First PDFs | Invoice+PL draft preview/download, versions | golden tests with fictional data | UI and renderer can parallelize |
| G7: Shipping/regulatory drafts | transport data, BL draft, HC/CO/PS sample templates | blockers + consistency test + draft watermark | Per template if isolated |
| G8: Review/authority evidence | approvals and external original attachments | cannot mark official issued without evidence/rights | Limited |
| G9: Imports | CSV/Excel → PDF text → scanned OCR + review | low-confidence never auto-applied | Per extractor |
| G10: Launch hardening | backups, security, real-user pilot, deployment | acceptance demo and recovery test | Tests/ops parallel |

A small two-person team with AI assistance can work in short milestone gates; **do not promise a fixed schedule before reviewing samples and scope**. Prioritize usable manual end-to-end vertical slice over OCR and decorative UI.

## 16. First guided lesson (human operator + AI assistants)
**Lesson 1 — Domain discovery and first working foundation.**
1. In `docs/OPEN-QUESTIONS.md`, answer the items marked P0 (below). Before code, decide what counts as a project, document, carrier BL draft, and external official issued PDF.
2. Create a fictional shipment fixture (no real IDs) and fill a one-page matrix: owner field, source document, dependent documents, validation rule, external issuer yes/no.
3. Install JDK 21, supported Node LTS, Docker, Git and Codex/OpenCode; verify versions with commands above.
4. Unzip this repository scaffold, read `AGENTS.md`; initialize Git; start PostgreSQL with Docker Compose.
5. Have architect implement only G1 foundation, demonstrate backend health endpoint and Angular root page, and commit working tests.
6. Next task: implement `POST /api/projects` + a single create-project Angular form; manually inspect database row. **This is your first finished vertical slice.**
7. After this gate, start two parallel worktrees (backend commercial lines, frontend form shell) using approved OpenAPI contract; compare cost and wall-clock performance with earlier serial task.

**Concepts to learn while building:** Git branch/worktree and PR, PostgreSQL relational integrity/migration, REST and OpenAPI, Angular reactive forms, server-side validation vs UI validation, immutable generated artifacts and content hashes, deterministic test fixtures. At each gate study the concept, write a tiny exercise, then implement the production slice.

## 17. Unresolved questions / clarification priority
**P0 — block schema and workflow choices:**
1. **RESOLVED:** Exactly one shipment per project. **OPEN:** What precisely is “Trinity” in the notes? How many invoices/suppliers/batches can a single shipment include?
2. Which PDF forms in the 25-page dossier must **your company create**, which must be **imported unchanged from the carrier/authority**, and which may be generated only as drafts? Please identify forms by page numbers.
3. **RESOLVED:** Seal and lock are distinct; seal is important early, lock comes very late. **OPEN:** Who supplies the lock number, and which individual document outputs actually require it?
4. Is the 3-day-before-ETD invoice an **early commercial draft** or an additional invoice type? Is the rule business days/calendar days and warning/blocker?
5. **CONFIRMED:** Catch quantities are **before processing**, approximately **22,000 kg in the example**, and the exported finished product is approximately **19,200 kg net**; the output is classified into **three categories depending on box size**. These are not directly equal. **OPEN:** precise lot-by-lot allocation and conversion/yield rules, tolerances, and grade/box-size definitions. Never hardcode a universal 22,000 kg input or processing yield.

**P1 — before regulatory templates/imports:**
6. What does `CS` stand for? What are official issuer names for HC, CO, PS and any catch/processing codes?
7. Target countries/markets and carriers at launch; which templates are authorized for local PDF drafting?
8. Which 3 document types should be imported first, and can you provide **blank** source files plus redacted filled examples?
9. Should a client sign in, or is draft review handled outside via email/phone? Who can approve company stamp use?
10. FR/EN UI or documents; deployment residency/hosting; number of operators/shipments; retention requirements.

## 18. Reference links (checked on 2026-11-11)
- Official Maestro repository: https://github.com/RunMaestro/Maestro
- Maestro CLI reference: https://github.com/RunMaestro/Maestro/blob/main/docs/cli.md
- Maestro official site: https://runmaestro.ai/
- Lucent template: https://uberdesigner.ai/t/lucent
- Uberdesigner license/pricing: https://uberdesigner.ai/

## 19. Definition of done (entire MVP)
1. No need to re-enter a shared field across forms; user sees canonical owner and provenance.
2. At least one complete fictional shipment can pass all steps; changes invalidate already generated drafts.
3. Cross-document totals are correct, with net/gross and catch/product distinctions preserved.
4. PDFs can be recreated with versioned templates and an immutable audit record; external originals are preserved as originals.
5. Authorization, tests, backups, and role boundaries are demonstrably working.
6. Project remains simple: one SPA, one backend, one relational database, private file storage; Maestro is optional developer tooling, not a production dependency.

## Confirmed owner decisions — 2026-11-11
- Project cardinality: **exactly one shipment per project**.
- Five generated output families: **HC, draft BL, CC, CO, Shipping List**. The system generates documents for legitimate signing and a separately watermarked client preview visually indicating signing placement. Actual signatures/authority issuance remain outside the app unless a legitimate approved signing workflow is implemented later.
- `seal_number` and `lock_number` are **not the same**. Seal is prioritized; lock is supplied very late.
- Every cross-document value must be coherent. **A coherent quantity model may involve processing yield**, so equality of every weight number is not assumed.
- **MVP A:** manual entry first, shared data, coherence validation, then PDF generation; OCR/import follows.
- **Angular only.** Uberdesigner/Lucent is a source of inspiration for selectively usable animations/styles, not an application template.

### Immediate next implementation slice
1. Establish Angular + Spring Boot + local PostgreSQL skeleton and working health endpoint.
2. Define project creation API (`one project / one shipment`) and first Flyway migration.
3. Implement Angular Create Project form and project list.
4. Add test fixture representing the 25-page example dossier, **anonymized**, and first consistency checks.
5. Proceed to invoice / packing / catch manual-entry workflows; PDF outputs only once fields and ownership are approved.


## 19. Confirmed traceability and signature rules — owner answers, 2026-11-11
### 19.1 Input → processing → finished export weight
The landed/caught raw material is the **input to processing**, about **22,000 kg** for the supplied example, whereas the finished exported **net product** is about **19,200 kg**; the exported gross weight in the example is **20,480 kg**. The three product categories refer to **box-size classifications** (verify category labels and quantity per category against the invoice/packing list; sample page 2 appears to show size grades 50–80, 80–100, and 100+). These numbers describe this one sample shipment, **not** a rule for every project.

Proposed data flow: `catch_vessel_entry` (vessel/date/source catch kg) → `raw_material_allocation` (linked to catch certificate, batches) → `processing_batch` (input raw kg, output finished kg, losses, date, yield) → `finished_category_allocation` (three or more configurable box-size categories, carton counts and weight) → `packing_line` / `project_item` → generated HC, CC, CO, BL draft, Shipping List. Allow one-to-many and many-to-one allocations as verified by business rules, but do not use untraceable aggregate adjustments.

**Checkable conservation example:** raw input 22,000 kg → 19,200 kg finished net + 2,800 kg processing loss/by-products/other accounted outputs, giving illustrative yield `19,200 / 22,000 ≈ 87.27%`. This reconciliation is a **sample mathematical illustration**, not a confirmed actual processing-loss breakdown. `gross_export_kg >= finished_net_kg`. Production mass balance must account for all relevant outputs; exact allowed adjustments are still to be confirmed.

### 19.2 Shipping List is a distinct HC annex
Shipping List **is not** the Packing List. The owner identifies pages **16, 17, and 22** of `DOSSIER N°12 (1) (1).pdf` as source examples: fishing vessels, their catch quantities, their dates/delays, and an aggregated list showing the total per vessel and sums of lots. The Shipping List belongs to the supporting annexes of the **Health Certificate (HC)** dossier. Generate it as its own versioned document and let the export package include it with the HC. Reconcile per-vessel totals against linked catch certificates and processing batch allocations. Preserve individual vessel and lot traceability instead of collapsing everything into a single total.

### 19.3 Signature-free printable originals and illustrative client copies
The **ready-for-signature** variant must contain **no signature whatsoever anywhere on any page** and no signature/stamp graphic from scanned source templates. It must retain blank spaces and labels where each party will physically sign. Where scanned forms are used as references, reconstruct a clean template rather than drawing a blank rectangle over existing signed paperwork.

The **client-preview** variant should visualize **all relevant signature locations** with authorized sample/pre-scanned illustration assets only; prominently label every page `CLIENT PREVIEW — NOT SIGNED — NOT VALID FOR OFFICIAL USE`, and mark simulated/illustrative signing content. The system must not impersonate a public official, carrier or another third party, and must not create an apparently officially executed certificate. Authorized company-signature illustrations can be gated by role and audited. For other signatories use clear `SAMPLE / SIGNATURE LOCATION` placeholders unless actual authorization for that use is verified.

### 19.4 Tests derived from these decisions
- Vessel 1 + vessel 2 + vessel N catch mass equals the related catch/lot summary, with no double counting.
- Processing input/output quantities reconcile by batch and category, with losses recorded explicitly; do **not** assert raw = finished net.
- Shipping List and HC annex share the same vessel IDs, catch dates, certificate references and lot sums.
- Seal value remains stable across consuming documents; late lock value is a separate field.
- Every page of the signing-ready HC, draft BL, CC, CO and Shipping List contains zero signature images/stamp artwork; blank endorsement areas remain.
- Each client-preview page has an unambiguous non-official watermark; preview cannot be exported as an issued/signed original.

### 19.5 Remaining points requiring business confirmation
1. Exact three **box-size category names**, per-category carton counts and post-processing weight assignments for the sample and for configurable future shipments.
2. Does 'delay' in vessel pages mean catch date, landing date, trip duration, or something else? Confirm relevant columns directly with the operator.
3. Are 22,000 kg and 19,200 kg exact sample figures or rounded examples? Which losses/byproducts must be entered and whether other sourced catches feed the same batch.
4. For each of the five outputs, list **all signing/endorsement roles**. Which company-owned signature illustrations are legally authorized in preview? For officials use clearly simulated placeholders until independently authorized.
