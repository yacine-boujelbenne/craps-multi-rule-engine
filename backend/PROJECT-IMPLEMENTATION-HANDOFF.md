# Project API implementation and integration report

Branch: `feat/backend-projects`. Worktree: `/run/media/yacine/yassine/DMS/backend-projects`.
Implementation is ready for integration review; **build and PostgreSQL acceptance
remain blocked by the environment**. No completion checkbox is marked.

## Contract and ownership

The implementation uses Agent 1's frozen
[`projects-v1.yaml`](../docs/contracts/projects-v1.yaml) and
[transaction/mapping contract](../docs/contracts/README.md), imported from
`36fe428`, plus foundation `6e57314` and architecture tests `10301da`.
The backend was initially empty; Agent 1 supplied these dependencies during the
task. Only `reference` is mandatory for draft creation, as confirmed by the owner.
`DRAFT_DATA` and PATCH replace the older plan's DRAFT/PUT wording for this slice.

No migration or frontend file was changed by the backend implementation. Agent 1
explicitly authorized replacing its deny-all fallback security class. The only
added Maven dependency is the OAuth2 resource-server starter.

Implementation commit: `ad3e86b`. Agent 1 reviewed the source and verified fixes
for coherent read snapshots and strict UUID/date parsing. That review did not
approve runtime behavior; compilation and integration execution remain open.

## Implemented operations

| Method | Route | Behavior |
| --- | --- | --- |
| POST | `/api/projects` | 201 + Location; normalize unique reference; save draft, one transport extension, parties, creator access and transactional audit |
| GET | `/api/projects/{id}` | 200 complete representation; 404 for missing/invisible projects |
| GET | `/api/projects` | Zero-based page/size (20 default, 100 maximum), optional status and literal case-insensitive reference prefix q, createdAt/id descending |
| PATCH | `/api/projects/{id}` | 200; required expectedVersion; atomic update; 409 for stale version; exactly one version increment for an effective change |

Omitted PATCH properties are unchanged; explicit null clears nullable fields.
`producerIds` replaces the set; [] clears it. Reference may be edited under the
contract; UUID identity, status, timestamps and creator are read-only. Equivalent
and version-only patches create no revision or audit changes. Future lifecycle
states are represented, but no transition command is implemented.

Validation covers reference normalization/format/uniqueness, field lengths,
strict UUID and ISO calendar-date text, ISO country codes, distinct producer IDs,
active company/role membership and active ports. Historical ETD is allowed.
Unknown body fields and query parameters are rejected. PostgreSQL's unique
constraint arbitrates duplicate races. Update row locking prevents overlapping
party replacements from bypassing expectedVersion. Snapshot read transactions
keep version, party, transport, count and page data coherent under concurrent writes.

Errors use application/problem+json with type/title/status/detail/instance and
errors entries containing field/code/message: 400 invalid input, 401 anonymous,
403 role/user rejection, 404 missing/invisible project, 409 duplicate/stale version,
422 invalid reference data, and safe 500 responses. Audit rows come from Agent 1's
database triggers; API writes set transaction-local trusted actor/request values.

## Files changed by this implementation

- `backend/src/main/java/com/exportdocs/project/api/`: controller, presence-aware
  parser, response/page DTOs, MapStruct mapper and error handling.
- `backend/src/main/java/com/exportdocs/project/application/`: service, actor,
  validated input values, write command and domain-facing errors.
- `backend/src/main/java/com/exportdocs/project/domain/`: Project JPA aggregate
  and lifecycle vocabulary.
- `backend/src/main/java/com/exportdocs/project/infrastructure/`: Spring Data
  repository, party/transport adapter, user lookup and JWT security configuration.
- `backend/src/main/java/com/exportdocs/infrastructure/FoundationSecurityConfiguration.java`:
  removed by agreement with Agent 1; project configuration supplies the full filter chain.
- `backend/pom.xml`: resource-server starter.
- `backend/src/test/java/com/exportdocs/project/`: PostgreSQL API integration
  tests, parser unit tests and authorization policy unit tests.
- This report, `docs/OPEN-QUESTIONS.md`, and the project playbook's verification note.

## Verification

| Check | Result |
| --- | --- |
| `python3 infra/scripts/check-contract.py` | **10 passed**; contract checks, not HTTP/persistence tests |
| Isolated `ProjectActorTest` with cached JUnit Jupiter 5.14.2 engine | **5 passed**; compiled with release 21 and ran on IntelliJ's Java 25 runtime |
| Java source parsing with release 21 | Passed syntax check; does not establish type-checking/MapStruct generation or application compilation |
| `./mvnw -Dtest=ProjectRequestParserTest test` | Blocked before compilation: Maven Central DNS failure resolving Spring Boot parent |
| `./mvnw verify` | Same parent-resolution failure; full unit/integration suite did not run |
| PostgreSQL/Testcontainers | Cannot start: Docker API socket permission denied |
| Frontend test gate | No frontend package/build in this backend worktree; delegated to Agent 3 at integration |

The API tests cover successful minimal/full creation, missing fields, invalid
company/role/port references, duplicate references and concurrent inserts,
retrieval, missing resources, updates, null/omission, immutable IDs, no-op/stale
versions, party/transport-only revisions, concurrent updates, pagination/filtering,
permissions, a concurrent read snapshot and transactional rollback on audit failure.
Tests use actual Flyway migrations and PostgreSQL 16; they do not skip missing
Docker or substitute H2. Agent 1's FoundationsIT suite is also included.

The Maven commands used `JAVA_HOME=/snap/intellij-idea/154/jbr`,
`MAVEN_COMMAND=/snap/intellij-idea/154/plugins/maven-plugin/lib/maven3/bin/mvn`
and `-Dmaven.repo.local=/tmp/dms-backend-maven` because Java/Maven are absent from
PATH and only the task workspace/tmp are writable. No Java 21 runtime was found.
No PostgreSQL connection or working HTTP endpoint has been demonstrated here.

## Run and authentication

Use the [local infrastructure instructions](../infra/README.md) with Java 21,
Maven Central access and an accessible Docker daemon. Set POSTGRES_PASSWORD in
the ignored local .env. Configure these variables for the backend process:

```text
SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI=<trusted OIDC issuer>
SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_JWK_SET_URI=<optional explicit JWKS URL>
EXPORTDOCS_SECURITY_AUDIENCE=exportdocs-api
```

The issuer is required for accepting tokens. Without it, health stays public and
all business tokens are rejected. JWT signatures, issuer, timestamps and the
configured audience are verified. Map the verified token's subject to an active
`app_user.external_subject`; assign its database roles through `role_assignment`.
ADMIN/OPERATOR may write; REVIEWER/VIEWER may read. Non-admins require project_access;
creation grants creator access. JWT role claims do not grant database permissions.
User/directory administration and an identity provider are outside this milestone.

After configuring the environment:

```bash
docker compose up -d --wait db
./infra/scripts/backend-local.sh
# In another terminal, run the full acceptance gate:
cd backend
./mvnw verify
```

## API examples and Angular handoff

Examples assume a valid access token and a provisioned application user. They have
not been executed in this environment.

```bash
curl -i http://localhost:8080/api/projects \
  -H "Authorization: Bearer $ACCESS_TOKEN" -H 'Content-Type: application/json' \
  -d '{"reference":"EXP-2026-001","destinationCountryCode":"FR","expectedDepartureDate":"2026-12-01"}'

curl -i "http://localhost:8080/api/projects/$PROJECT_ID" \
  -H "Authorization: Bearer $ACCESS_TOKEN"

curl -i 'http://localhost:8080/api/projects?page=0&size=20&status=DRAFT_DATA&q=EXP-' \
  -H "Authorization: Bearer $ACCESS_TOKEN"

curl -i -X PATCH "http://localhost:8080/api/projects/$PROJECT_ID" \
  -H "Authorization: Bearer $ACCESS_TOKEN" -H 'Content-Type: application/json' \
  -d '{"expectedVersion":0,"notes":"Revised shipment setup","expectedDepartureDate":null}'
```

Agent 3 should generate its typed client from `docs/contracts/projects-v1.yaml`
using the agreed `npm run gen:api` script. Send Authorization: Bearer on requests;
use the Angular development proxy for `/api` to avoid cross-origin configuration.
Read list data from items/totalElements/totalPages. Retain version from each save,
send it as expectedVersion, reload on 409, and map errors by errors[].field.
Preserve explicit null when clearing fields and omit unchanged ones. Directory
selectors need provisioned IDs with the matching organization roles; directory
lookup endpoints have not been introduced. Complete the real PostgreSQL/API and
frontend gates before merging or claiming a working end-to-end shipment flow.
