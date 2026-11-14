# Phase 1.1: Master data
**Agent:** backend, frontend | **Branch:** feat/master-data
> Before any task: read `AGENTS.md`, `docs/00-context.md`, `docs/01-field-ownership.md`. One unchecked task per session; tick it and add a one-line result note.

- [ ] Backend: CRUD API `/api/companies` (types EXPORTER, PRODUCER, CONSIGNEE, NOTIFY, FORWARDER, SUPPLIER) with validation (name, country required) and search by name/type. Tests per endpoint.
- [ ] Backend: CRUD API `/api/shipping-lines` with `bl_template_key`, and `/api/products` (species, scientific name, commercial name, HS code, presentation). Tests per endpoint.
- [x] Frontend: list + create/edit screens for companies and products/grades with filter and pagination, implemented against `docs/contracts/commercial-setup-v1.yaml` (organizations, products, grades). The older `/api/companies` + `/api/shipping-lines` plan is superseded; shipping lines have no published contract.
  - Result: company directory (`/partners`), product/grade catalog (`/catalog`), shipment commercial-parties panel, shared country/organization pickers; company role validation, empty-role legacy edit, and mock-service specs. Commercial parties share the project version (refresh on mismatch) and surface per-slot API field errors. Build/prettier clean; 94 tests pass. Headless Firefox smoke against the mock API: admin login, create a SUPPLIER company, create a product, create a grade with `10.000` kg and `12.5000` EUR.
- [ ] Commit `feat: master data`.
