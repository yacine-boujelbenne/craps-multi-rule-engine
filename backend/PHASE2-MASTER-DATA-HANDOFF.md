# Phase 2 backend preparation and delivery

Workspace: `/run/media/yacine/yassine/DMS/export-docs-platform`, using the current shared branch. The owner reconfirmed on 2026-11-14: no copies or additional worktrees. Coordinate branch switches and Git mutations; preserve unrelated work. Agent 1 owns schema/migrations/contracts and acceptance review; Agent 2 owns backend implementation. Angular remains outside this backend task.

## Current Agent 1 → Agent 2 handoff — 2026-11-14

Tasks 3/4 (addresses, factories, approvals and project-factory selection) are accepted on owner-reported focused verification SUCCESS. Task 5 now extends product CRUD metadata/name compatibility and implements taxonomy category list/create/PATCH plus product-assignment GET/PUT. Source review found no blockers; coverage is in existing tests, with a constructor-only UuidPathBindingTest update. Twenty-three offline contract checks and diff-check pass; new Java compilation/runtime verification remains pending the owner command. Grades/defaults/prices remain ADMIN-only; ordinary product/taxonomy metadata permits ADMIN/OPERATOR. Only three new production files were needed; existing Catalog classes are reused. Shipment-product endpoints remain the next separate task, and full Phase 2 is not complete. No automatic Maven/Docker execution.

Agent 1 corrections published in master-data-v1.yaml v1.2.0 and additive V5 (PostgreSQL execution subsequently reported successful by the owner):

- Decimal patterns accept fractional weights/prices at the declared scale and reject malformed values.
- Project-product price/currency fields are explicitly ADMIN-only; reject operator presence, including null or unchanged values. Grade selection copies configured defaults; ordinary measurement edits do not refresh them. V5 adds applied grade version/code/name/carton-default columns; the existing unit_price/currency retain applied prices. Existing rows are not backfilled with today's defaults. See operation descriptions for grade-change and ADMIN override rules.
- V5 adds same-factory, single-successor approval links and a derived effective-validity view. Replace an open-ended approval by appending a successor with a later start; predecessor rows and document snapshots remain immutable. A factory lock and overlap check protect inserts at READ COMMITTED. Select by effective date using the view; reject ambiguity in legacy data. Applied V1–V4 migrations are unchanged.

Agent 2: preserve existing API compatibility and use presence-aware PATCH, canonical UUIDs, exact decimals, transactional audit, fresh role/form checks, project locking and effective-change version increments. Ordinary company/product/taxonomy metadata follows the published ADMIN/OPERATOR access while size grades, box-weight defaults and prices remain ADMIN-only. Changes to contracts/migrations go back to Agent 1. Address/factory behavior, versions, inactive references, ordering and live UTC approval selection are defined in the current contract; product APIs follow a later bounded task and review.

Acceptance requires targeted validation of changed behavior, permission/tampering cases, stale/no-op revisions, audit rollback and existing client compatibility. Agent 1 must verify V4 on fresh PostgreSQL and as a V1–V3 upgrade before accepting the migration. Follow the owner's minimal-execution rules in AGENTS.md: no new test files without explicit request, no full suite for isolated changes, and no unexecuted-check claims. The existing full backend gate remains necessary before accepting the integrated Phase 2 slice. Report changed files, actual checks and blockers concisely.

## Existing implementation to extend

| Capability | Existing owner | Reuse |
| --- | --- | --- |
| Company directory, roles, name search, pagination | `party/Organization*`, `/api/organizations` | Existing `organization` identity and multi-role rows; no duplicate company table |
| Product definitions | `catalog/Catalog*`, `/api/products` | Existing code, scientific name, HS code, activation and version |
| Configurable box-size grades | `/api/products/{id}/grades`, `product_category` | Existing grade defaults and per-product identity; no fixed category count |
| Client/consignee/supplier/importer | `project/CommercialParties*` | Separate nullable role links; canonical consignee shared with project setup |
| Exporter/notify/forwarder/carrier | `project/Project*`, `project_party` | Existing company-role validation and stable organization UUIDs |
| Authentication and form access | `account/*`, `ProjectActorProvider` | Local sessions, cookie/header CSRF, ready-account checks, fresh permissions |
| Transactions and revisions | `ProjectService`, directory/catalog services | Presence-aware PATCH, stale version conflicts, effective-change/no-op behavior |
| Audit | `ProjectDetailsStore.auditContext`, database triggers | Actor/request context in the same transaction; rollback on failure |
| Reference selection locking | `OrganizationSelectionStore` | Serialize selection against deactivation/role removal |
| Errors and JSON boundaries | `ProjectErrorHandler`, `SetupJson`, UUID binding | Consistent field errors, strict types/unknown fields and UUID validation |

No existing document renderer or document-data service is present in the
current Java source. Document tables already exist. New factory and product
resolution must feed canonical source data without adding a PDF engine or
mutating historical document snapshots.

## Published database contract and mapping

Target artifacts are V4, additive `V5__approval_supersession_and_applied_grade_defaults.sql` and `docs/contracts/master-data-v1.yaml` v1.2.0. Apply them after V1–V3; preserve existing rows and legacy organization.address_text. Existing API consumers keep their responses plus additive metadata.

| API/resource | Storage and rule |
| --- | --- |
| Company | Extend existing `organization`; roles stay in `organization_role`. V4 adds trade name, tax ID, email and phone. Roles include CARRIER while retaining SHIPPING_LINE. |
| Addresses | `organization_address` owns country, optional city/manual city, both address lines and text postal code. Exactly one of city_id/manual_city is populated; city country must match address country. Multiple addresses per company; one may be linked to each factory. `address_text` remains untouched for old records. |
| Country/city | `country` is seeded with all 249 ISO 3166-1 alpha-2 codes in the build host's iso-codes dataset. Updates are additive Flyway data migrations; do not delete codes referenced by addresses/projects. `city` is country-scoped and searchable; manual city is valid fallback. |
| Factory | `factory.company_id` references organization; each company can own many factories. Address FK enforces address belongs to the same company. `project_factory` is one optional factory per project and enforces that the factory's company is that project's PRODUCER in `project_party`. |
| Approval | `factory_approval` captures issuer, numbering system, country, reference and recorded dates. Namespace uniqueness is preserved. V5 appends same-factory successors without modifying old rows; `factory_approval_effective` derives the inclusive effective end. Serialize inserts at READ COMMITTED; reject overlap/branching and select by the document's effective date. Ambiguous legacy history returns a conflict; do not choose arbitrarily. |
| Product | Existing `product` gains description, processing method, storage requirements and default unit. Existing `product_category` remains configurable size/packing grade. New `product_taxonomy_category` plus `product_taxonomy_assignment` model reusable product classifications without conflating them with size grades. |
| Shipment products | `project_product` links one product and optional existing product_category to a shipment. It holds nullable progressive carton/quantity/net/gross/price/currency/lot/process data. Weights and prices use exact numerics. These are source shipment facts, never reusable product defaults. |
| Documents | Existing append-only `document_version.snapshot_json` is the historical record. Document assembly stores legal/trade name, chosen structured address fields, factory approval ID/reference/authority/country/validity, product/classification/grade labels and source IDs in that version snapshot. No update to organization/factory/product changes existing document versions. |

Existing commercial-parties remains the distinct clientId/consigneeId API. Same company can occupy both roles; if separate, IDs remain separate. V4 does not infer either role from the other. Existing project JSON stays unchanged. v1.1 adds company/product CRUD metadata, structured address PATCH, taxonomy-category CRUD and product-category assignment replacement. Existing `product_category` remains the selectable packing grade; taxonomy does not replace it.

Additive `/api/projects/{id}/factory` has GET for the selected factory and PUT for replacement. `/api/projects/{id}/products` POST/GET and `/{projectProductId}` PATCH own shipment product details. Every project relationship mutation sends `expectedProjectVersion`, returns `projectVersion`, locks the project, rechecks assignment permission, and increments project.version once for an effective change. Product PATCH also requires its own expectedVersion; productId is immutable and not resent. No-op changes do not increment. CLIENT and CONSIGNEE are never substituted.

Per the Phase 2 request, company/address/factory/approval/product/category master-data writes permit ADMIN and OPERATOR; VIEWER/REVIEWER are read-only. Shipment factory selection uses PROJECT_SETUP and shipment quantity edits use PACKING_LIST assignment; ADMIN can edit. Existing project creation remains reference-only and preserves its current permission contract.

The migration adds transactional audit triggers to all new business tables and the existing immutable row trigger to approval references. `recorded_by` must be set from trusted session identity, never request JSON. Existing document-version snapshot/audit design is reused.

## Acceptance coverage

`MasterDataSchemaIT` covers complete ISO seed, city/country FK, postal code leading zeros, manual city fallback, multi-role/different client-consignee, multiple factories/company, duplicate approval namespace, immutable approval, configurable categories and shipment-specific product quantities. It compiles, but Docker-blocked environments cannot execute its PostgreSQL bodies. Existing 79 unit/MVC sources remain untouched. Run `./backend/verify.sh` in a Docker-enabled host; this adds 5 MasterDataSchemaIT invocations to the verified 200-test baseline (expected total 205).

## Verification so far

`./mvnw -o ... test-compile` with Java 21 runtime and release-21 compiler passed at 2026-11-13 14:34. `docs/contracts/master-data-v1.yaml` parses as OpenAPI YAML (9 paths, 16 schemas after company/product additions), and `git diff --check` passes. V4 has not yet been applied to a database in this work session. Fresh-schema execution and upgrade-preserves-existing-row proof remain required on host PostgreSQL before migration acceptance.

Agent 2 can implement against master-data-v1.yaml v1.1.0 using this mapping. Agent 3 can generate types with its normal OpenAPI tool. The contract's x-authorized-roles and role notes specify access; don't silently narrow operator master-data mutation to admin-only. Keep Angular files outside this database branch. No OCR or document-rendering engine is introduced.
