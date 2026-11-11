# Phase 1.1: Master data
**Agent:** backend, frontend | **Branch:** feat/master-data
> Before any task: read `AGENTS.md`, `docs/00-context.md`, `docs/01-field-ownership.md`. One unchecked task per session; tick it and add a one-line result note.

- [ ] Backend: CRUD API `/api/companies` (types EXPORTER, PRODUCER, CONSIGNEE, NOTIFY, FORWARDER, SUPPLIER) with validation (name, country required) and search by name/type. Tests per endpoint.
- [ ] Backend: CRUD API `/api/shipping-lines` with `bl_template_key`, and `/api/products` (species, scientific name, commercial name, HS code, presentation). Tests per endpoint.
- [ ] Frontend: list + create/edit dialog screens for companies, shipping lines, products with filter and pagination. Component test for the company form validation.
- [ ] Commit `feat: master data`.
