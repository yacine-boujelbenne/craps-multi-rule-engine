# Phase 2 backend preparation and delivery

Workspace: `/run/media/yacine/yassine/DMS/export-docs-platform`, using the current shared branch. The owner reconfirmed on 2026-11-14: no copies or additional worktrees. Coordinate branch switches and Git mutations; preserve unrelated work. Agent 1 owns schema/migrations/contracts and acceptance review; Agent 2 owns backend implementation. Angular remains outside this backend task.

## Current Agent 1 → Agent 2 handoff — 2026-11-14

V1–V3 backend behavior is implemented; the saved host result is 200 passing tests, not a fresh run today. V4, `MasterDataSchemaIT` and `master-data-v1.yaml` v1.1.0 are present, but Phase 2 services/controllers are absent from the current checkout. Start with company/contact/address and country/city APIs, reusing the existing party/catalog/account/project infrastructure. Continue with factories, taxonomy and shipment links against resolved contracts. Do not restart project/account/commercial setup or add OCR/PDF generation.

Agent 1 must resolve these contract gaps before the affected operations are implemented:

- Decimal regexes in `master-data-v1.yaml` are over-escaped: the published net-weight pattern rejects `12.500`. Correct decimal schemas and validate representative values.
- Project-product POST/PATCH expose `unitPrice` and `currency` to assigned operators without explicit field restrictions. Preserve the confirmed ADMIN-only price/default policy; define how applied grade/price values are retained and which fields operators may select or edit. Do not invent snapshot persistence or permit a price override.
- Immutable `factory_approval` rows plus the one-open-ended-row index prevent replacing an existing open-ended approval. Define a version/supersession policy and effective-date selection; never update an immutable row to close it or use a correction migration as a routine operator workflow. Any migration change must account for whether V4 has already been applied.

Agent 2: preserve existing API compatibility and use presence-aware PATCH, canonical UUIDs, exact decimals, transactional audit, fresh role/form checks, project locking and effective-change version increments. Extend role handling for CARRIER; ordinary company/product/taxonomy metadata follows the published ADMIN/OPERATOR access while size grades, box-weight defaults and prices remain ADMIN-only. Changes to contracts/migrations go back to Agent 1. Unaffected operations can proceed while the gaps above remain open.

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

The committed target artifacts are `V4__master_data_addresses_factories_and_project_products.sql` and `docs/contracts/master-data-v1.yaml` v1.1.0. Apply V4 only after V1-V3. It is additive; it does not rename/drop tables, rewrite project rows, or transform legacy organization.address_text. Existing API consumers keep their V1/V2 responses. Implement unaffected operations against these artifacts; the review gaps above remain open for affected operations.

| API/resource | Storage and rule |
| --- | --- |
| Company | Extend existing `organization`; roles stay in `organization_role`. V4 adds trade name, tax ID, email and phone. Roles include CARRIER while retaining SHIPPING_LINE. |
| Addresses | `organization_address` owns country, optional city/manual city, both address lines and text postal code. Exactly one of city_id/manual_city is populated; city country must match address country. Multiple addresses per company; one may be linked to each factory. `address_text` remains untouched for old records. |
| Country/city | `country` is seeded with all 249 ISO 3166-1 alpha-2 codes in the build host's iso-codes dataset. Updates are additive Flyway data migrations; do not delete codes referenced by addresses/projects. `city` is country-scoped and searchable; manual city is valid fallback. |
| Factory | `factory.company_id` references organization; each company can own many factories. Address FK enforces address belongs to the same company. `project_factory` is one optional factory per project and enforces that the factory's company is that project's PRODUCER in `project_party`. |
| Approval | `factory_approval` references factory and captures issuer, numbering system, issuer country, reference and optional validity dates. Duplicate number is prevented within issuer + numbering system + country across all factories. Only one open-ended (valid_until NULL) reference per factory. Rows are append-only; correct/reissue by inserting a successor with the prior end date captured on insert. Never UPDATE/DELETE. Select the record valid on the document's effective date; if validity dates overlap, fail selection and require an explicit correction migration/record policy instead of guessing. |
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
