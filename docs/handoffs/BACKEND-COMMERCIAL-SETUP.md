# Backend handoff: commercial setup slice 1

Owner-confirmed business inputs are in docs/architecture/COMMERCIAL-MODEL.md and docs/architecture/QUANTITY-MODEL.md. This is the next small backend task. Implement only after this contract/schema commit is available; keep your own feature branch/worktree. Do not modify frontend or applied V1/V2.

## Slice boundaries and compatibility

**Slice 1 now:** organization directory, product/grade defaults and shipment commercial party selection. `docs/contracts/commercial-setup-v1.yaml` is the authoritative OpenAPI 3.0.3 contract. `V3__commercial_parties_and_admin_grade_defaults.sql` is the architect-owned migration. `CommercialSetupSchemaIT` contains the schema checks.

**Slice 2 later:** one supplier-to-consignee general invoice, direct per-grade carton/net/gross quantities and immutable applied-default snapshots. No invoice/packing content API is published by this slice. Do not independently invent its table/DTO or require loss/by-product accounting. Publish/review that second contract before implementing it.

Instead of changing the tested Project v2 response or creating versioned duplicate controllers, slice 1 adds GET/PATCH `/api/projects/{id}/commercial-parties`. Existing `/api/projects` requests/responses remain unchanged; preserve their tests and the existing local-account/session API. The new subresource stores its consignee in the SAME canonical project_party record used by v2's consigneeId. Do not keep a second consignee or a second project revision. Legacy producerIds and BUYER remain supported; do not silently translate them into the new supplier/client/importer roles.

## Modules and ownership

Agent 2 owns services/controllers/DTOs/adapters/tests under com.exportdocs.party, com.exportdocs.catalog and project integration. Use the established api/application/domain/infrastructure convention as appropriate; avoid generic CRUD frameworks. Reuse session identity, ADMIN checks, Problem Details, presence-aware PATCH and audit context. The architect owns migrations/OpenAPI; request a coordinated follow-up migration for defects rather than editing applied files.

## Canonical mappings

| API data | Storage |
|---|---|
| Organization legalName/countryCode/addressText/registrationNumber/active/version | organization.legal_name/country_code/address_text/registration_number/active/version |
| Organization roles | organization_role; add CLIENT and IMPORTER independently of existing BUYER |
| Product code/name/scientificName/hsCode/active/version | product matching columns |
| Grade code/name/active/version | product_category matching columns; productId from immutable parent path |
| Grade displayOrder | product_category.display_order |
| defaultCartonNetKg | product_category.default_carton_net_kg |
| defaultPricePerKg/defaultCurrency | product_category.default_price_per_kg/default_currency |
| supplierId/clientId/importerId/consigneeId | project_party roles SUPPLIER/CLIENT/IMPORTER/CONSIGNEE |

A company can support several roles. The same organization ID can appear in several shipment slots if it has those roles; do not require either equality or distinctness. At most one company per each of the four commercial roles per project. All four may be absent on an incomplete draft. No default supplier or hidden migration from producerIds is inferred.

V3 adds no tables. It versions organizations/products/grades, adds grade defaults/display order and new role checks, narrows CC/HC logical cardinality, and makes finished_product_quantity.processing_batch_id optional. Existing generic audit triggers already cover organization/roles/product/grade/project-party mutations. Failed uniqueness/cardinality migration must stop rather than deduplicate business data. The read-only `infra/sql/commercial-v3-preflight.sql` checks duplicate (project_id,role) for the four roles, duplicate catch_certificate.project_id, and duplicate (project_id,document_type_code) for HC/CC before deploying to an existing populated database. Run it against the actual V1/V2 database; resolve reported contradictions with the operator before migration. An empty result is diagnostic, not a substitute for V3 enforcement.

## Permissions

- All ready ADMIN/OPERATOR/REVIEWER/VIEWER accounts can read directory/catalog metadata. List endpoints default to active entries; includeInactive=true is ADMIN-only. By-ID reads may return inactive metadata so existing references remain understandable; never select inactive records for new shipment links.
- Every organization/product/grade POST/PATCH is ADMIN-only. A form grant does not grant catalog write privileges. Non-admin valid writes return 403 and leave rows/versions/audit untouched.
- Commercial-party GET follows project visibility (inaccessible/missing -> 404). PATCH requires ADMIN, or active OPERATOR with project membership AND PROJECT_SETUP, like existing setup PATCH. An accessible project without that edit grant -> 403.
- Validate requested organization existence, active state and required role on create/change; invalid references -> 422 with supplierId/clientId/importerId/consigneeId error paths.
- First-admin bootstrap, login/CSRF, must-change-password checks and user-role restrictions remain the existing account implementation. Add no new authentication mode.

## DTO, decimal and validation rules

All fields/types/requiredness are in OpenAPI. Reject unknown body/query properties, repeated query parameters, wrong JSON types, malformed UUIDs and enum ordinals. Responses include every documented nullable field as null when absent. Create/update role arrays are nonempty unique sets; an existing unconfigured organization may return an empty roles array without acquiring any implicit role.

Trim outer whitespace from names/codes. Reject blank required text. Preserve case for product/grade codes (DB uniqueness is case-sensitive). Normalize optional blank address/registration/scientific-name text to null; validate country, HS and currency codes when supplied. Organization registration/name has no invented global uniqueness constraint. Role replacement that removes an in-use project role returns 409/roles/role_in_use; do not cascade away shipment links. Deactivation preserves referenced rows.

**Decimals are JSON strings**, not JSON number tokens. defaultCartonNetKg is positive numeric(18,3), at most 15 integer/3 fractional digits. defaultPricePerKg is nonnegative numeric(18,4), at most 14 integer/4 fractional digits. Reject exponents, negative values, excessive precision and binary floats; use BigDecimal. Responses normalize weights to 3 fractional digits and prices to 4. Do not round user defaults in this slice: reject excess precision before SQL can round numeric typmods. defaultPricePerKg and defaultCurrency must be both set or both null in the RESULTING record after PATCH merge. Clearing one without clearing the other is 400. A missing box/price default is allowed while ADMIN configures master data; no fictitious value or automatic FX conversion is introduced. USD from the example is not a hardcoded universal currency/price.

## PATCH, revision, audit and concurrency

expectedVersion is required and nonnegative. Omitted fields are unchanged; explicit null clears only nullable fields. Null name/code/roles/active/displayOrder is invalid. roles replaces the full set; duplicate roles are invalid. Compare versions before mutation. Version-only/equivalent request with current version is a no-op, with no version/timestamp/audit write. Stale version -> 409/expectedVersion/stale_version. Effective master-data changes increment that entity version once. Organization updated_at advances on effective scalar OR role changes. Grade changes do not increment the parent product version. Master-data changes do not increment all project versions or reprice historical shipments.

For commercial PATCH, lock the project before checking shared project.version and replacing any party rows; recheck access/grant after obtaining the lock. Effective changes increment project.version/updated_at exactly once; no-op unchanged. Preserve the v2 consignee projection. Read aggregate/page/count data in a consistent snapshot, as existing GET/list do. Set transaction-local actor/request audit context on every write and roll back all data/audit on failure.

Both project and commercial responses expose the same revision, but two separate HTTP reads can race. Frontend must use the version on its latest editing response and refresh if its separately loaded project/commercial revisions differ; it must not combine mismatched versions into a save.

## Lists and errors

Zero-based page default 0, size default 20, max 100; reject offsets beyond supported JPA/JDBC range. Optional q is a trimmed literal case-insensitive prefix; escape SQL wildcard characters. Organization role filter uses organization_role; q searches legalName. Product q searches code OR name; grade q searches code OR name within its parent. Filters combine with AND. Ordering: organizations legalName ASC/id ASC, products name ASC/id ASC, grades displayOrder ASC/code ASC/id ASC. Empty page totalPages=0. Do not inflate counts with joins to role arrays.

Use the existing application/problem+json errors[].field/code/message contract: 400 validation/type/pair/precision, 401 authentication, 403 role/form/CSRF, 404 missing/inaccessible, 409 stale/duplicate/in-use role, 422 invalid requested shipment party. DB duplicate grade (product_id,code) ->409/code/duplicate_code; product.code likewise. No endpoint deletes catalog/organization rows; use active=false.

## Required acceptance

- Minimal master records create/read/update with correct null/default/version behavior; unique codes and grade parent mismatch handled correctly.
- Non-admin reads succeed; every non-admin catalog/default mutation returns 403, including with INVOICE/PACKING_LIST assignments.
- Price/box decimal strings round-trip exactly; numeric JSON, bad scales, zero box weight and invalid currency pairs fail without partial/audit writes.
- Role arrays are real role strings, not numbers. Same-company multiple roles allowed; second company for one shipment role rejected.
- Operator commercial edits require PROJECT_SETUP; unassigned projects stay invisible. Same shared project version, stale-write conflict, child-only increment and no-op behavior match v2. v2 consignee and new consigneeId stay identical.
- Role removal in use is 409; no silent link deletion. Current invoice/packing snapshots are not touched by default changes (the concrete snapshot persistence comes in slice 2).
- V1 -> V2 -> V3 migration/validation, new schema tests and existing 94 tests pass. No skipped Docker/H2 substitute. Frontend work remains its owner's responsibility.

## Slice 2 planning guardrails (not an implementation contract yet)

One invoice per project, seller from SUPPLIER, bill-to from CONSIGNEE. Snapshot the applied grade code/name, carton-weight default, price/kg/currency and source versions when a line is created. Operator changes only its form-owned shipment quantities/actual net/gross values; protected snapshot/default overrides are ADMIN-only. Never reprice an existing line when ADMIN later changes a default. User-entered actual net/gross are not silently replaced by a guessed formula. No mandatory processing batch, loss, by-product or yield equation. The later contract must define partial totals/nulls, invoice currency and monetary rounding explicitly before money totals are implemented; none of those calculation endpoints are in this slice.

## Architect verification before publication

17 offline contract checks passed, OpenAPI local references resolved, and all test sources compile for Java 21 (including 12 new CommercialSetupSchemaIT cases). Docker socket access is still denied in the agent sandbox, so V3 and the new PostgreSQL tests are NOT yet executed. The historical 94-test backend baseline applies to V1/V2. Require a new complete host Maven verify for this slice before acceptance. The restarted tool environment no longer exposes the previous cross-thread message tool; this committed handoff is the delivery artifact, not a claim of a successfully sent agent message.

## Implementation started — 2026-11-12

After the owner said “start then,” the old backend branch was preserved and `feat/backend-commercial-setup` was prepared from d2dd11a in `/run/media/yacine/yassine/DMS/commercial-backend`. The existing cross-thread/CLI resume path was unavailable/read-only, so a dedicated backend-role worker was started in this isolated worktree. It is implementing slice 1 acceptance tests/services/controllers; the architect remains responsible for review and integration. OpenCode's frontend worktree is separate and untouched. This note records dispatch, not feature completion.


## Integration status — 2026-11-12

Slice 1 implementation is integrated on feat/architecture-phase1. The worker branch is preserved; no applied migration, Project v2/account contract or frontend source was changed by its implementation. The response-only legacy organization role amendment is included. 50 unit tests and 18 offline contract checks pass, and all main/test sources compile with release 21. Full verify packaged the application but failed at Docker/Testcontainers startup, so V3 and all new PostgreSQL acceptance remain unverified. See ../../backend/COMMERCIAL-SETUP-IMPLEMENTATION-HANDOFF.md for exact commands and test inventory. Earlier preparation/dispatch entries above are historical. Do not mark acceptance complete until the new host verification passes.

Frontend integration instructions are in FRONTEND-COMMERCIAL-SETUP.md. Invoice/packing content and snapshot APIs remain a separately contracted slice 2.
