# Phase 1 validation and delivery — 2026-11-11

**Integrated source implementation prepared; runtime acceptance remains open.** PostgreSQL startup, migration execution, Java compilation, application startup and integration tests have not passed in this environment. No playbook completion boxes were changed.

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
