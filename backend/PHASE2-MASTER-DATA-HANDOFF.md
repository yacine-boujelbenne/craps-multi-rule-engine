# Phase 2 backend preparation and delivery

Branch: `feat/backend-phase2-master-data`, based on `b2ae193`.
Worktree: `/run/media/yacine/yassine/DMS/backend-phase2-master-data`.
The owner's explicit Phase 2 worktree instruction takes precedence over the
repository's earlier single-checkout convention. Angular and migrations are
outside this implementation's ownership.

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

The committed target artifacts are `V4__master_data_addresses_factories_and_project_products.sql` and `docs/contracts/master-data-v1.yaml`. Apply V4 only after V1-V3. It is additive; it does not rename/drop tables, rewrite project rows, or transform legacy organization.address_text. Existing API consumers keep their V1/V2 responses. These are ready for Agent 2 to implement against.

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

Existing commercial-parties remains the distinct clientId/consigneeId API. Same company can occupy both roles; if separate, IDs remain separate. V4 does not infer either role from the other. Existing project JSON stays unchanged. Additive `/api/projects/{id}/factory` selects factory and advances the shared project version. `/api/projects/{id}/products` owns shipment product quantities and advances the same project revision (coordinate transactional revision/audit implementation). Project creation remains reference-only and its existing ADMIN permission is unchanged until the owner explicitly changes the access contract; assigned operators edit only their granted forms.

The migration adds transactional audit triggers to all new business tables and the existing immutable row trigger to approval references. `recorded_by` must be set from trusted session identity, never request JSON. Existing document-version snapshot/audit design is reused.

## Acceptance coverage

`MasterDataSchemaIT` covers complete ISO seed, city/country FK, postal code leading zeros, manual city fallback, multi-role/different client-consignee, multiple factories/company, duplicate approval namespace, immutable approval, configurable categories and shipment-specific product quantities. It compiles, but Docker-blocked environments cannot execute its PostgreSQL bodies. Existing 79 unit/MVC sources remain untouched. Run `./backend/verify.sh` in a Docker-enabled host; this adds 5 MasterDataSchemaIT invocations to the verified 200-test baseline (expected total 205).

## Verification so far

`./mvnw -o ... test-compile` with Java 21 runtime and release-21 compiler passed at 2026-11-13 14:34. `docs/contracts/master-data-v1.yaml` parses as OpenAPI YAML (9 paths, 16 schemas after company/product additions), and `git diff --check` passes. V4 has not yet been applied to a database in this work session. Fresh-schema execution and upgrade-preserves-existing-row proof remain required on host PostgreSQL before migration acceptance.

Agent 2 can implement the endpoints described by master-data-v1.yaml using this mapping. Agent 3 can generate types with its normal OpenAPI tool and integrate after the backend route permission semantics are agreed. Keep Angular files outside this database branch. No OCR or document-rendering engine is introduced.
