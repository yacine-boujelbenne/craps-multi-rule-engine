# Shared API contracts

**Commercial setup slice 1:** `commercial-setup-v1.yaml` adds organization/product/grade directories and `/api/projects/{id}/commercial-parties` without changing Project v2 JSON. V3 supplies the supporting schema and confirmed single-CC/HC rules. Business endpoint implementation is integrated on feat/architecture-phase1; 72 unit/MVC and 18 contract checks pass, and host V3/schema plus existing PostgreSQL tests now pass. The CSRF test fixture is corrected; new commercial API acceptance awaits a full host rerun. See `../../backend/COMMERCIAL-SETUP-IMPLEMENTATION-HANDOFF.md` and `../handoffs/FRONTEND-COMMERCIAL-SETUP.md`. The subsequent invoice/packing snapshot contract is a separate slice.

**Access update:** the owner now requires internal admin-managed accounts and per-form editing. `projects-v2.yaml` and `accounts-access-v1.yaml` are the stable target contracts for that change; Agent 2's implementation is integrated in source; the host Maven verify run passed all 94 backend tests; frontend/browser acceptance remains separate. See `../architecture/ACCESS-MODEL.md`. The existing v1 artifact is preserved for compatibility/history.

`projects-v1.yaml` is the previous OpenAPI 3.0.3 input for Agent 2 and Angular client generation. Agent 2's implementation of these endpoints is integrated on feat/architecture-phase1; its source and tests have been reviewed but the host Maven verify run passed all 94 backend tests; frontend/browser acceptance remains separate. Reference-only creation was confirmed by the owner on 2026-11-11. Initial status is `DRAFT_DATA`, aligning with Agent 2's explicit assignment; this replaces the older plan's `DRAFT` spelling. The other status names follow the master plan. Status transitions are deferred.

## Integration ownership

- Architect: this contract, Flyway migrations, Maven/bootstrap/health, architecture docs.
- Agent 2: `com.exportdocs.project/{api,application,domain,infrastructure}`, party/port lookups, DTO validation, MapStruct mappings, services, repositories, business controllers, authentication/access checks and their tests. Do not duplicate migrations.
- Angular agent: frontend only; generate typed clients from this YAML with the agreed `npm run gen:api` script. This foundation does not change frontend files.
- Contract changes require a coordinated version update before dependent edits. V1/V2 are applied and preserved; V3 is reserved for commercial setup. Never edit an applied migration; coordinate the next available version.

## DTO and storage mapping

| JSON | Canonical table/column |
|---|---|
| reference, status, version, destinationCountryCode, expectedDepartureDate, notes | project.reference/status/version/destination_country_code/expected_departure_date/notes |
| exporterId | project_party, role EXPORTER |
| producerIds | project_party, role PRODUCER (array replacement) |
| consigneeId | project_party, role CONSIGNEE |
| notifyPartyId | project_party, role NOTIFY_PARTY |
| freightForwarderId | project_party, role FREIGHT_FORWARDER |
| shippingLineId | project_party, role SHIPPING_LINE |
| portOfLoadingId, portOfDischargeId | shipment_transport.port_of_loading_id/port_of_discharge_id |

All party IDs reference `organization.id`. An organization may have several `organization_role` rows. Match the assigned role against those rows. `project_party` also uses a composite FK to enforce that membership. No company is intrinsically an exclusive exporter or consignee. The owner has now confirmed one supplier/factory, one client, one importer, one consignee and one general factory invoice per completed shipment; client/importer/consignee can differ and Trinity is just a tag. This newly confirmed commercial model is recorded in `../architecture/COMMERCIAL-MODEL.md`; the dedicated role fields are exposed by commercial-setup-v1.yaml and V3 defines the cardinality constraints; its database execution remains pending. The existing v2 shapes are unchanged.

## Create transaction

1. For the current v2 contract, require authenticated ADMIN for creation (operator setup edits require PROJECT_SETUP); obtain the user ID from the trusted principal, never the JSON body.
2. Trim and uppercase `reference` with Locale.ROOT, then validate the documented ASCII pattern and length. Reject unknown fields. Only reference is mandatory; do not require parties, dates, ports, containers, seals, or locks to save a draft.
3. Resolve any supplied active organizations/roles and ports; report missing/incompatible references as 422 Problem Details with field errors.
4. Insert project (`DRAFT_DATA`, version 0, created_by current user), optional project_party rows and exactly one shipment_transport row (PK = project ID, shared shipment identity).
5. Grant creator project_access. Database audit triggers capture every business mutation in the same transaction. Set transaction-local `app.actor_id` to the principal UUID and `app.request_id` to the request correlation value using `set_config(..., true)` through the transaction's JDBC connection. A missing actor is recorded as SYSTEM for migrations/maintenance; production API must always set it.
6. Return 201 + Location and the full response; optional scalar fields are null, producerIds is []. Roll back all rows/audit on any failure. Unique reference index handles concurrent collisions; return 409, not 500.

## Patch, concurrency and lists

Omitted JSON property means unchanged. Null clears only a nullable property. Null reference/producerIds/expectedVersion is invalid; [] clears all producers. Require expectedVersion and reject stale writes (409). Use JPA `@Version` or atomic UPDATE ... WHERE version = expectedVersion. Increment project.version exactly once per effective transaction, including party/transport changes; set project.updated_at. Reference uniqueness includes archived rows. No-op with current version returns unchanged data, without mutation/audit. Every writer in future modules must follow this revision rule, so snapshots can be marked stale by comparing source_revision.

Do not map a plain nullable record directly to PATCH: it loses the difference between omitted and null. Use a presence-aware command parsed from an ObjectNode or equivalent; Bean Validation still validates supplied values. Reject status, id, createdBy and arbitrary JSON properties in writes.

Page numbers are zero-based; size defaults to 20, max 100. Optional status and literal reference-prefix `q` combine with AND. Order is fixed createdAt DESC then id DESC. Authorization filters precede pagination/count. Page response has items, page, size, totalElements, totalPages (0 when empty). Malformed input -> 400, invalid foreign reference -> 422, invisible/missing project -> 404, conflicts -> 409. Use RFC 9457 application/problem+json with errors[].field/code/message; never SQL or stack traces.

## Deliberate scope limits

Directory CRUD and seed users are not implemented here. Frontend needs authorized directory lookups or fictional development fixtures from Agent 2; never embed real customer data. The integrated v2 business API requires local session authentication and an active application account. Administrators bootstrap/create accounts and assign project/form access. No external JWT issuer is required. Follow `../../backend/ACCOUNTS-IMPLEMENTATION-HANDOFF.md`; v1 remains the historical bearer-auth contract. It does not implement workflow transitions, document generation, imports, or OCR.
