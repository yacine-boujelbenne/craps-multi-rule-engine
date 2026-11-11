# Local accounts and assigned form editing

Branch: `feat/backend-local-accounts`, worktree `/run/media/yacine/yassine/DMS/backend-projects`.
The original `feat/backend-projects` delivery is preserved. This follow-up implements
Agent 1's owner-confirmed account/access contract from `e46d6b2` (imported as
`84b3fcc`). **Runtime acceptance remains blocked; no successful application build,
PostgreSQL connection or browser workflow is claimed.**

Implementation commit: `dc058f9`. Agent 1's password wording/source-review update
`8e930eb` is imported as `87045d3`.

## Implemented contract

Authoritative schemas: `docs/contracts/accounts-access-v1.yaml` and
`docs/contracts/projects-v2.yaml`. Permission rules:
`docs/architecture/ACCESS-MODEL.md`. The architect supplied V2; no V1/V2 migration
or frontend file was edited by this implementation.

| Method | Route | Behavior |
| --- | --- | --- |
| GET | `/api/auth/csrf` | Issue XSRF-TOKEN cookie; 204 |
| POST | `/api/auth/login` | Local username/password login, session rotation and explicit security-context persistence |
| POST | `/api/auth/logout` | Invalidate session, clear cookies; 204, idempotent with valid CSRF |
| GET | `/api/auth/me` | Current active account and fresh database roles |
| POST | `/api/auth/password` | Verify current password, change password, invalidate all old sessions; 204 |
| POST, GET | `/api/admin/users` | Admin-only account creation and paginated listing |
| GET, PATCH | `/api/admin/users/{id}` | Admin-only metadata retrieval and presence-aware versioned updates |
| POST | `/api/admin/users/{id}/password` | Admin temporary-password reset; version check and session revocation |
| GET | `/api/admin/projects/{projectId}/operators` | Admin operator-membership and form-grant listing |
| PUT, DELETE | `/api/admin/projects/{projectId}/operators/{userId}` | Admin idempotent assignment replacement or atomic grant/membership revocation |
| GET | `/api/projects/{id}/permissions` | Project-wide read permission and effective editable-form set |

Existing project data shapes remain unchanged. Project creation now requires ADMIN.
An assigned OPERATOR can read every form in its shipment and PATCH project setup
only with PROJECT_SETUP. Other form grants never confer setup/admin permissions.
Future form-write endpoints must call the same assignment policy for their named
form; those business endpoints are not implemented in this slice. VIEWER/REVIEWER
remain read-only. Missing/invisible project is 404; accessible unassigned form is
403. Form/account changes do not increment project revision.

## Security behavior

- Local account passwords use Spring's delegating PasswordEncoder with bcrypt.
  Enforce 12–64 characters and at most 72 UTF-8 bytes, without trimming passwords.
  Username alone is trimmed/lowercased. No public registration or email reset.
- Session principal contains immutable user UUID and credential_version, never a
  password. Session cookies are HttpOnly/SameSite=Lax with a 30-minute idle timeout;
  deploy over HTTPS and set `SERVER_SERVLET_SESSION_COOKIE_SECURE=true`.
- CSRF is enabled on all unsafe operations, including login/logout. The cookie is
  readable for Angular; the raw header uses Spring's SPA-compatible CSRF handler.
  Session context is explicitly persisted and session ID rotates on login, following
  [Spring's session guidance](https://docs.spring.io/spring-security/reference/6.5/servlet/authentication/session-management.html)
  and [SPA CSRF guidance](https://docs.spring.io/spring-security/reference/6.5/servlet/exploits/csrf.html).
- Temporary accounts can only use csrf/me/password/logout until their password is
  changed. Every authenticated API request rechecks active state and credential
  version; password resets, username changes and active-state changes durably revoke
  old sessions. Re-enabling a user does not revive an old session. Roles and form
  grants are checked against PostgreSQL, not retained as permanent session authority.
- A changed password must differ from the current password; reusing a temporary
  password leaves the password-change requirement and session unchanged.
- Account updates use expectedVersion and increment app_user.version once per
  effective change. A transaction advisory lock serializes admin mutation checks,
  preventing concurrent removal/deactivation of the last active local administrator.
- V2's redacted audit trigger is reused. Password hashes are isolated in
  local_account, excluded from audit JSON and never returned in response DTOs.
  Credential request objects redact their toString values. Business errors use the
  existing structured Problem Details handler; Bean Validation exposes field errors
  without echoing rejected passwords.

## First administrator: explicit one-time bootstrap

Prepare PostgreSQL using `infra/README.md`, export its credentials to the backend
process, and configure Java 21/Maven. Set these environment variables privately
(use a secret manager or an interactive secret input; never put passwords in Git
or command-line arguments):

```text
BOOTSTRAP_ADMIN_USERNAME=<chosen username>
BOOTSTRAP_ADMIN_DISPLAY_NAME=<display name>
BOOTSTRAP_ADMIN_PASSWORD=<temporary password meeting the password policy>
```

Then run from `backend/`:

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=bootstrap-admin
```

The dedicated profile starts without a web server, applies Flyway, creates one
temporary-password ADMIN transactionally, closes the context and exits. It refuses
to run when an active local administrator exists. Unset the bootstrap variables
afterwards, then start from `backend/` with `../infra/scripts/backend-local.sh`
(or use `./mvnw spring-boot:run` with exported
database variables). First login requires changing the temporary password and
logging in again. No external issuer/JWT configuration is required.

## Frontend integration

Generate clients from accounts-access-v1.yaml and projects-v2.yaml. Use the same
origin or Angular dev proxy. Do not store credentials/session tokens in localStorage.

1. GET `/api/auth/csrf` before login, then send X-XSRF-TOKEN equal to the XSRF-TOKEN
   cookie on each POST/PATCH/PUT/DELETE; the browser sends JSESSIONID.
2. POST `/api/auth/login` with `{"username":"operator.name","password":"<entered password>"}`.
   GET csrf again after login/logout because the old token is cleared.
3. If mustChangePassword is true, show only the password-change screen and logout.
   POST currentPassword/newPassword; then log in again.
4. ADMIN creates accounts using username/displayName/roles/initialPassword. Users
   receive temporary credentials through an administrator-controlled channel.
5. Assign via PUT `/api/admin/projects/{projectId}/operators/{userId}` with
   `{"formKeys":["PACKING_LIST"]}`. [] grants project-wide read-only access;
   DELETE removes both form grants and read membership.
6. GET `/api/projects/{id}/permissions`; show all forms but enable only editableForms.
   This UI behavior does not replace server-side checks. Account PATCH/reset use
   account version; shipment PATCH uses its separate project version.

Directory master-data endpoints, non-setup form content, notifications and document
generation remain outside this milestone. The frontend has not been changed here.

## Files and verification

Added `com.exportdocs.account/{api,application,infrastructure}`, account/password/
assignment DTOs and services, session/CSRF handling, non-web admin bootstrap and
its profile, AccountApiIT and PasswordPolicyTest. Updated project authorization,
actor lookup, field-error handling, strict UUID path conversion and existing project
integration fixtures. Removed the JWT filter configuration and resource-server
dependency. The shared architecture migrations and schema contracts were imported
unchanged from Agent 1.

- **13 offline contract checks passed** using `python3 infra/scripts/check-contract.py`.
- **8 isolated JUnit tests passed** with cached Jupiter 5.14.2: 5 actor policy tests
  and 3 password policy tests. Compiled with release 21, ran on bundled Java 25.
- Java release-21 syntax parsing and `git diff --check` passed. Parsing is not full
  Java type checking, MapStruct generation or application compilation.
- `./mvnw verify` was attempted with IntelliJ's bundled Java/Maven and an allowed
  `/tmp` Maven cache. It failed before compilation resolving the Spring Boot parent
  because Maven Central DNS is unavailable. PostgreSQL/Testcontainers additionally
  remains blocked by Docker socket permissions. A Java 21 runtime is still needed
  for the supported runtime acceptance gate.

Prepared real PostgreSQL/MockMvc tests cover CSRF login/logout, session rotation,
temporary-password restrictions, password change/reset, username normalization,
hash/audit redaction, duplicate users, account versions/no-ops, stale-session
revocation including deactivate/reactivate, concurrent last-admin protection,
assignment/read/edit separation, assignment revocation/idempotence, fresh role
checks, and all original project API cases under the new local-session contract.
No Docker tests are skipped or replaced with H2. Run the complete `./mvnw verify`
and frontend gate in an enabled environment before accepting/merging this slice.

Agent 1 performed read-only source review. Findings about durable deactivation
revocation, enum ordinal coercion, bcrypt byte limits and different replacement
passwords were corrected and checked again in source. This is not runtime approval.
