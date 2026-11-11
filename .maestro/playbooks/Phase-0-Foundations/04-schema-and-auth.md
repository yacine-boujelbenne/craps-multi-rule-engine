# Phase 0.4: Core schema and auth
**Agent:** backend (Codex), then frontend (OpenCode) | **Branch:** feat/schema-auth
> Before any task: read `AGENTS.md`, `docs/00-context.md`, `docs/01-field-ownership.md`. One unchecked task per session; tick it and add a one-line result note.

- [ ] Write Flyway `V1__core_schema.sql` creating: `company`, `shipping_line`, `app_user`, `role`, `user_role`, `project`, `project_item`, `supplier_lot`, `audit_log`, `field_value`, `document_instance`, `validation_issue`, `import_job` with primary keys (UUID), foreign keys, `created_at/updated_at`, and indexes on every foreign key. Columns follow the plan doc, section 6. Write a Testcontainers test that migration applies on an empty DB.
- [ ] Create JPA entities and Spring Data repositories for `company`, `project`, `app_user`. Test: save and load a `Project` with exporter and consignee.
- [ ] Implement JWT authentication (Spring Security resource server) with roles ADMIN, OPERATOR, REVIEWER, VIEWER, plus a dev-only user seeder active under profile `dev`. Tests: unauthenticated request returns 401; VIEWER cannot POST (403).
- [ ] Implement an `AuditService` and an `@Audited` aspect writing before/after JSON to `audit_log`. Test: updating a project writes one audit row with the user id.
- [ ] Frontend: login page, auth interceptor attaching the JWT, route guard by role, and a unit test for the guard.
- [ ] Run both test suites and commit `feat: schema, auth, audit`.
