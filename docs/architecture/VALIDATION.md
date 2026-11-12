# Validation and delivery

**New commercial slice status (2026-11-12):** V3 and commercial-setup-v1.yaml are prepared; the 12 new PostgreSQL schema cases compile but have not executed because Docker access is denied in this sandbox. All 17 offline contract checks pass. The verified 94-test result below is the V1/V2 baseline and is not evidence that V3 or the unimplemented commercial endpoints have passed.

**Backend verification passed on the host: 94 tests, zero failures/errors/skips.** Independently inspected the saved Surefire XML (17 unit tests), Failsafe XML/summary (77 PostgreSQL integration tests), Flyway V1/V2 application logs and packaged Spring Boot JAR. The host Maven run finished BUILD SUCCESS on 2026-11-12 at 00:48:37+01:00. See `../../backend/VERIFICATION-RECHECK-2026-11-12.md`. Earlier sandbox failures below are historical; frontend/browser acceptance, first-admin bootstrap and deployment are still separate pending checks. No playbook completion boxes were changed.

## Environment and executed checks

| Command/check | Result |
|---|---|
| Read AGENTS.md, MASTER-PLAN.md, context, ownership, agent roster/runbook, open questions; inspect repository | Existing documentation scaffold identified before implementation. |
| git init / status / log / switch / worktree add | Isolated feat/architecture-phase1 at `/run/media/yacine/yassine/DMS/architecture-foundations`. See concurrency note below. |
| java -version / command -v mvn | Neither on initial PATH. |
| /snap/intellij-idea/154/jbr/bin/java -version | JBR/OpenJDK 25.0.4.1 found; Java 21 runtime not available/verified. |
| JAVA_HOME=/snap/intellij-idea/154/jbr /snap/intellij-idea/154/plugins/maven-plugin/lib/maven3/bin/mvn -version | Maven 3.9.16 available. |
| python3 infra/scripts/check-contract.py | **PASS: 10 offline contract tests.** Includes reference-only draft, PATCH null semantics, malformed inputs, explicit optional response fields and status vocabulary. This is not full OpenAPI-tool validation or endpoint execution. |
| XML ElementTree parse backend/pom.xml; PyYAML parse Compose, application YAML and OpenAPI | **PASS.** Syntax only, no dependency resolution. |
| POSTGRES_PASSWORD=validation-only docker compose config --quiet | **PASS.** Placeholder is supplied only to validate configuration; no real credential persisted. |
| bash -n infra/scripts/backend-local.sh; sh -n backend/mvnw | **PASS.** Shell syntax only. |
| git diff --check | **PASS.** |
| POSTGRES_PASSWORD=validation-only docker compose up -d --wait db | **BLOCKED:** permission denied accessing unix:///var/run/docker.sock. No database started. |
| curl -I --max-time 15 https://repo.maven.apache.org/maven2/ | **BLOCKED:** DNS resolution failed. |
| JAVA_HOME=/snap/intellij-idea/154/jbr MAVEN_COMMAND=/snap/intellij-idea/154/plugins/maven-plugin/lib/maven3/bin/mvn ./mvnw -Dmaven.repo.local=/tmp/exportdocs-m2 -o verify (in backend) | **BLOCKED:** Spring Boot parent 3.5.16 is not in the offline cache. |
| timeout 35s env JAVA_HOME=/snap/intellij-idea/154/jbr MAVEN_COMMAND=/snap/intellij-idea/154/plugins/maven-plugin/lib/maven3/bin/mvn ./mvnw -Dmaven.repo.local=/tmp/exportdocs-m2 -Dmaven.wagon.http.retryHandler.count=0 verify (in backend) | **BLOCKED:** Maven Central DNS failure; command exited 1 at parent resolution, before compilation/tests. |
| Frontend npm tests | Not run: frontend implementation belongs to its agent; this foundation has no frontend source changes. |

No dependencies were downloaded, system packages installed, permissions changed, or privileged Docker action performed. Maven scratch repository stays under /tmp. No H2 replacement or skip-Docker test gate was used.

## Prepared database integration tests (16; not executed)

`backend/src/test/java/com/exportdocs/infrastructure/FoundationsIT.java` uses real PostgreSQL 16 through Testcontainers, Spring Boot, Flyway and transactionally rolled-back fictional fixtures. Failsafe runs `*IT` under `verify`; unavailable Docker causes failure.

Coverage: startup migration validation and second-run no-op; reference-only/default status/version and duplicate reference; invalid statuses; canonical reference casing; shared transport identity; separate catch/finished balance and independent aggregation; cross-project allocation rejection; catch overallocation; prevention of lowering already allocated raw kg; gross/net ordering; role-qualified parties; independent seal/late lock; audit attribution and rollback; immutable snapshots and revision-derived staleness; immutable audit rows; public health/protected business routes.

Concurrency of competing allocation transactions, full OpenAPI generator validation, all future service authorization rules, external artifact storage, document rendering and production migration privileges are not verified by this suite. Agent 2 supplies project endpoints and transactional concurrency tests. Source mass-balance business approvals remain open in DECISIONS.md.

## Reproduce the remaining acceptance checks

On a host with JDK 21, Maven, working DNS/registry access and Docker socket access:

```bash
cp .env.example .env
# Set a local password in .env.
docker compose up -d --wait db
./infra/scripts/backend-local.sh
# In a second terminal:
curl --fail http://127.0.0.1:8080/actuator/health/readiness
cd backend
./mvnw verify
```

Record successful readiness, installed Flyway version and Failsafe reports before declaring the milestone complete. The contract/foundation are available to downstream agents now; runtime acceptance is not claimed.

## File manifest

Created:

- `backend/pom.xml`
- `backend/mvnw` (installed-Maven launcher; not a downloading wrapper)
- `backend/src/main/java/com/exportdocs/ExportDocsApplication.java`
- `backend/src/main/java/com/exportdocs/infrastructure/FoundationSecurityConfiguration.java`
- `backend/src/main/resources/application.yml`
- `backend/src/main/resources/db/migration/V1__architecture_foundations.sql`
- `backend/src/test/java/com/exportdocs/infrastructure/FoundationsIT.java`
- `backend/src/test/resources/sql/foundation-fixture.sql`
- `docs/contracts/projects-v1.yaml`
- `docs/contracts/README.md`
- `docs/contracts/examples/project-draft.json`
- `docs/architecture/README.md`
- `docs/architecture/DECISIONS.md`
- `docs/architecture/VALIDATION.md`
- `infra/README.md`
- `infra/scripts/backend-local.sh`
- `infra/scripts/check-contract.py`

Modified within infrastructure ownership: root `docker-compose.yml` (health, loopback bind, explicit password) and `.env.example` (environment configuration). Existing shared plans, AGENTS.md, playbooks and Angular source contents were preserved.

## Parallel-session Git note and integration

Another session initialized Git and staged Angular files between the initial inspection and this session's Git initialization command. Baseline commit `12c6126` inadvertently recorded those staged frontend scaffold files unchanged. Their contents were not edited or removed. The original checkout is restored to `feat/frontend-dashboard-projects`; architecture changes live only in the separate worktree.

To integrate without importing that unrelated baseline commit, cherry-pick architecture deliverable commits individually, rather than merging its baseline history:

1. `36fe428` — frozen Project OpenAPI and integration semantics.
2. `6e57314` — V1 database schema, Maven/bootstrap/configuration and local Compose/run helper.
3. The subsequent `test: document and verify architecture contracts` commit — tests, examples, architecture/runbook/evidence.

Agent 2 has already cherry-picked the first two commits and owns business API/security implementation. Agent 2 may replace FoundationSecurityConfiguration with its real security configuration, avoiding ambiguous normal-configuration bean ordering. No other agent should edit V1 or silently diverge from the published contract. Reserve the next migration version through the architect before implementing further schema changes.

The frontend agent consumes `docs/contracts/projects-v1.yaml` and its documented null/update/list semantics. The initial request is only `{"reference":"DEMO-001"}`; response status is DRAFT_DATA, optional scalar fields null and producerIds []. Directory lookup endpoints remain an Agent 2 dependency, not an invented frontend schema.

## Integrated Agent 2 delivery

Agent 2's commits ad3e86b and 26c504b were cherry-picked without conflicts as 5765d59 and 949cae3. They add the four Project endpoints, JPA/MapStruct/presence-aware validation, transactional service, role/project access, JWT resource-server security, unit/API tests and the implementation handoff. The agreed security configuration replaces the original foundation fallback class. See `../../backend/PROJECT-IMPLEMENTATION-HANDOFF.md` for the additional file inventory and exact API/authentication instructions. Existing source-review corrections are in INTEGRATION-REVIEW.md; its formerly stale backend report has now been rewritten and inspected.

On the combined tree, the 10 offline contract tests, Compose configuration, git diff --check and the check for no frontend content changes passed. Agent 2 separately reports five isolated ProjectActorTest cases passed with cached JUnit and Java source syntax parsing at release 21; these are not evidence of a Spring build or database integration. No new success is claimed for Maven, Flyway execution, PostgreSQL startup or readiness. All runtime blockers above remain.

The architecture branch now contains the integrated backend; consumers can base a new integration branch on feat/architecture-phase1. If integrating only into an existing backend branch, avoid reapplying its already-cherry-picked foundation/API commits. Root frontend content remains unchanged relative to the preserved baseline; no original shared checkout branch was merged or overwritten.

## Owner-confirmed account/form access update

Added V2__local_accounts_and_form_assignments.sql, AccountAssignmentsIT (six PostgreSQL schema tests), ACCESS-MODEL.md, accounts-access-v1.yaml and projects-v2.yaml; updated contract index, decisions, open questions and the offline checker. V1 is unchanged. The target schema contains 26 tables: local_account and project_form_assignment extend the previous 24. Credentials use a dedicated redacted audit trigger; project membership remains read access and per-form grants add editing. Local-account login/enforcement services are still pending Agent 2 implementation; the currently integrated code remains JWT-based.

**13 offline contract tests passed** after this change. The six new schema tests have not compiled/executed. Docker/Maven blockers remain; no migration success is claimed. This extends the earlier manifest; previous check counts describe the historical slice.

## Integrated local accounts and form permissions — 2026-11-12

Imported Agent 2 dc058f9/f78e83e as 52f1e3a/2ec067d without conflicts. The integrated source now supports local admin-managed accounts, session login/logout/change-password, explicit headless first-admin bootstrap, CSRF and admin-managed operator/form assignments. Project creation is ADMIN-only; operator setup edits require PROJECT_SETUP. Other form-content endpoints/UI are not delivered by this slice. Runtime acceptance remains open.

On the combined tree, 13 offline contract checks, Compose/configuration parsing and git diff checks pass; migration and frontend content checks show no integration changes. Agent 2 reports 8 isolated cached-JUnit tests passed and release-21 Java syntax parsing; this is not a Spring build. The 14 AccountApiIT cases, updated ProjectApiIT and PostgreSQL schema suites remain unexecuted because Maven parent resolution and Docker access are blocked. The supported Java 21 runtime gate is also outstanding. Current file/API/auth/bootstrap inventory is `../../backend/ACCOUNTS-IMPLEMENTATION-HANDOFF.md`. Older JWT/pending implementation notes above describe historical stages and are superseded by this entry.

## Successful compile/unit recheck — 2026-11-12

Imported Agent 2 verification report 25e8579 after inspecting its commands and the integrated worktree's generated MapStruct sources/main and test class files. Confirmed Java 21.0.13.0.3 and Maven 3.9.12 are now on PATH. The recheck was run against unchanged integrated c446e58: all 37 main and 7 test sources compile with release 21; 17 compiled JUnit tests passed on Java 21 (9 parser, 5 actor, 3 password-policy tests), using the cached Jupiter engine directly. The 13 offline contract checks also pass. No source changes were needed.

This supersedes earlier uncompiled/Java-21-unavailable statements. Standard Maven verify remains blocked by incomplete Surefire/jar-plugin metadata and DNS; Docker socket access remains denied. The installed Java 21 is a runtime without javac, so compilation used the bundled IntelliJ compiler with --release 21. No PostgreSQL/Flyway/HTTP integration acceptance is claimed. Exact command and temporary-runner location are preserved in `../../backend/VERIFICATION-RECHECK-2026-11-12.md`.

## Backend runtime verification passed — 2026-11-12

Imported Agent 2 report f9b95fa. Independently parsed all seven saved TEST-*.xml suites in the integrated worktree: Surefire 17 tests and Failsafe 77 tests, each with zero failures/errors/skips. Failsafe summary also records zero flakes. Counts are PasswordPolicyTest 3, ProjectRequestParserTest 9, ProjectActorTest 5, AccountApiIT 14, AccountAssignmentsIT 6, FoundationsIT 16 and ProjectApiIT 41.

Saved integration output confirms real PostgreSQL 16.15 containers started, Flyway validated/applied both V1 and V2, and Spring Boot/MockMvc initialized. `backend/target/exportdocs-backend-0.1.0-SNAPSHOT.jar` exists. The owner ran the complete Maven verify command from the normal host terminal with the configured compiler; this supersedes earlier incomplete-plugin/DNS/Docker-blocked backend acceptance status. Sandbox tools remaining restricted do not negate the verified host result. No source changes or redundant test reruns were needed.

Current acceptance: backend schema, migrations, application initialization, project/auth/permission endpoints through real PostgreSQL/MockMvc, compile/package and all 94 backend tests passed. The separate 13 offline contract checks also passed previously. Local Compose configuration is validated; a configured long-running Compose development database, one-time administrator bootstrap, frontend/browser journey and deployment are not established by this test run and remain separately pending. Unresolved later commercial/processing/document business decisions remain recorded in DECISIONS.md.

## Commercial setup contract and V3 preparation — 2026-11-12

Added commercial-setup-v1.yaml, architect-owned V3 migration, CommercialSetupSchemaIT and BACKEND-COMMERCIAL-SETUP.md; extended the contract checker and contract/decision documentation. No business controllers or frontend files were changed; Project v2/account contracts and applied V1/V2 remain unchanged. This first commercial slice adds no tables.

Passed: 17 offline contract checks (13 existing + 4 commercial cases), local OpenAPI reference resolution (84 references before the final response-header additions; all referenced components resolved), YAML parse and git diff --check. Maven offline test-compile passed with release 21, compiling all 8 test sources including the 12 parameterized/schema cases in CommercialSetupSchemaIT. Command: `./mvnw -o -Dmaven.repo.local=/tmp/dms-backend-recheck-maven -Dmaven.compiler.fork=true -Dmaven.compiler.executable=/snap/intellij-idea/154/jbr/bin/javac test-compile` from backend.

Blocked: `docker info --format '{{.ServerVersion}}'` still returns socket permission denied here. V3 migration execution and the new schema tests have not run. Reuse the host verification route after Agent 2 implements the published commercial API; do not reinterpret the historical 94-test success as V3 acceptance. Compile success is not migration/service correctness.

## Commercial implementation dispatch and client schema check

Owner authorized starting slice 1. Dedicated backend-role implementation is active in `/run/media/yacine/yassine/DMS/commercial-backend`, branch feat/backend-commercial-setup, based on d2dd11a. Previous backend branch and OpenCode frontend checkout are preserved. The parent added a read-only V3 duplicate-cardinality preflight; no database execution is claimed for that script.

The new commercial contract generated standalone TypeScript definitions successfully with cached openapi-typescript 7.13.0: `/home/yacine/.npm/_npx/21d31b7563e6adbc/node_modules/.bin/openapi-typescript docs/contracts/commercial-setup-v1.yaml -o /tmp/exportdocs-commercial-schema.d.ts`. This checked local/external schema references without changing frontend files or installing dependencies. Backend business implementation/review and new runtime verification are still in progress.
