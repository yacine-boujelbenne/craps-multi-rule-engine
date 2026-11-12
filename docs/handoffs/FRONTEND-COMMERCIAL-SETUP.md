# OpenCode: commercial setup slice 1

Continue Angular work in your isolated frontend branch. Preserve existing local changes and coordinate integration from feat/architecture-phase1; do not reset the frontend checkout. The backend implementation is integrated, compiles/packages and passes 50 unit tests. Host V3/schema and existing PostgreSQL cases now pass. New commercial API acceptance awaits a CSRF test-helper correction and full rerun; contract-driven UI work can proceed now.

Read AGENTS.md, docs/architecture/ACCESS-MODEL.md, docs/architecture/COMMERCIAL-MODEL.md, docs/architecture/QUANTITY-MODEL.md and docs/contracts/commercial-setup-v1.yaml. Use projects-v2.yaml and accounts-access-v1.yaml for existing project/session behavior. Generate typed clients through the agreed frontend generator; do not hand-invent request shapes. The commercial contract has also generated successfully with openapi-typescript 7.13.0.

Implement these screens/forms:

- ADMIN company directory: create/edit organizations, independent role selections, activation and versioned updates. Include SUPPLIER, CLIENT, IMPORTER and CONSIGNEE; one company may hold several roles. Preserve other existing roles.
- ADMIN product/grade catalog: grade label/code, display order, optional default carton net kg, optional price/kg and currency pair. Only ADMIN can edit these defaults or prices. No real example prices are seeded.
- Shipment commercial setup: independent nullable supplier/client/importer/consignee selectors. ADMIN may edit; an OPERATOR needs project membership and PROJECT_SETUP permission. Other permitted viewers see read-only fields. Reference-only incomplete drafts remain valid.

Use these backend resources:

| Resource | Operations |
|---|---|
| /api/organizations | GET list, POST create |
| /api/organizations/{id} | GET, PATCH |
| /api/products | GET list, POST create |
| /api/products/{id} | GET, PATCH |
| /api/products/{productId}/grades | GET list, POST create |
| /api/products/{productId}/grades/{gradeId} | GET, PATCH |
| /api/projects/{id}/commercial-parties | GET, PATCH |

All master-data writes require ADMIN, irrespective of form assignments. Reuse existing session cookies, CSRF and required-password-change handling. Directory/catalog lists default to active-only; includeInactive=true is ADMIN-only. Historical by-ID selections remain readable after deactivation. Do not offer hard deletion. Use documented pagination and literal prefix filters.

PATCH requires expectedVersion. Omitted fields remain unchanged; explicit null clears only nullable fields. Decimal values are JSON strings: carton weight up to three fractional places, price up to four. Preserve exact decimal strings; do not convert them through floating-point arithmetic. A default price and currency must both be present or both absent in the resulting record.

Commercial parties and Project v2 use the same consignee and project version. Refresh if their separately loaded versions differ; after saving one form refresh affected state. Display stale-version conflicts and field errors from application/problem+json. Do not silently overwrite a newer edit or manufacture roles for existing organizations whose role arrays are empty.

Invoice/packing shipment quantity entry, calculations and immutable applied-default snapshots belong to slice 2 and have no published persistence API yet. The confirmed future UI must distinguish variable shipment carton counts/actual net/gross from ADMIN-only grade/box-weight/price defaults. Do not require a loss/yield workflow, equate raw catch with finished product weight, or hardcode the sample shipment quantities.

Validate using the frontend's existing checks and browser flows. Live end-to-end acceptance requires the backend's new host PostgreSQL verification and account bootstrap; do not report those as passed from generated types or mocked UI tests.
