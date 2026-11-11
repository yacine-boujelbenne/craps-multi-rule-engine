# Owner-confirmed accounts and form assignments

The owner confirmed: the platform has its own accounts; an administrator creates/manages users and assigns operators to shipment projects and particular forms. An assigned operator can read every form in that shipment and edit only assigned forms. A project remains exactly one shipment.

## Permission rules

| Action | ADMIN | OPERATOR |
|---|---|---|
| Create/manage accounts and reset temporary passwords | Allowed | Denied |
| Create a shipment project | Allowed | Denied in the updated contract |
| Assign/revoke project membership or form editing | Allowed | Denied |
| Read a shipment and all its forms | All projects | Assigned projects only |
| Edit project setup fields | Allowed | Requires PROJECT_SETUP assignment |
| Save a form's owned fields | Allowed | Requires both project membership and that form's edit assignment |
| Edit inherited fields owned by another form | Follow canonical ownership | Requires assignment to the canonical owning form |

REVIEWER/VIEWER remain read-only on assigned projects; review/issuance actions require their future workflow contracts. A form grant does not grant user administration, approval, official issuance or permission to change another form's fields. Angular disables/hides editing controls, and the backend independently checks every write. Return 404 for an inaccessible shipment, 403 for an unassigned form within an accessible shipment.

Example: assign operator A to shipment DEMO-001 and PACKING_LIST. A reads the full shipment, edits packing data only, and cannot change invoice, catches, HC-owned fields or project setup. Another form grant adds that specific editing permission. An empty form list keeps the shipment read-only. Revoking project membership removes its form grants in the same transaction. Deactivation immediately prevents authenticated use, even if an old session exists.

Initial form keys: PROJECT_SETUP, INVOICE, PACKING_LIST, CATCH_CERTIFICATES, PROCESSING, TRANSPORT, HEALTH_CERTIFICATE, BILL_OF_LADING, CERTIFICATE_OF_ORIGIN, SHIPPING_LIST. These identify source/editor sections, independently of the five generated document types. The initial grant applies to the named section within one project; no universal access or cross-project grant is implied. Processing Statement is not added as an output.

## Minimal technical design

Use Spring Security local username/password authentication and a server session, keeping one backend and one PostgreSQL. Administrators choose a unique username (an email-like identifier is allowed), display name, roles and temporary password. No public registration, external login provider, automatic email service or reset-by-email workflow is introduced. First administration is bootstrapped by an explicit one-time command/environment workflow; no default password, anonymous setup endpoint or secret in Git. Refuse removal/deactivation of the last active administrator under concurrent updates.

Use Spring's [PasswordEncoder](https://docs.spring.io/spring-security/reference/6.5/servlet/authentication/passwords/password-encoder.html) through a delegating encoder with bcrypt. Require 12–64 characters and at most 72 UTF-8 bytes; do not trim passwords. Store only hashes. Temporary/reset passwords must be replaced with a different password before accessing shipment/admin endpoints; csrf/me/password/logout remain available. Password/username changes increment credential_version; validate that version and active state for each authenticated session so old sessions become invalid. Role/form changes are read from the database, not cached as lasting grants in the browser/session. The session principal carries the immutable app_user UUID.

The backend uses standard JSESSIONID, HttpOnly, SameSite=Lax, Secure under HTTPS and a configurable idle timeout (initially 30 minutes). For this single-instance MVP, server memory holds sessions; restart requires login again. No additional session database/service is necessary. Follow Spring's [explicit security-context/session handling](https://docs.spring.io/spring-security/reference/6.5/servlet/authentication/session-management.html): rotate session ID at login and save context through SecurityContextRepository. Logout invalidates it. Return JSON Problem Details rather than login-page redirects.

Enable [CSRF protection for the Angular SPA](https://docs.spring.io/spring-security/reference/6.5/servlet/exploits/csrf.html). GET /api/auth/csrf materializes the XSRF-TOKEN cookie; unsafe requests, including login/logout, require X-XSRF-TOKEN. Refresh the token after login/logout. Use Spring's SPA-compatible CSRF request handling so cookie and header values are interpreted consistently. Angular talks through the same origin or development proxy. Session identifiers are not placed in localStorage.

## Database changes: V2, without editing V1

- app_user.external_subject becomes optional; existing external mappings remain intact. app_user.version supports optimistic account management.
- local_account holds user_id, unique lowercase username, password_hash, must_change_password, credential_version and timestamps. Keeping credentials separate prevents the existing full-row app_user audit trigger from recording hashes.
- project_form_assignment holds project_id, user_id, form_key, assigned_by and assigned_at. Composite FK (project_id,user_id) -> project_access ensures no edit grant exists without matching read membership. Removing membership requires deleting grants first in the same transaction.
- A dedicated local_account audit trigger removes password_hash before writing history and records only that a password changed. Form assignments use existing transactional audit. Permission/account changes do not increment shipment source revision or stale its PDFs; account changes increment their own version.

The schema now has **26 tables**: the existing 24 plus these two. No grants are inferred/backfilled for existing operators. Existing membership remains read access. Services must verify the target is an active OPERATOR before granting form editing. No generated-document metadata is reused as an edit-permission table.

## Stable handoff and implementation status

- docs/contracts/accounts-access-v1.yaml: login/logout/current-user/password, admin user management, admin assignment management and effective permissions.
- docs/contracts/projects-v2.yaml: same project data shapes, session authentication, ADMIN-only creation and PROJECT_SETUP-gated updates. projects-v1.yaml stays preserved as the previous contract; v2 is the target for this changed access requirement.
- V2__local_accounts_and_form_assignments.sql: additive database support.
- AccountAssignmentsIT: schema tests for local identity, credential audit redaction, unique usernames, membership-bound edit grants and no permission-driven shipment revision changes.

Agent 2 owns authentication, account/assignment services and controllers, password handling, privilege/session revocation, migration/service tests and replacement of the previous JWT-only security. The frontend agent owns login/account/assignment interfaces and typed clients. Publish/cherry-pick this schema/contract before dependent implementation. Form endpoints must check the same grants when those workflows are implemented.

The migration and tests are prepared but have not executed: Maven DNS and Docker permissions remain blocked. The currently integrated backend is still JWT-based and lacks local-account/admin-assignment endpoints; the schema/contract alone does not enforce the new policy. Do not describe the new account/form workflow as working until Agent 2 implements it and the PostgreSQL/API tests pass.

## Follow-up source review — 2026-11-12

Reviewed Agent 2's local-account implementation read-only in feat/backend-local-accounts. Session context persistence/rotation, SPA CSRF handling, current database roles, project-bound form grants and last-administrator serialization were inspected. Found that disabling/re-enabling could revive an unused old session; Agent 2 now increments credential_version on active-state changes and added a regression. The change was verified in source. Also requested strict string enum handling, bounded UTF-8 current-password matching and a genuine replacement of temporary credentials. Full compilation, browser behavior and PostgreSQL tests remain unverified; the implementation is not yet integrated here.
