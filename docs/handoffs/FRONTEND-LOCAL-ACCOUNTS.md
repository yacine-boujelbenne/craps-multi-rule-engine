# OpenCode request: integrate the verified backend with Angular

Owner explicitly requested that OpenCode continue frontend work now (2026-11-12). Resume existing OpenCode session `ses_e343dadeaffe3jKWkxIpbe2WWx`, titled "Building Angular export docs platform UI". Its session directory is `/run/media/yacine/yassine/DMS`; the actual frontend checkout is `/run/media/yacine/yassine/DMS/export-docs-platform-v0.4/export-docs-platform`, branch `feat/frontend-dashboard-projects` (last inspected HEAD 902f410). Inspect status/history before edits and preserve existing work. Do not start a duplicate frontend implementation.

## Stable dependencies

The integrated architect branch is `feat/architecture-phase1`, in `/run/media/yacine/yassine/DMS/architecture-foundations`. Backend Maven verify passed on the host: 17 unit + 77 real PostgreSQL integration tests, no failures/errors/skips. The saved XML reports, applied V1/V2 logs and packaged JAR were independently inspected. See backend/VERIFICATION-RECHECK-2026-11-12.md. No backend/migration edits are needed for this frontend slice.

Read AGENTS.md and the relevant architecture/contract docs. Use these owning files from the integrated branch/worktree if your checkout is behind:

- docs/contracts/projects-v2.yaml (same Project request/response data, session authentication).
- docs/contracts/accounts-access-v1.yaml (auth, admin users, assignments, effective permissions).
- docs/architecture/ACCESS-MODEL.md.
- backend/ACCOUNTS-IMPLEMENTATION-HANDOFF.md (exact endpoint and first-admin setup instructions).
- docs/architecture/COMMERCIAL-MODEL.md (new party facts; future commercial API fields are not implemented).

Coordinate/safely merge newer architecture changes only if required; never reset/overwrite frontend work, applied migrations or another agent's files. Keep your isolated frontend branch and report merge dependencies/conflicts rather than silently changing backend contracts.

## Implement now, frontend ownership only

1. Resume the current Angular standalone/signals/Reactive Forms/Material application and preserve its design/accessibility behavior.
2. Local login, logout, current-user loading and route guards. No public registration. ADMIN creates/manages accounts; first admin comes from the documented explicit backend bootstrap, not a browser setup endpoint.
3. Session/CSRF integration: GET /api/auth/csrf before login; send X-XSRF-TOKEN from XSRF-TOKEN cookie on unsafe requests; use same-origin `/api` proxy and browser JSESSIONID. Refresh CSRF after login/logout. Never store passwords/session tokens in localStorage. Distinguish 401 session expiry and 403 permission denial.
4. When mustChangePassword is true, show password change/logout only; replacement must differ. On successful change, log in again. No email-reset workflow is contracted.
5. ADMIN account list/create/update/disable/password-reset screens using username/displayName/roles/temporary-password and account expectedVersion. Never echo/store returned passwords or invent account deletion endpoints. Handle last-active-admin conflicts.
6. ADMIN shipment creation (reference only required), project list/details and setup editing against real Project API. OPERATOR has no project-create action.
7. ADMIN assignment screen: choose an operator and a project, replace assigned formKeys via the published endpoint. [] grants read-only shipment access; DELETE removes project/form access.
8. Effective permissions from GET /api/projects/{id}/permissions: operator sees every form of assigned shipments, edits only editableForms. PROJECT_SETUP is required for project setup edits. VIEWER/REVIEWER remain read-only. Backend enforces permissions independently. Future form contents may remain clearly identified placeholders; this is not a request to implement uncontracted business forms.
9. Generate/type clients for BOTH OpenAPI files. Existing gen:api currently generates project types only; extend within frontend ownership. Preserve PATCH omitted/null distinctions, expectedVersion, error field paths, status DRAFT_DATA and pagination envelope.
10. Connect the normal integration flow to the real backend. Keep any explicit demo/mock mode clearly separated; do not present localStorage fixtures as persisted backend data. No fabricated directory IDs. Company/port directory endpoints are not yet contracted: reference-only project creation must work without those optional lookups.

## Confirmed commercial facts and scope limits

One shipment has one supplier/factory, one consignee, one client and one importer. Client/importer/consignee can differ; do not require them to be equal or all distinct. One general factory/supplier invoice is billed to the consignee. Trinity is only a tag. Existing project v2 has no dedicated supplierId/clientId/importerId; do not invent fields/endpoints, collapse client/importer into consignee, or silently reinterpret producerIds. The next commercial API/schema will be published separately.

No OCR/imports, PDF/signature rendering, advanced workflow, invoice/packing/catch content APIs or backend controllers are part of this request. The remaining product/processing/traceability questions do not block the auth/admin/project frontend slice.

## Acceptance and delivery

Run frontend unit tests (`npm run test:ci` or the supported non-watch equivalent) and production build. With an available local backend/admin, exercise login -> password change -> admin creates operator -> admin creates project -> assign operator -> operator reads project and allowed/forbidden setup edits -> revoke/deactivate -> access denied. Include actual cookie/header CSRF, expiry/403/404/409 handling and absence of privileged controls for operators. Browser/API integration is a separate gate from mocked HttpClient tests. If host/bootstrap/browser access is unavailable, report that boundary precisely; do not claim the real journey passed.

Return feature commit(s), changed files, generated contract inputs, commands/results, browser evidence or exact blocker, and integration instructions. Leave backend/migrations untouched and do not rerun the already-passing backend suite without a new reason.
