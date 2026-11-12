# Commercial setup slice 1 implementation handoff

Current verification update: host V3 migration and all 12 schema checks passed. The first commercial API run exposed a CSRF fixture defect; its correction is integrated and all 72 unit/MVC tests pass. A full 193-test host rerun remains required. See [COMMERCIAL-CSRF-FIX.md](COMMERCIAL-CSRF-FIX.md). The original implementation record below is historical.

Implementation branch: `feat/backend-commercial-setup` in the isolated `commercial-backend` worktree. Base: `d2dd11a` (architect-owned commercial contract/V3). Implementation is committed and compiles; PostgreSQL acceptance remains pending because the sandbox cannot open the Docker socket.

## Commits

- `8269613` — acceptance-first API scenarios.
- `baeb51a` — organization directory, strict request primitives, safe organization selection adapter, MapStruct responses.
- `6729274` — product/grade catalog, protected defaults, exact decimal strings and versioned PATCH.
- `9a63d0e` — commercial project parties, canonical project revision/consignee integration, parser/policy tests, expanded PostgreSQL acceptance.

No migrations, OpenAPI files, frontend files, applied V1/V2, or other worktrees were edited. The architect's response-only clarification `732270f` must accompany integration: legacy organization metadata can have an empty response role array; create/update role arrays remain nonempty. This implementation returns existing empty role sets without manufacturing roles.

## Implemented API

- `/api/organizations`: ADMIN create/update, ready-account read/list; legal names, independent roles, country/address/registration, activation, timestamps and versions.
- `/api/products`: ADMIN create/update, ready-account read/list; case-sensitive product codes, names and optional scientific name/HS code.
- `/api/products/{productId}/grades`: ADMIN create/update, ready-account read/list; immutable parent, ordered size-grade definitions, optional carton net weight, price/kg and currency defaults.
- `/api/projects/{id}/commercial-parties`: visible-project GET, ADMIN or assigned OPERATOR with PROJECT_SETUP PATCH. Supplier/client/importer/consignee are independent nullable scalar selections. Same-company multi-role selection is supported. Consignee uses the existing canonical project_party record, and the response uses the existing project version.

Directory/catalog controllers, typed request/response records, services, JDBC adapters and MapStruct mappers live under `com.exportdocs.party` and `com.exportdocs.catalog`. Commercial integration uses `CommercialParties*` under `com.exportdocs.project`.

## Behavior and safeguards

- Strict JSON parsing rejects unknown/read-only body fields, enum ordinals, coerced booleans, non-string decimal tokens, malformed UUIDs and invalid nulls. Unknown/repeated query parameters are rejected, including on resource reads/writes that accept no query parameters. Bean Validation handles text/code limits; service checks validate ISO country/currency values and the resulting price/currency pair.
- Box weights use BigDecimal numeric(18,3), prices numeric(18,4). Scale/precision/exponents/signs are validated before SQL. Responses use fixed-scale strings, with no binary floating point or rounding of user defaults.
- Presence-aware PATCH merges omitted fields, clears nullable fields on explicit null, checks required nonnegative expectedVersion, detects stale writes, increments once for effective changes, and leaves rows/timestamps/audit unchanged for equivalent/version-only requests.
- ADMIN alone changes master data/defaults. Form assignments confer no directory/catalog mutation permission. Non-admin includeInactive=true is forbidden; by-ID metadata remains readable after deactivation.
- Organization role removal checks current project links and returns 409/roles/role_in_use; links are preserved. Deactivation preserves references. Master-data updates never increment project versions or touch existing shipment/default snapshots.
- Both commercial and v2 project party selection now share OrganizationSelectionStore, which locks selected company rows FOR SHARE in sorted UUID order before validating active state/roles. Organization PATCH uses FOR UPDATE, preventing deactivation or role removal from racing any API that selects a company.
- Commercial and existing v2 setup PATCH lock the project, refresh identity/visibility/form permission after the lock, then check the same canonical project revision. Commercial child-only changes touch/flush Project once. Reads/lists use repeatable-read snapshots.
- Writes reuse existing local account/session/CSRF/must-change-password checks and transaction-local actor/request audit context. Errors reuse the existing application/problem+json field/code/message format. Audit failures roll back data changes.

## Verification

Final compilation and unit run passed with Java 21, including generated MapStruct code:

```bash
cd backend
./mvnw -o \
  -Dmaven.repo.local=/tmp/exportdocs-commercial-m2 \
  -Dmaven.compiler.fork=true \
  -Dmaven.compiler.executable=/snap/intellij-idea/154/jbr/bin/javac \
  -DargLine=-javaagent:/tmp/exportdocs-commercial-m2/org/mockito/mockito-core/5.17.0/mockito-core-5.17.0.jar \
  test
```

Result: **50 unit tests passed, 0 failures, 0 errors, 0 skips** (17 existing + 29 strict commercial parser cases + 4 commercial permission/resulting-pair cases). All 66 main source files and 11 test source files compile. `git diff --check` passes.

The new unit policy cases explicitly deny OPERATOR/REVIEWER/VIEWER organization/product/grade create and PATCH without store/audit mutations, even when a project form grant is available. The resulting-record price/currency PATCH rule is tested before mutation.

`CommercialSetupApiIT` defines **32 PostgreSQL acceptance invocations** across minimal/default/null behavior, exact decimals, permissions and CSRF, role removal, literal filters/order/pagination, duplicate codes, immutable parent, canonical consignee/version, no-op/stale/conflicting writers, inactive and legacy metadata, invalid-reference rollback, user audit context and audit-failure rollback. Deterministic race cases check that v2 selection blocks concurrent deactivation and that both setup APIs recheck a revoked PROJECT_SETUP grant after waiting for the project lock. These test bodies have compiled but have **not executed** in this sandbox.

A full `verify` using the same options compiled, passed all 50 unit tests and packaged successfully, then failed Failsafe at startup for all six PostgreSQL integration classes. Exact cause:

```text
DOCKER_HOST unix:///var/run/docker.sock is not listening
java.net.SocketException: Operation not permitted
Could not find a valid Docker environment
Tests run: 6, Failures: 0, Errors: 6, Skipped: 0
```

This is a class-startup environment failure, not six executed integration scenarios. No Docker permission bypass, skipped Testcontainers annotation or H2 substitute was introduced. Logs: `/tmp/dms-commercial-verify.log`, `/tmp/dms-commercial-unit-final.log`; Maven reports under `backend/target/surefire-reports` and `backend/target/failsafe-reports`.

The originally supplied `/tmp/dms-backend-recheck-maven` cache compiled but had incomplete Surefire plugin metadata. The parent staged the existing complete host cache as `/tmp/exportdocs-commercial-m2`. Mockito dynamic self-attach was denied here; the normal startup Java agent option above avoids dynamic attach without modifying production/build settings or sandbox permissions. The temporary attach artifact was removed.

## Required before acceptance

Run complete host Maven verify with working Docker/PostgreSQL 16. It must execute the existing 77 PostgreSQL integration cases, the architect's 12 V3 schema cases and the new 32 API cases, alongside 50 unit cases (171 expected total). Verify V1 -> V2 -> V3 migration/validation and all concurrency/audit/permission assertions; historical 94-test V1/V2 success is not evidence that this slice passed.

The parent reviewed directory/catalog/commercial sources read-only, identified the cross-API company-selection race, and confirmed the shared locking/post-lock permission changes close it. No unresolved schema/contract defect remains. Apply the architect's legacy-role response clarification and read-only V3 preflight through the parent integration branch. Frontend validation stays with its owner.

Slice 2 remains outside implementation: no invoice/packing content endpoint, monetary total/rounding policy, shipment quantity API or applied-default snapshot persistence was invented. No processing/loss/by-product workflow is required by this slice.
